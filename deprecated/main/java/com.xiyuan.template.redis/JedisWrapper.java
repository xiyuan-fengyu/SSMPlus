package com.xiyuan.template.redis;

import com.google.gson.JsonObject;
import com.xiyuan.template.redis.index.RedisIndex;
import com.xiyuan.template.redis.manager.DbManager;
import com.xiyuan.template.redis.manager.IndexManager;
import com.xiyuan.template.redis.manager.LuaManager;
import com.xiyuan.template.util.Util;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by xiyuan_fengyu on 2017/12/1 14:35.
 */
public class JedisWrapper implements FactoryBean<Jedis>, MethodInterceptor {

    //可配置参数 start
    private JedisPool jedisPool;

    private String indexes = "redis/indexs.json";
    //可配置参数 end


    private boolean isAlive = true;

    private final Map<Thread, JedisWrapperItem> jedisCache = new ConcurrentHashMap<>();

    private final Map<String, Long> keyLoadFromDbCache = new ConcurrentHashMap<>();

    private IndexManager indexManager;

    private LuaManager luaManager;

    @Autowired
    private DbManager dbManager;

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void setIndexes(String indexes) {
        this.indexes = indexes;
    }

    @Override
    public Jedis getObject() throws Exception {
        startLooper();

        indexManager = new IndexManager(indexes);
        luaManager = new LuaManager();

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Jedis.class);
        enhancer.setCallback(this);
        return (Jedis) enhancer.create();
    }

    @Override
    public Class<?> getObjectType() {
        return Jedis.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @PreDestroy
    private void destory() {
        isAlive = false;

        for (Map.Entry<Thread, JedisWrapperItem> entry : jedisCache.entrySet()) {
            entry.getValue().jedis.close();
        }
        jedisCache.clear();

        keyLoadFromDbCache.clear();
        jedisPool.destroy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        Object result = null;
        try (JedisWrapperItem jedisWrapperItem = getJedis()) {
            Jedis jedis = jedisWrapperItem.jedis;
//            System.out.println("intercept: " + method.getName() + "    " + jedis + "    " + Thread.currentThread());

            String methodName = method.getName();
            boolean override = false;

            // 如果是 hset 或者 hmset，则主动校验是否有过期时间设置，索引是否需要更新，
            // 然后在一个 lua 脚本中完成 原记录更新，过期时间更新，索引更新，
            // 最后将更新异步更新到 mysql
            if ("hset".equals(methodName)) {
                override = true;
                result = hset(jedis, objects);
            }
            else if ("hmset".equals(methodName)) {
                override = true;
                result = hmset(jedis, objects, false);
            }
            if (override) {
                // 将更改插入到队列中，然后在 looper 中将更新同步到mysql
                Map<String, String> map;
                if ("hset".equals(methodName)) {
                    map = new HashMap<>();
                    map.put((String) objects[1], (String) objects[2]);
                }
                else {
                    map = (Map<String, String>) objects[1];
                }
                if (dbManager != null) {
                    dbManager.addToQueue((String) objects[0], map);
                }
            }


            // 如果是 hget 或者 hgetall，如果从 redis 没有命中数据，则从 mysql 中获取数据，
            // 然后自动更新到 redis，并自动设置超时时间 和 相关的索引
            if(!override && ("hget".equals(methodName) || "hgetAll".equals(methodName))) {
                override = true;
                boolean notFound;
                String key = (String) objects[0];
                if ("hget".equals(methodName)) {
                    result = jedis.hget(key, (String) objects[1]);
                    notFound = result == null;
                }
                else {
                    Map<String, String> temp = jedis.hgetAll(key);
                    result = temp;
                    notFound = temp.isEmpty();
                }

                if (notFound) {
                    //从数据库加载数据，然后存入到redis，再返回结果
                    LinkedHashMap<String, String> dataInDB = dbManager.selectByKey(key);
                    if (dataInDB != null) {
                        hmset(jedis, new Object[] {key, dataInDB}, true);
                        result = "hget".equals(methodName) ? dataInDB.get(objects[1].toString()) : dataInDB;
                    }
                }
            }


            // 如果是对 set, list, zset 的查询，key 是已配置的索引，且没有这个key的历史查询记录，redis中查出的记录数和mysql中查出的数量不统一，则从 mysql 查询相应的记录，
            // 然后存入 redis，并将这个 key 的历史查询记录的超时时间设置为对应的index的超时时间（如果expire=-1，则设置为1天），最后再次从redis中查询结果
            Class<?> returnType = method.getReturnType();
            if (!override && Collection.class.isAssignableFrom(returnType)) {
                override = true;
                result = method.invoke(jedis, objects);
                int resultSize = ((Collection) result).size();

                // 查询所有相关的index
                HashMap<RedisIndex, Set<String>> indexKeys = new HashMap<>();
                for (Object object : objects) {
                    if (object instanceof String[]) {
                        String[] keys = (String[]) object;
                        for (String key : keys) {
                            mergeIndexAndKey(key, indexKeys);
                        }
                    }
                    else if (object instanceof String) {
                        mergeIndexAndKey((String) object, indexKeys);
                    }
                }

                long now = System.currentTimeMillis();
                boolean shouldRetry = false;
                for (Map.Entry<RedisIndex, Set<String>> entry : indexKeys.entrySet()) {
                    RedisIndex index = entry.getKey();
                    for (String key : entry.getValue()) {
                        if (!keyLoadFromDbCache.containsKey(key)) {
                            keyLoadFromDbCache.put(key, now + (index.expireL < 0 ? 3600000L * 24 : index.expireL * 1000));//在index设置的超时时间内不再检测这个key的size是否和数据库同步，默认时间为 1 天
                            shouldRetry = collectionFromDbToRedis(jedis, index, key, resultSize) || shouldRetry;
                        }
                    }
                }

                if (shouldRetry) {
                    result = method.invoke(jedis, objects);
                }
            }

            if (!override) {
                result = method.invoke(jedis, objects);
            }
        }

        return result;
    }

    private void mergeIndexAndKey(String key, HashMap<RedisIndex, Set<String>> indexKeys) {
        RedisIndex index = indexManager.findByKey(key);
        if (index != null) {
            Set<String> existedKeys = indexKeys.get(index);
            if (existedKeys != null) {
                existedKeys.add(key);
            }
            else {
                existedKeys = new HashSet<>();
                existedKeys.add(key);
                indexKeys.put(index, existedKeys);
            }
        }
    }

    private boolean collectionFromDbToRedis(Jedis jedis, RedisIndex index, String key, int sizeInRedis) {
        RedisIndex keyIndex = indexManager.keyIndex(key);
        if (keyIndex != null) {
            int sizeInDB = dbManager.selectCountByIndexAndKey(index.index, key);
            if (sizeInRedis != sizeInDB && sizeInDB > 0) {
                List<LinkedHashMap<String, String>> datasInDB = dbManager.selectByIndexAndKey(index.index, key);
                if (!datasInDB.isEmpty()) {
                    for (LinkedHashMap<String, String> map : datasInDB) {
                        String tempKey = keyIndex.createKey(map);
                        if (tempKey != null) {
                            hmset(jedis, new Object[]{
                                    tempKey, map
                            }, true);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private Object hset(Jedis jedis, Object[] args) {
        String key = (String) args[0];
        String field = (String) args[1];
        JsonObject data = new JsonObject();
        data.addProperty("NOW", System.currentTimeMillis());
        data.addProperty("method", "hset");
        data.addProperty("key", key);
        data.addProperty("field", field);
        data.addProperty("value", (String) args[2]);
        data.add("indexes", indexManager.findArrByKeyField(key, field));
        //System.out.println(data);
        return jedis.eval(luaManager.hUpdate, 0, data.toString());
    }

    private Object hmset(Jedis jedis, Object[] args, boolean forceAddIndex) {
        String key = (String) args[0];
        JsonObject data = new JsonObject();
        data.addProperty("NOW", System.currentTimeMillis());
        data.addProperty("method", "hmset");
        data.addProperty("key", key);
        data.add("map", Util.gson.toJsonTree(args[1]));
        data.add("indexes", indexManager.findArrByKeyField(key, null));
        if (forceAddIndex) {
            data.addProperty("forceAddIndex", true);
        }
        //System.out.println(data);
        return jedis.eval(luaManager.hUpdate, 0, data.toString());
    }

    private void startLooper() {
        Thread looper = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isAlive) {
                    try {
                        long now = System.currentTimeMillis();

                        // 归还jedis连接
                        if (!jedisCache.isEmpty()) {
                            Iterator<Map.Entry<Thread, JedisWrapperItem>> it = jedisCache.entrySet().iterator();
                            while (it.hasNext()) {
                                Map.Entry<Thread, JedisWrapperItem> entry = it.next();
                                JedisWrapperItem item = entry.getValue();
                                Long returnAt = item.returnAt;
                                if (!item.inUse.get() && returnAt != null && returnAt <= now) {
                                    try {
                                        item.jedis.close();
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    it.remove();
                                }
                            }
                        }

                        //清除 keyLoadFromDbCache 中超时的记录
                        if (!keyLoadFromDbCache.isEmpty()) {
                            Iterator<Map.Entry<String, Long>> it = keyLoadFromDbCache.entrySet().iterator();
                            while (it.hasNext()) {
                                Map.Entry<String, Long> entry = it.next();
                                if (entry.getValue() <= now) {
                                    it.remove();
                                }
                            }
                        }

                        Thread.sleep(100);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        looper.setDaemon(true);
        looper.start();
    }

    private JedisWrapperItem getJedis() {
        Thread curThread = Thread.currentThread();
        JedisWrapperItem cache = jedisCache.get(curThread);
        if (cache == null) {
            cache = new JedisWrapperItem(jedisPool.getResource(), null);
            jedisCache.put(curThread, cache);
        }
        else {
            cache.returnAt = null;
        }
        cache.inUse.set(true);
        return cache;
    }

    private static final class JedisWrapperItem implements Closeable {
        private Jedis jedis;
        private Long returnAt;
        private AtomicBoolean inUse = new AtomicBoolean(false);
        private JedisWrapperItem(Jedis jedis, Long returnAt) {
            this.jedis = jedis;
            this.returnAt = returnAt;
        }

        @Override
        public void close() {
            inUse.set(false);
            returnAt = System.currentTimeMillis() + 2000;
        }
    }

}
