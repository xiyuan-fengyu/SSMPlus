package com.xiyuan.template.mongo.util;

import com.xiyuan.template.util.Util;

import java.util.*;

/**
 * Created by xiyuan_fengyu on 2018/3/28 16:16.
 */
public class MongoQueryHelper {

    public static Map<String, Object> $date(long value) {
        Map<String, Object> con = new LinkedHashMap<>();
        con.put("$date", value);
        return con;
    }

    public static Map<String, Object> con(String key, Object ...operatorValues) {
        if (key == null || operatorValues == null || operatorValues.length < 2 || operatorValues.length % 2 != 0) return null;
        Map<String, Object> con = new LinkedHashMap<>();
        Map<String, Object> subCons = new LinkedHashMap<>();
        for (int i = 0, len = operatorValues.length; i < len; i += 2) {
            subCons.put(operatorValues[i].toString(), operatorValues[i + 1]);
        }
        con.put(key, subCons);
        return con;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> and(Map<String, Object> ...cons) {
        Map<String, Object> con = new LinkedHashMap<>();
        List<Object> subCons = new ArrayList<>();
        if (cons != null) {
            subCons.addAll(Arrays.asList(cons));
        }
        con.put("$and", subCons);
        return con;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> or(Map<String, Object> ...cons) {
        Map<String, Object> con = new LinkedHashMap<>();
        List<Object> subCons = new ArrayList<>();
        if (cons != null) {
            subCons.addAll(Arrays.asList(cons));
        }
        con.put("$or", subCons);
        return con;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> not(Map<String, Object> con) {
        Map<String, Object> notCon = new LinkedHashMap<>();
        String keyOrOperator = con.keySet().toArray()[0].toString();
        if (keyOrOperator.startsWith("$")) {
            List<Object> newSubCons = new ArrayList<>();
            for (Map<String, Object> subCon : (List<Map<String, Object>>) con.get(keyOrOperator)) {
                newSubCons.add(not(subCon));
            }
            notCon.put(keyOrOperator.equals("$and") ? "$or" : "$and", newSubCons);
        }
        else {
            Map<String, Object> subCons = (Map<String, Object>) con.get(keyOrOperator);
            String subKeyOrOperator = subCons.keySet().toArray()[0].toString();
            switch (subKeyOrOperator) {
                case "$and":
                case "$or":
                    List<Object> newSubCons = new ArrayList<>();
                    for (Map<String, Object> subCon : (List<Map<String, Object>>) subCons.get(subKeyOrOperator)) {
                        Map<String, Object> newSubCon = new LinkedHashMap<>();
                        newSubCon.put("$not", subCon);
                        newSubCons.add(newSubCon);
                    }
                    notCon.put(keyOrOperator, newSubCons);
                    break;
                case "$not":
                    notCon.put(keyOrOperator, subCons.get(subKeyOrOperator));
                    break;
                default:
                    Map<String, Object> newSubCon = new LinkedHashMap<>();
                    newSubCon.put("$not", subCons);
                    notCon.put(keyOrOperator, newSubCon);
                    break;
            }
        }
        return notCon;
    }

    public static void main(String[] args) {
        {
            Map<String, Object> con1 = con("companyCode", "$eq", "2");
            Map<String, Object> con2 = con("nowCity", "$eq", "155");
            Map<String, Object> con3 = and(con1, con2);
            Map<String, Object> con4 = not(con3);
            Map<String, Object> con5 = con("userRole", "$gte", "1", "$lte", "4");
            Map<String, Object> con6 = and(con4, con5);
            Map<String, Object> con7 = not(con6);
//            System.out.println(Util.gsonFormat.toJson(con7));
        }

        {
            Map<String, Object> con = and(
                or(
                    con("orgCode", "$in", new String[] {"1", "2", "3"}),
                    con("userRole", "$eq", "2"),
                    con("companyCode", "$eq", "2"),
                    con("nowCity", "$eq", "155")
                ),
                not(con("comeAddress", "$eq", "暂无")),
                con("isonjob", "$ne", "0"),
                con("userBirthday", "$gte", $date(157737600000L), "$lte", $date(473356800000L)),
                con("userPosition", "$regex", "高级.*经理")
            );
            System.out.println(Util.gsonFormat.toJson(con));
        }
    }

}