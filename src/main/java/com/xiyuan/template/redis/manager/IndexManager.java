package com.xiyuan.template.redis.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xiyuan.template.redis.index.RedisIndex;
import com.xiyuan.template.util.Util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiyuan_fengyu on 2017/12/4 15:38.
 */
public class IndexManager {

    public final String indexesResName;

    private final List<RedisIndex> indexes;

    public IndexManager(String indexesResName) {
        this.indexesResName = indexesResName;
        this.indexes = RedisIndex.createIndexes(Util.readJsonFromResource(indexesResName, StandardCharsets.UTF_8).getAsJsonArray());
    }

    /**
     * 找到 key 对应的 keyIndex
     *  例如  tab_user:id:12:tb_log:id 对应的 keyIndex 为 tb_log:id:${id}
     * @param key
     * @return
     */
    public RedisIndex keyIndex(String key) {
        String[] keySplit = key.split(":");
        String keyIndex = keySplit[keySplit.length - 2] + ":" + keySplit[keySplit.length - 1] + ":${" + keySplit[keySplit.length - 1] + "}";
        for (RedisIndex index : indexes) {
            if (index.index.equals(keyIndex)) {
                return index;
            }
        }
        return null;
    }

    /**
     * 通过 key 和field 来查找所有先关的index
     * @param key
     * @param field
     * @return
     */
    public List<RedisIndex> findListByKeyField(String key, String field) {
        List<RedisIndex> res = new ArrayList<>();
        res.add(null);//第0个位置始终用来存放 keyIndex

        String tableNameAndField = key.substring(0, key.lastIndexOf(':'));
        String keyIndex = tableNameAndField + ":${" + tableNameAndField.substring(tableNameAndField.lastIndexOf(":") + 1) + "}";
        for (RedisIndex index : indexes) {
            if (index.index.endsWith(tableNameAndField)) {
                if (field == null || index.fields.contains(field)) {
                    res.add(index);
                }
            }
            else if (index.index.equals(keyIndex)) {
                res.set(0, index);
            }
        }
        return res;
    }

    public JsonArray findArrByKeyField(String key, String field) {
        List<RedisIndex> list = findListByKeyField(key, field);
        JsonArray arr = new JsonArray();
        for (RedisIndex index : list) {
            arr.add(index != null ? index.config : new JsonObject());
        }
        return arr;
    }

    /**
     * 仅返回 集合 类型的index，且 key 要符合 index 的模式
     * @param key
     * @return
     */
    public RedisIndex findByKey(String key) {
        String[] split = key.split(":");
        for (RedisIndex index : indexes) {
            String[] indexSplit = index.index.split(":");
            if (split.length == indexSplit.length) {
                boolean isMatch = true;
                for (int i = 0; i < split.length; i++) {
                    String indexSplitItem = indexSplit[i];
                    if (indexSplitItem.equals(split[i]) || (indexSplitItem.startsWith("${") && indexSplitItem.endsWith("}"))) {
                        //匹配
                    }
                    else {
                        isMatch = false;
                        break;
                    }
                }
                if (isMatch) {
                    return index;
                }
            }
        }
        return null;
    }

}
