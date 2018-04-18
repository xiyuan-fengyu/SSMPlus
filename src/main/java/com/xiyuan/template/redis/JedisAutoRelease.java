package com.xiyuan.template.redis;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by xiyuan_fengyu on 2017/12/1 14:35.
 */
public class JedisAutoRelease implements FactoryBean<Jedis>, MethodInterceptor {

    private static final long releaseDelay = 3000;

    private JedisPool jedisPool;

    private AtomicBoolean running = new AtomicBoolean(true);

    private ThreadLocal<JedisWrapper> threadLocalJedis = new ThreadLocal<>();

    private DelayQueue<JedisWrapper> delayQueue = new DelayQueue<>();

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public Jedis getObject() {
        Thread thread = new Thread(() -> {
            while (running.get()) {
                try {
                    JedisWrapper jedisWrapper = delayQueue.take();
                    if (!jedisWrapper.released.get()) {
                        synchronized (jedisWrapper.released) {
                            if (!jedisWrapper.released.get() && jedisWrapper.releaseAt != -1 && System.currentTimeMillis() >= jedisWrapper.releaseAt) {
                                jedisWrapper.released.set(true);
                                jedisWrapper.jedis.close();
//                                System.out.println("release: " + jedisWrapper + " " + jedisWrapper.jedis);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

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
        running.set(false);
        jedisPool.destroy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        JedisWrapper jedisWrapper = getFromThreadLocal();
        Object result;
        try {
            result = method.invoke(jedisWrapper.jedis, objects);
        }
        finally {
            delayRelease(jedisWrapper);
        }

        return result;
    }

    private JedisWrapper getFromThreadLocal() {
        JedisWrapper jedisWrapper = threadLocalJedis.get();
        if (jedisWrapper == null || jedisWrapper.released.get()) {
            jedisWrapper = new JedisWrapper(jedisPool.getResource());
            threadLocalJedis.set(jedisWrapper);
        }
        else jedisWrapper.releaseAt = -1;
        return jedisWrapper;
    }

    private void delayRelease(JedisWrapper jedisWrapper) {
        jedisWrapper.releaseAt = System.currentTimeMillis() + releaseDelay;
        if (!delayQueue.contains(jedisWrapper)) {
            delayQueue.add(jedisWrapper);
        }
    }

    private class JedisWrapper implements Delayed {

        private final AtomicBoolean released = new AtomicBoolean(false);

        private long releaseAt = -1;

        private Jedis jedis;

        private JedisWrapper(Jedis jedis) {
            this.jedis = jedis;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return this.releaseAt == -1 ? releaseDelay : this.releaseAt - System.currentTimeMillis();
        }

        @Override
        public int compareTo(Delayed o) {
            if (releaseAt == -1) return 1;

            long delta = releaseAt - ((JedisWrapper) o).releaseAt;
            if (delta == 0) return 0;
            else if (delta < 0) return -1;
            else return 1;
        }

    }

}
