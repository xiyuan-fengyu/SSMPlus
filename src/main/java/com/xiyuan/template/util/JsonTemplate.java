package com.xiyuan.template.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.JsonPath;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by xiyuan_fengyu on 2018/6/8 9:16.
 */
@SuppressWarnings("unchecked")
public class JsonTemplate {

    public static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").serializeNulls().create();

    public static final Gson gsonPretty = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").serializeNulls().setPrettyPrinting().create();

    private static final ScriptEngine jsEngine = new ScriptEngineManager().getEngineByName("js");

    private static final Map<String, String> resourceCaches = new HashMap<>();

    public static Object parseTemplate(String template, Object ...params) {
        return filler(gson.fromJson(template, Object.class), params);
    }

    public static Object parseResourceTemplate(String resource, Object ...params) {
        return parseTemplate(getResourceContent(resource), params);
    }

    private static String getResourceContent(String resource) {
        return resourceCaches.computeIfAbsent(resource, key -> {
            try (InputStream in = JsonTemplate.class.getClassLoader().getResourceAsStream(resource)) {
                if (in != null) {
                    byte[] bytes = new byte[in.available()];
                    if (in.read(bytes) > -1) {
                        return new String(bytes, StandardCharsets.UTF_8);
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        });
    }

    private static Object filler(Object obj, Object params[]) {
        if (obj == null || params == null) return null;

        Map<String, Object> context = new HashMap<>();
        for (int i = 0; i < params.length; i++) {
            context.put("$" + i, params[i]);
        }
        return filler(obj, context);
    }

    private static Object filler(Object obj, Map<String, Object> context) {
        if (obj instanceof Map) {
            return fillMapValue((Map<String, Object>) obj, context);
        }
        else if (obj instanceof List) {
            return fillListValue((List<Object>) obj, context);
        }
        else if (obj instanceof String) {
            return fillStringValue((String) obj, context);
        }
        return obj;
    }

    private static Map<String, Object> fillMapValue(Map<String, Object> map, Map<String, Object> context) {
        Map<String, Object> fillerObj = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            Placeholder placeholder = new Placeholder(key);
            if (!placeholder.raw) {
                if (placeholder.conditionValid(context)) {
                    if (placeholder.paramIndexOrName == null) {
                        Object filledValue = filler(value, context);
                        if (placeholder.key != null) {
                            fillerObj.put(placeholder.key, filledValue);
                        }
                        else if (filledValue instanceof Map) {
                            fillerObj.putAll((Map) filledValue);
                        }
                    }
                    else {
                        List<Object> unwindMaps = new ArrayList<>();
                        if (placeholder.foreach) {
                            unwindMaps.addAll(fillForeach(placeholder, value, context));
                        }
                        else {
                            Object placeholderValue = placeholder.value(context);
                            if (placeholderValue instanceof Map) {
                                unwindMaps.add(placeholderValue);
                            }
                            else if (isBasicOrStringType(placeholderValue)) {
                                fillerObj.put(placeholderValue.toString(), filler(value, context));
                            }
                        }

                        if (unwindMaps.size() > 0) {
                            for (Object item : unwindMaps) {
                                if (item instanceof Map) {
                                    Map<String, Object> unwindMap = (Map<String, Object>) item;
                                    Set<String> unwindMapKeys = unwindMap.keySet();
                                    List<String> unwindKeys = new ArrayList<>();
                                    if (value == null) {
                                        // 全展开
                                        unwindKeys.addAll(unwindMapKeys);
                                    } else if (value instanceof String) {
                                        // 单个展开规则
                                        keyFilter(unwindKeys, unwindMapKeys, (String) value);
                                    } else if (value instanceof List) {
                                        // 多个展开规则
                                        for (String exp : (List<String>) value) {
                                            keyFilter(unwindKeys, unwindMapKeys, exp);
                                        }
                                    }

                                    for (String unwindKey : unwindKeys) {
                                        fillerObj.put(unwindKey, unwindMap.get(unwindKey));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else {
                fillerObj.put(placeholder.rawStr, filler(value, context));
            }
        }
        return fillerObj;
    }

    private static boolean isBasicOrStringType(Object obj) {
        return obj instanceof Number
                || obj instanceof String
                || obj instanceof Character
                || obj instanceof Boolean;
    }

    private static List<Object> fillForeach(Placeholder foreach,  Object valueTemplate, Map<String, Object> context) {
        List<Object> res = foreach.unwind ? new UnwindArrayList() : new ArrayList<>();
        Object foreachValue = foreach.value(context);
        if (foreachValue instanceof Map) {
            ((Map<String, Object>) foreachValue).forEach((key, value) -> {
                Map<String, Object> subContext = new HashMap<>(context);
                subContext.put("$" + foreach.foreachIndexOrKeyName, key);
                subContext.put("$" + foreach.foreachValueName, value);
                Object subValue = filler(valueTemplate, subContext);
                if (subValue != null) res.add(subValue);
            });
        }
        else if (foreachValue instanceof Iterable) {
            int index = 0;
            for (Object item : (Iterable<Object>) foreachValue) {
                Map<String, Object> subContext = new HashMap<>(context);
                subContext.put("$" + foreach.foreachIndexOrKeyName, index);
                subContext.put("$" + foreach.foreachValueName, item);
                Object subValue = filler(valueTemplate, subContext);
                if (subValue != null) res.add(subValue);
                index++;
            }
        }
        return res;
    }

    private static List<Object> fillListValue(List<Object> list, Map<String, Object> context) {
        List<Object> fillerList = new ArrayList();
        for (Object o : list) {
            if (o instanceof String) {
                Placeholder placeholder = new Placeholder((String) o);
                if (placeholder.raw) fillerList.add(placeholder.rawStr);
                else {
                    if (placeholder.conditionValid(context)) {
                        Object placeholderValue = placeholder.value(context);
                        if (placeholder.unwind && placeholderValue instanceof Iterable) {
                            Iterable it = (Iterable) placeholderValue;
                            for (Object obj : it) {
                                fillerList.add(obj);
                            }
                        }
                        else {
                            fillerList.add(placeholderValue);
                        }
                    }
                }
            }
            else {
                Object filled = filler(o, context);
                if (filled == null
                        || (filled instanceof Map && ((Map) filled).isEmpty())) {
                    // ignore
                }
                else {
                    if (filled instanceof UnwindArrayList) {
                        fillerList.addAll((UnwindArrayList) filled);
                    }
                    else fillerList.add(filled);
                }
            }
        }
        return fillerList;
    }

//    private static List<Object> listSlice(String placeholder, Object[] params) {
//        List<Object> subList = new ArrayList<>();
//
//        String temp = placeholder;
//        temp = temp.substring(2, temp.length() - 2);
//        int paramI = Integer.parseInt(temp.substring(Math.max(0, placeholder.indexOf('?') + 1), temp.indexOf('[')).trim().replaceAll("\\$", "").trim());
//        String [] slices = temp.substring(temp.indexOf('[') + 1, temp.lastIndexOf(']')).split(",");
//        List<Object> unwindList = (List<Object>) params[paramI];
//        for (String slice : slices) {
//            String[] fromToS = (slice + ' ').split(":");
//            if (fromToS.length == 1) {
//                subList.add(unwindList.get(Integer.parseInt(fromToS[0].trim())));
//            } else {
//                int from = Integer.parseInt(fromToS[0].trim());
//                int to;
//                String toS = fromToS[1].trim();
//                if (toS.isEmpty()) to = unwindList.size();
//                else {
//                    to = Integer.parseInt(toS);
//                    if (to < 0) {
//                        to = unwindList.size() + to;
//                    }
//                }
//                for (int j = from; j < to; j++) {
//                    subList.add(unwindList.get(j));
//                }
//            }
//        }
//        return subList;
//    }

    private static Object fillStringValue(String str, Map<String, Object> context) {
        Placeholder placeholder = new Placeholder(str);
        if (placeholder.raw) return placeholder.rawStr;
        if (placeholder.conditionValid(context)) return placeholder.value(context);
        return null;
    }

    private static void keyFilter(List<String> unwindKeys, Set<String> mapKeys, String exp) {
        if (exp.startsWith("-:")) {
            // 按正则排除
            String excludeKeys = exp.substring(2);
            unwindKeys.addAll(mapKeys.stream().filter(k -> !excludeKeys.equals(k) && !k.matches(excludeKeys)).collect(Collectors.toList()));
            unwindKeys.removeIf(k -> excludeKeys.equals(k) || excludeKeys.matches(exp));
        }
        else unwindKeys.addAll(mapKeys.stream().filter(k -> exp.equals(k) || k.matches(exp)).collect(Collectors.toList()));
    }

    private static class UnwindArrayList extends ArrayList<Object> {

    }

    private static class Placeholder {

        public boolean raw;

        public String rawStr;

        public boolean unwind;

        public String condition;

        public String key;

        public boolean foreach;

        public String foreachIndexOrKeyName;

        public String foreachValueName;

        public String paramIndexOrName;

        public String jsonPath;

        private static final Pattern conditionKeyP = Pattern.compile("^(.+) *\\? * '(.*)'$");

        private static final Pattern conditionParamPathP = Pattern.compile("^((.+) *\\? *)?( *foreach +\\((.+), (.+)*\\) +of +)?\\$(.+?)(\\$.*)?$");

        private Placeholder(String placeholder) {
            if ((placeholder.startsWith("{{") && placeholder.endsWith("}}"))
                    || placeholder.startsWith("[[") && placeholder.endsWith("]]")) {
                raw = false;
                unwind = placeholder.charAt(0) == '[';

                boolean match = false;
                String content = placeholder.substring(2, placeholder.length() - 2).trim();
                if (content.endsWith("?")) {
                    match = true;
                    condition = content.substring(0, content.length() - 1);
                }

                if (!match) {
                    Matcher conditionKeyM = conditionKeyP.matcher(content);
                    if (conditionKeyM.find()) {
                        match = true;
                        condition = conditionKeyM.group(1);
                        key = conditionKeyM.group(2);
                    }
                }

                if (!match) {
                    Matcher conditionParamPathM = conditionParamPathP.matcher(content);
                    if (conditionParamPathM.find()) {
                        condition = conditionParamPathM.group(2);
                        foreach = conditionParamPathM.group(3) != null;
                        foreachIndexOrKeyName = conditionParamPathM.group(4);
                        foreachValueName = conditionParamPathM.group(5);
                        paramIndexOrName = conditionParamPathM.group(6);
                        jsonPath = conditionParamPathM.group(7);
                    }
                }
            }
            else {
                raw = true;
                if (placeholder.startsWith("raw(") && placeholder.endsWith(")")) {
                    rawStr = placeholder.substring(4, placeholder.length() - 1);
                }
                else {
                    rawStr = placeholder;
                }
            }
        }

        private boolean conditionValid(Map<String, Object> context) {
            if (condition == null) return true;
            try {
                Object res = jsEngine.eval(condition, new SimpleBindings(context));
                if (res == null || Boolean.FALSE.equals(res)) return false;
                if (res instanceof Number) {
                    double d = ((Number) res).doubleValue();
                    return d != 0 && !Double.isNaN(d);
                }
                return !(res instanceof String) || !((String) res).isEmpty();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        private Object value(Map<String, Object> context) {
            if (paramIndexOrName == null) return null;
            else if (jsonPath == null) return context.get("$" + paramIndexOrName);
            else return JsonPath.parse(context.get("$" + paramIndexOrName)).read(jsonPath, Object.class);
        }

    }

    public static void main(String[] args) throws ScriptException {
//        Map<String, Object> $0 = new HashMap<>();
//        $0.put("aaa", "aaa");
//        $0.put("bbb", "bbb");
//        $0.put("ccc", "ccc");
//
//        Map<String, Object> map = new HashMap<>();
//        map.put("a", "aaa");
//        map.put("b", "bbb");
//        map.put("c", "ccc");
//        List<Object> $1 = Arrays.asList(map, 1, 2, 3, 4, 5, 6, 7, 8, 9);
//        Object obj = parseResourceTemplate("jsonTemplate/0.json", $0, $1);
//        System.out.println(gsonPretty.toJson(obj));

        Map<String, Object> res = (Map<String, Object>) jsEngine.eval("" +
                "var res = {};\n" +
                "res.condition = true;\n" +
                "if (res.condition) res.value = 123;\n" +
                "res;");
        System.out.println(res.get("condition"));
        System.out.println(res.get("value"));
    }

}
/*
var res = {};
res.condition = true;
if (res.condition) res.value = 123;
res;
 */