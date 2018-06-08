package com.xiyuan.template.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
example:
    {
      "testMap": "${0}",
      "${0}": null,
      "testArr": [
        {
          "${0}": "a.*"
        },
        {
          "${0}": "-:a.*"
        },
        {
          "${4}": ["b.*", "-:a.*"]
        }
      ],
      "testArrSub": [1, "${5}", 3],
      "testArrSubCut 0": [1, "${5,0}", 3],
      "testArrSubCut 0,1,2": [1, "${5,0,1,2}", 3],
      "testArrSubCut 0:": [1, "${5,0:}", 3],
      "testArrSubCut 0:-2": [1, "${5,0:-2}", 3],
      "testArrSubCut 0,2:-1": [1, "${5,0,2:-1}", 3]
    }
 */
/**
 * Created by xiyuan_fengyu on 2018/6/8 9:16.
 */
@SuppressWarnings("unchecked")
public class JsonUtil {

    public static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").serializeNulls().create();

    private static final Matcher placeholderM = Pattern.compile("\\$\\{ *(\\d+) *}").matcher("");

    private static final Matcher unwindArrPlaceholderM = Pattern.compile("\\$\\{ *(\\d+) *(, *((\\d+)( *: *(-?\\d+)?)?))*}").matcher("");

    private static final Map<String, String> resourceCaches = new HashMap<>();

    public static Object parseTemplate(String template, Object ...params) {
        return filler(gson.fromJson(template, Object.class), params);
    }

    public static Object parseResourceTemplate(String resource, Object ...params) {
        return parseTemplate(getResourceContent(resource), params);
    }

    private static String getResourceContent(String resource) {
        return resourceCaches.computeIfAbsent(resource, key -> {
            try (InputStream in = JsonUtil.class.getClassLoader().getResourceAsStream(resource)) {
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

        if (obj instanceof Map) {
            Map<String, Object> fillerObj = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : ((Map<String, Object>) obj).entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // 先检查 key 是否为 Map 对象， 如果是，则以 value 对应的键值展开，如果为 null， 全展开
                placeholderM.reset(key);
                if (placeholderM.find()) {
                    int index = Integer.parseInt(placeholderM.group(1));
                    Map unwindMap = (Map) params[index];
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
                else {
                    fillerObj.put(key, filler(value, params));
                }
            }
            return fillerObj;
        }
        else if (obj instanceof List) {
            List<Object> fillerList = new ArrayList();
            List list = (List) obj;
            for (Object o : list) {
                if (o instanceof String) {
                    boolean match = false;
                    placeholderM.reset((CharSequence) o);
                    if (placeholderM.find()) {
                        match = true;
                        int index = Integer.parseInt(placeholderM.group(1));
                        if (index < params.length) {
                            fillerList.add(params[index]);
                        }
                    }

                    if (!match) {
                        unwindArrPlaceholderM.reset((CharSequence) o);
                        if (unwindArrPlaceholderM.find()) {
                            match = true;
                            String temp = ((String) o).trim();
                            String[] split = temp.substring(2, temp.length() - 1).split(",");
                            int paramI = Integer.parseInt(split[0].trim());
                            List<Object> unwindList = (List<Object>) params[paramI];
                            for (int i = 1, len = split.length; i < len; i++) {
                                String[] fromToS = (split[i] + ' ').split(":");
                                if (fromToS.length == 1) {
                                    fillerList.add(unwindList.get(Integer.parseInt(fromToS[0].trim())));
                                }
                                else {
                                    int from = Integer.parseInt(fromToS[0].trim());
                                    int to;
                                    String toS = fromToS[1].trim();
                                    if (toS.isEmpty()) to = list.size();
                                    else {
                                        to = Integer.parseInt(toS);
                                        if (to < 0) {
                                            to = unwindList.size() + to;
                                        }
                                    }
                                    for (int j = from; j < to; j++) {
                                        fillerList.add(unwindList.get(j));
                                    }
                                }
                            }
                        }
                    }

                    if (!match) {
                        fillerList.add(o);
                    }
                }
                else fillerList.add(filler(o, params));
            }
            return fillerList;
        }
        else if (obj instanceof String) {
            placeholderM.reset((String) obj);
            if (placeholderM.find()) {
                int index = Integer.parseInt(placeholderM.group(1));
                if (index < params.length) {
                    return params[index];
                }
            } else return obj;
        }
        return obj;
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

}
