package com.xiyuan.template.mongo.util;

import com.xiyuan.template.util.Util;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;

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

    public static Map<String, Object> and(Map<String, Object> ...cons) {
        return and(Arrays.asList(cons));
    }

    public static Map<String, Object> and(Collection<Map<String, Object>> cons) {
        Map<String, Object> con = new LinkedHashMap<>();
        List<Object> subCons = new ArrayList<>();
        if (cons != null) {
            subCons.addAll(cons);
        }
        con.put("$and", subCons);
        return con;
    }

    public static Map<String, Object> or(Map<String, Object> ...cons) {
        return or(Arrays.asList(cons));
    }

    public static Map<String, Object> or(Collection<Map<String, Object>> cons) {
        Map<String, Object> con = new LinkedHashMap<>();
        List<Object> subCons = new ArrayList<>();
        if (cons != null) {
            subCons.addAll(cons);
        }
        con.put("$or", subCons);
        return con;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> not(Map<String, Object> con) {
        if (con == null || con.isEmpty()) return null;
        Set<Map.Entry<String, Object>> entries = con.entrySet();
        if (entries.size() > 1) {
            List<Map<String, Object>> subNotCons = entries.stream().map(entry -> {
                HashMap<String, Object> subCon = new HashMap<>();
                subCon.put(entry.getKey(), entry.getValue());
                return not(subCon);
            }).collect(Collectors.toList());
            return or(subNotCons);
        }
        else {
            Map<String, Object> notCon = new LinkedHashMap<>();
            String keyOrOperator = con.keySet().toArray()[0].toString();
            if (keyOrOperator.startsWith("$")) {
                if (keyOrOperator.equals("$and") || keyOrOperator.equals("$or")) {
                    List<Object> newSubCons = new ArrayList<>();
                    for (Map<String, Object> subCon : (List<Map<String, Object>>) con.get(keyOrOperator)) {
                        newSubCons.add(not(subCon));
                    }
                    notCon.put(keyOrOperator.equals("$and") ? "$or" : "$and", newSubCons);
                }
                else if (keyOrOperator.equals("$not")){
                    return (Map<String, Object>) con.get(keyOrOperator);
                }
                else {
                    List<String> inverseOperators = Arrays.asList(
                            "$eq", "$ne",
                            "$in", "$nin",
                            "$lt", "$gte",
                            "$lte", "$gt"
                    );
                    for (int i = 0; i < inverseOperators.size(); i++) {
                        if (inverseOperators.get(i).equals(keyOrOperator)) {
                            notCon.put(inverseOperators.get(i + (i % 2 == 0 ? 1 : -1)), con.get(keyOrOperator));
                            return notCon;
                        }
                    }
                    notCon.put("$not", con.get(keyOrOperator));
                    return notCon;
                }
            }
            else {
                Object subCons = con.get(keyOrOperator);
                if (subCons instanceof Map) {
                    notCon.put(keyOrOperator, not((Map<String, Object>) subCons));
                }
                else {
                    Map<String, Object> ne = new HashMap<>();
                    ne.put("$ne", subCons);
                    notCon.put(keyOrOperator, ne);
                }
            }
            return notCon;
        }
    }

    public static Document keyValuesToDoc(Object ...params) {
        if (params == null || params.length == 0) return new Document();
        Document doc = new Document();
        for (int i = 0, len = params.length / 2; i < len; i++) {
            doc.put(params[i * 2].toString(), params[i * 2 + 1]);
        }
        return doc;
    }

    public static Document fields(String ...params) {
        if (params == null || params.length == 0) return new Document();
        Object[] fields = new Object[params.length * 2];
        for (int i = 0; i < params.length; i++) {
            fields[i * 2] = params[i];
            fields[i * 2 + 1] = 1;
        }
        return keyValuesToDoc(fields);
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
//            System.out.println(Util.gsonPretty.toJson(con7));
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
            System.out.println(Util.gsonPretty.toJson(con));
        }
    }

}