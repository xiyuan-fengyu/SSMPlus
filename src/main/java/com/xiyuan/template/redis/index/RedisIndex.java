package com.xiyuan.template.redis.index;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xiyuan_fengyu on 2017/12/4 13:34.
 */
public class RedisIndex {

    public final String index;

    public final HashSet<String> fields = new HashSet<>();

    public final String type;

    public final String expire;

    public final long expireL;

    public final JsonObject config;

    protected RedisIndex(String index, String type, String expire) {
        this.index = index;
        this.type = type;
        this.expire = expire;
        this.expireL = parseExpire(expire);

        config = new JsonObject();
        config.addProperty("index", index);
        config.add("fields", findFieldsFromIndex(index));
        config.addProperty("type", type);
        config.addProperty("expire", expireL);
    }

    private JsonArray findFieldsFromIndex(String index) {
        JsonArray fieldArr = new JsonArray();
        Matcher matcher = Pattern.compile("\\$\\{(.*?)\\}").matcher(index);
        while (matcher.find()) {
            String field = matcher.group(1);
            if (!fields.contains(field)) {
                fieldArr.add(field);
                fields.add(field);
            }
        }
        return fieldArr;
    }

    private long parseExpire(String expireStr) {
        long temp = -1;
        if (expireStr == null || "".equals(expireStr)) {
            temp = -1;
        }
        else if (expireStr.matches("[+|-]?\\d+")) {
            temp = Long.parseLong(expireStr);
        }
        else if (expireStr.matches("\\d+([dhms])?( * \\* *(\\d+)([dhms])?)*")) {
            temp = 1;
            Matcher matcher = Pattern.compile("(\\d+)([dhms])?").matcher(expireStr);
            while (matcher.find()) {
                long unit = 1;
                String unitStr = matcher.group(2);
                if ("d".equals(unitStr)) unit = 86400L;
                else if ("h".equals(unitStr)) unit = 3600L;
                else if ("m".equals(unitStr)) unit = 60L;
                temp *= Long.parseLong(matcher.group(1)) * unit;
            }
        }

        if (temp < 0) {
            temp = -1;
        }
        return temp;
    }

    public String createKey(Map<String, String> map) {
        String key = index;
        for (String field : fields) {
            Object value = map.get(field);
            if (value == null) {
                return null;
            }
            key = key.replaceAll("\\$\\{" + field + "\\}", value.toString());
        }
        return key;
    }

    public static RedisIndex createIndex(JsonObject indexObj) {
        String index = indexObj.get("index").getAsString();
        String type = indexObj.get("type").getAsString();
        String expire = indexObj.has("expire") ? indexObj.get("expire").getAsString() : null;
        RedisIndex redisIndex = null;
        switch (type) {
            case "Hash":
            case "h": {
                redisIndex = new HashIndex(index, "h", expire);
                break;
            }
            case "List":
            case "l": {
                redisIndex = new ListIndex(index, "l", expire);
                break;
            }
            case "Set":
            case "s": {
                redisIndex = new SetIndex(index, "s", expire);
                break;
            }
            case "SortedSet":
            case "z": {
                String score = indexObj.has("score") ? indexObj.get("score").getAsString() : null;
                redisIndex = new SortedSetIndex(index, "z", expire, score);
                break;
            }
        }
        return redisIndex;
    }

    public static List<RedisIndex> createIndexes(JsonArray indexArr) {
        List<RedisIndex> indexes = new ArrayList<>();
        for (JsonElement ele : indexArr) {
            RedisIndex index = createIndex(ele.getAsJsonObject());
            if (index != null) {
                indexes.add(index);
            }
        }
        return indexes;
    }

}
