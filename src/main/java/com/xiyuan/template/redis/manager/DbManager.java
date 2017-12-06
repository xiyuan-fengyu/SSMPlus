package com.xiyuan.template.redis.manager;

import com.xiyuan.template.redis.dao.ColVal;
import com.xiyuan.template.redis.dao.DynamicDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by xiyuan_fengyu on 2017/12/4 18:26.
 */
@Component
public class DbManager {

    @Autowired
    private DynamicDao dynamicDao;

    private final BlockingQueue<KeyAndMap> queue = new LinkedBlockingDeque<>();

    private final List<Object[]> errors = new ArrayList<>();

    @PostConstruct
    private void init() {
        // 开启一个 looper ,轮询 queue
        Thread looper = new Thread() {
            @Override
            public void run() {
                while (isAlive()) {
                    KeyAndMap keyAndMap = null;
                    try {
                        keyAndMap = queue.take();
                        String key = keyAndMap.key;
                        String[] split = key.split(":");
                        String tableName = split[0];
                        String pk = split[1];
                        String pkVal = split[2];

                        List<ColVal> colVals = new ArrayList<>();
                        for (Map.Entry<String, String> entry : keyAndMap.map.entrySet()) {
                            if (!entry.getKey().equals(pk)) {
                                colVals.add(new ColVal(
                                        entry.getKey(),
                                        entry.getValue()
                                ));
                            }
                        }

                        //首先判断数据库中是否已经存在这条记录了
                        List<Object> exists = dynamicDao.existedCheck(tableName, pk, pkVal);
                        int size = exists.size();
                        if (size == 0) {
                            //插入新值
                            dynamicDao.insert(tableName, pk, pkVal, colVals);
                        }
                        else if (size == 1) {
                            //更新已存在的唯一的一条记录
                            dynamicDao.update(tableName, pk, pkVal, colVals);
                        }
                        else {
                            //存在多条记录
                            throw  new Exception("There are " + size + " rows where " + pk + " = " + pkVal + ", update has been cancelled");
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        if (keyAndMap != null) {
                            errors.add(new Object[]{
                                    keyAndMap,
                                    e
                            });
                        }
                    }
                }
            }
        };
        looper.setDaemon(true);
        looper.start();
    }

    public void addToQueue(String key, Map<String, String> map) {
        queue.offer(new KeyAndMap(key, map));
    }

    public LinkedHashMap<String, String> selectByKey(String key) {
        String[] split = key.split(":");
        String tableName = split[0];
        String pk = split[1];
        String pkVal = split[2];
        return dynamicDao.selectByPk(tableName, pk, pkVal);
    }

    public int selectCountByIndexAndKey(String index, String key) {
        List<ColVal> colVals = getColVals(index, key);
        if (colVals != null) {
            String[] keySplit = key.split(":");
            return dynamicDao.selectCountByColVals(keySplit[keySplit.length - 2], colVals);
        }
        else return 0;
    }

    public List<LinkedHashMap<String, String>> selectByIndexAndKey(String index, String key) {
        List<ColVal> colVals = getColVals(index, key);
        if (colVals != null) {
            String[] keySplit = key.split(":");
            return dynamicDao.selectByColVals(keySplit[keySplit.length - 2], colVals);
        }
        else return new ArrayList<>();
    }

    private List<ColVal> getColVals(String index, String key) {
        String[] indexSplit = index.split(":");
        String[] keySplit = key.split(":");
        if (indexSplit.length == keySplit.length) {
            List<ColVal> colVals = new ArrayList<>();
            for (int i = 0; i < indexSplit.length; i++) {
                String indexSplitItem = indexSplit[i];
                if (indexSplitItem.matches("\\$\\{.+\\}")) {
                    colVals.add(new ColVal(indexSplitItem.substring(2, indexSplitItem.length() - 1), keySplit[i]));
                }
            }
            return colVals;
        }
        else return null;
    }

    private static class KeyAndMap {
        private final String key;
        private final Map<String, String> map;
        private KeyAndMap(String key, Map<String, String> map) {
            this.key = key;
            this.map = map;
        }
    }

}
