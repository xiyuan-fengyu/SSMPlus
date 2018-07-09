package com.xiyuan.template.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xiyuan_fengyu on 2018/6/8 9:16.
 */
@SuppressWarnings("unchecked")
public class JsonTemplate {

    public static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    public static final Gson gsonPretty = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private static final ScriptEngine jsEngine = new ScriptEngineManager().getEngineByName("js");

    private static final Map<String, String> resourceCaches = new HashMap<>();

    private static final IgnoreObject ignoreObject = new IgnoreObject();

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

    private static Object fillMapValue(Map<String, Object> map, Map<String, Object> context) {
        if (map.isEmpty()) return map;

        Map<String, Object> fillerObj = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            Placeholder placeholder = new Placeholder(key);
            if (placeholder.raw) {
                fillerObj.put(placeholder.rawStr, filler(value, context));
            }
            else {
                ConditionValueRes evalRes = placeholder.eval(context);
                if (evalRes.condition) {
                    if (placeholder.foreach) {
                        Object foreachRes = fillForeach(placeholder, evalRes, value, context);
                        if (foreachRes instanceof List) {
                            for (Object obj : (List) foreachRes) {
                                if (obj instanceof Map) {
                                    fillerObj.putAll((Map<String, ?>) obj);
                                }
                            }
                        }
                    }
                    else if (evalRes.value != null) {
                        if (evalRes.value instanceof Map) {
                            fillerObj.putAll((Map<String, ?>) evalRes.value);
                        }
                        else if (evalRes.value instanceof Iterable) {
                            for (Object obj : (Iterable) evalRes.value) {
                                if (obj instanceof Map) {
                                    fillerObj.putAll((Map<String, ?>) obj);
                                }
                            }
                        }
                        else if (isBasicOrStringType(evalRes.value)) {
                            fillerObj.put(evalRes.value.toString(), filler(value, context));
                        }
                    }
                    else if (value != null) {
                        Object subValue = filler(value, context);
                        if (subValue instanceof Map) {
                            fillerObj.putAll((Map<String, ?>) subValue);
                        }
                        else if (subValue instanceof Iterable) {
                            for (Object obj : (Iterable) subValue) {
                                if (obj instanceof Map) {
                                    fillerObj.putAll((Map<String, ?>) obj);
                                }
                            }
                        }
                    }
                }
                else if (map.size() == 1) return ignoreObject;
            }
        }
        return fillerObj.isEmpty() ? ignoreObject : fillerObj;
    }

    private static boolean isBasicOrStringType(Object obj) {
        return obj instanceof Number
                || obj instanceof String
                || obj instanceof Character
                || obj instanceof Boolean;
    }

    private static Object fillForeach(Placeholder placeholder, ConditionValueRes evalValue,  Object valueTemplate, Map<String, Object> context) {
        if (!evalValue.condition) return ignoreObject;

        List<Object> res = placeholder.unwind ? new UnwindArrayList() : new ArrayList<>();
        if (evalValue.value instanceof Map) {
            ((Map<String, Object>) evalValue.value).forEach((key, value) -> {
                Map<String, Object> subContext = new HashMap<>(context);
                subContext.put("$" + placeholder.foreachIndexOrKeyName, key);
                subContext.put("$" + placeholder.foreachValueName, value);
                Object subValue = filler(valueTemplate, subContext);
                if (subValue != null && subValue != ignoreObject) tryUnwind(subValue, res);
            });
        }
        else if (evalValue.value instanceof Iterable) {
            int index = 0;
            for (Object item : (Iterable<Object>) evalValue.value) {
                Map<String, Object> subContext = new HashMap<>(context);
                subContext.put("$" + placeholder.foreachIndexOrKeyName, index);
                subContext.put("$" + placeholder.foreachValueName, item);
                Object subValue = filler(valueTemplate, subContext);
                if (subValue != null && subValue != ignoreObject) tryUnwind(subValue, res);
                index++;
            }
        }
        else return ignoreObject;
        return res;
    }

    private static Object fillListValue(List<Object> list, Map<String, Object> context) {
        if (list.isEmpty()) return list;

        List<Object> fillerList = new ArrayList();
        for (int i = 0, size = list.size(); i < size; i++) {
            Object o = list.get(i);
            if (o instanceof String) {
                Placeholder placeholder = new Placeholder((String) o);
                if (placeholder.raw) fillerList.add(placeholder.rawStr);
                else {
                    if (placeholder.foreach && size == 2 && i == 0) {
                        ConditionValueRes evalRes = placeholder.eval(context);
                        if (evalRes.condition) {
                            return fillForeach(placeholder, evalRes, list.get(1), context);
                        }
                        else return ignoreObject;
                    }
                    else {
                        Object fillRes = fillStringValue(placeholder, context);
                        if (fillRes != ignoreObject) {
                            if (fillRes instanceof UnwindArrayList) {
                                fillerList.addAll(((UnwindArrayList) fillRes));
                            }
                            else fillerList.add(fillRes);
                        }
                    }
                }
            }
            else {
                Object filled = filler(o, context);
                tryUnwind(filled, fillerList);
            }
        }
        return fillerList.isEmpty() ? ignoreObject : fillerList;
    }

    private static void tryUnwind(Object source, List<Object> dist) {
        if (source != ignoreObject) {
            if (source instanceof UnwindArrayList) dist.addAll(((UnwindArrayList) source));
            else dist.add(source);
        }
    }

    private static Object fillStringValue(String str, Map<String, Object> context) {
        Placeholder placeholder = new Placeholder(str);
        return fillStringValue(placeholder, context);
    }

    private static Object fillStringValue(Placeholder placeholder, Map<String, Object> context) {
        if (placeholder.raw) return placeholder.rawStr;
        ConditionValueRes evalRes = placeholder.eval(context);
        if (!evalRes.condition) return ignoreObject;
        if (placeholder.foreach) {
            return ignoreObject;
        }
        else if (placeholder.unwind && evalRes.value instanceof List) {
            UnwindArrayList unwindArrayList = new UnwindArrayList();
            unwindArrayList.addAll((List) evalRes.value);
            return unwindArrayList;
        }
        return evalRes.value;
    }

    private static class UnwindArrayList extends ArrayList<Object> {
        private static final long serialVersionUID = -3946129382500020520L;
    }

    private static class IgnoreObject {}

    private static class Placeholder {

        boolean raw;

        String rawStr;

        boolean unwind;

        String conditionExp;

        boolean foreach;

        String foreachIndexOrKeyName;

        String foreachValueName;

        String valueExp;

        private static final Pattern conditionForeachValueP = Pattern.compile("^(if *\\((.+)\\) *)? *foreach *\\((.+),(.+)\\) *of +(.*)?$");

        private static final Pattern conditionValueP = Pattern.compile("^(if *\\((.+)\\) *)?(.*)?$");

        private Placeholder(String placeholder) {
            if ((placeholder.startsWith("{{") && placeholder.endsWith("}}"))
                    || placeholder.startsWith("[[") && placeholder.endsWith("]]")) {
                raw = false;
                unwind = placeholder.charAt(0) == '[';

                String content = placeholder.substring(2, placeholder.length() - 2).trim();
                Matcher conditionForeachValueM;
                if (content.contains("foreach") && (conditionForeachValueM = conditionForeachValueP.matcher(content)).find()) {
                    conditionExp = conditionForeachValueM.group(2);

                    foreach = true;
                    foreachIndexOrKeyName = conditionForeachValueM.group(3);
                    if (foreachIndexOrKeyName != null) foreachIndexOrKeyName = foreachIndexOrKeyName.trim();
                    foreachValueName = conditionForeachValueM.group(4);
                    if (foreachValueName != null) foreachValueName = foreachValueName.trim();

                    valueExp = conditionForeachValueM.group(5);
                }
                else {
                    Matcher conditionValueM = conditionValueP.matcher(content);
                    if (conditionValueM.find()) {
                        conditionExp = conditionValueM.group(2);
                        valueExp = conditionValueM.group(3);
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

        private ConditionValueRes eval(Map<String, Object> context) {
            if (conditionExp != null || valueExp != null) {
                try {
                    String evalStr = "var res = {};\n" +
                            "res.condition = " + (conditionExp == null ? "true" : conditionExp) + " ? true : false;\n" +
                            "if (res.condition) res.value = " + (valueExp == null || valueExp.trim().isEmpty() ? "null" : valueExp) + ";\n" +
                            "res;";
                    SimpleBindings bindings = new SimpleBindings();
                    bindings.putAll(context);
                    Map<String, Object> res = (Map<String, Object>) jsEngine.eval(evalStr, bindings);
                    return new ConditionValueRes(res);
                } catch (ScriptException e) {
                    e.printStackTrace();
                }
            }
            return new ConditionValueRes(false, null);
        }

    }

    private static class ConditionValueRes {
        boolean condition;
        Object value;

        ConditionValueRes(boolean condition, Object value) {
            this.condition = condition;
            this.value = value;
        }

        ConditionValueRes(Map<String, Object> map) {
            condition = Boolean.TRUE.equals(map.get("condition"));
            value = map.get("value");
        }
    }

    public static void main(String[] args) {
        Map<String, Object> $0 = new HashMap<>();
        $0.put("aaa", "aaa");
        $0.put("bbb", "bbb");
        $0.put("ccc", "ccc");

        Map<String, Object> map = new HashMap<>();
        map.put("a", "aaa");
        map.put("b", "bbb");
        map.put("c", "ccc");
        List<Object> $1 = Arrays.asList(map, 1, 2, "3");
        Object obj = parseResourceTemplate("jsonTemplate/0.json", $0, $1);
        System.out.println(gsonPretty.toJson(obj));
    }

}
