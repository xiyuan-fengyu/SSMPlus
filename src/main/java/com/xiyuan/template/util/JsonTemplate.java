package com.xiyuan.template.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.JsonPath;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
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
        SimpleBindings bindings = new SimpleBindings();
        for (int i = 0; i < params.length; i++) {
            bindings.put("$" + i, params[i]);
        }
        return filler(obj, params, bindings);
    }

    private static Object filler(Object obj, Object params[], SimpleBindings bindings) {
        if (obj instanceof Map) {
            return fillMapValue((Map<String, Object>) obj, params, bindings);
        }
        else if (obj instanceof List) {
            return fillListValue((List<Object>) obj, params, bindings);
        }
        else if (obj instanceof String) {
            return fillStringValue((String) obj, params, bindings);
        }
        return obj;
    }

    private static Map<String, Object> fillMapValue(Map<String, Object> map, Object params[], SimpleBindings bindings) {
        Map<String, Object> fillerObj = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            Placeholder placeholder = new Placeholder(key);
            if (!placeholder.raw) {
                if (placeholder.conditionValid(bindings)) {
                    if (placeholder.paramIndex == -1) {
                        Object filledValue = filler(value, params, bindings);
                        if (placeholder.key != null) {
                            fillerObj.put(placeholder.key, filledValue);
                        }
                        else if (filledValue instanceof Map) {
                            fillerObj.putAll((Map) filledValue);
                        }
                    }
                    else {
                        Object placeholderValue = placeholder.value(params);
                        if (placeholderValue instanceof Map) {
                            Map<String, Object> unwindMap = (Map<String, Object>) placeholderValue;
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
            else {
                fillerObj.put(placeholder.rawStr, filler(value, params));
            }
        }
        return fillerObj;
    }

    private static List<Object> fillListValue(List<Object> list, Object params[], SimpleBindings bindings) {
        List<Object> fillerList = new ArrayList();
        for (Object o : list) {
            if (o instanceof String) {
                Placeholder placeholder = new Placeholder((String) o);
                if (placeholder.raw) fillerList.add(placeholder.rawStr);
                else {
                    if (placeholder.conditionValid(bindings)) {
                        Object placeholderValue = placeholder.value(params);
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
                Object filled = filler(o, params);
                if (filled == null
                        || (filled instanceof Map && ((Map) filled).isEmpty())) {
                    // ignore
                }
                else fillerList.add(filled);
            }
        }
        return fillerList;
    }

    private static List<Object> listSlice(String placeholder, Object[] params) {
        List<Object> subList = new ArrayList<>();

        String temp = placeholder;
        temp = temp.substring(2, temp.length() - 2);
        int paramI = Integer.parseInt(temp.substring(Math.max(0, placeholder.indexOf('?') + 1), temp.indexOf('[')).trim().replaceAll("\\$", "").trim());
        String [] slices = temp.substring(temp.indexOf('[') + 1, temp.lastIndexOf(']')).split(",");
        List<Object> unwindList = (List<Object>) params[paramI];
        for (String slice : slices) {
            String[] fromToS = (slice + ' ').split(":");
            if (fromToS.length == 1) {
                subList.add(unwindList.get(Integer.parseInt(fromToS[0].trim())));
            } else {
                int from = Integer.parseInt(fromToS[0].trim());
                int to;
                String toS = fromToS[1].trim();
                if (toS.isEmpty()) to = unwindList.size();
                else {
                    to = Integer.parseInt(toS);
                    if (to < 0) {
                        to = unwindList.size() + to;
                    }
                }
                for (int j = from; j < to; j++) {
                    subList.add(unwindList.get(j));
                }
            }
        }
        return subList;
    }

    private static Object fillStringValue(String str, Object params[], SimpleBindings bindings) {
        Placeholder placeholder = new Placeholder(str);
        if (placeholder.raw) return placeholder.rawStr;
        if (placeholder.conditionValid(bindings)) return placeholder.value(params);
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

    private static class Placeholder {

        public boolean raw;

        public String rawStr;

        public boolean unwind;

        public String condition;

        public String key;

        public int paramIndex = -1;

        public String jsonPath;

        private static final Pattern conditionKeyP = Pattern.compile("^(.+) *\\? * '(.*)'$");

        private static final Pattern conditionParamPathP = Pattern.compile("^((.+) *\\? *)?\\$(\\d+)(\\$.*)?$");

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
                        paramIndex = Integer.parseInt(conditionParamPathM.group(3));
                        jsonPath = conditionParamPathM.group(4);
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

        private boolean conditionValid(SimpleBindings bindings) {
            if (condition == null) return true;
            try {
                Object res = jsEngine.eval(condition, bindings);
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

        private Object value(Object[] params) {
            if (paramIndex == -1) return null;
            else if (jsonPath == null) return params[paramIndex];
            else return JsonPath.parse(params[paramIndex]).read(jsonPath, Object.class);
        }

    }

//    public static void main(String[] args) {
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
//    }

}
