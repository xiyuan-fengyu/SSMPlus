package com.xiyuan.template.mongo.util;

import com.xiyuan.template.util.Util;
import org.bson.Document;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by xiyuan_fengyu on 2018/5/24 10:06.
 */
@SuppressWarnings("unchecked")
public class MongoShell {

    private static final Matcher placeholderM = Pattern.compile("\\$\\{(\\d+)}").matcher("");

    private static final Map<String, String> shells = new HashMap<>();

    private static String getShell(String resource) {
        return shells.computeIfAbsent(resource, key -> {
            try (InputStream in = MongoShell.class.getClassLoader().getResourceAsStream(resource)) {
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

    private static void filler(Object obj, Object params[]) {
        if (obj instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) obj).entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    placeholderM.reset((String) value);
                    if (placeholderM.find()) {
                        int index = Integer.parseInt(placeholderM.group(1));
                        if (index < params.length) {
                            entry.setValue(params[index]);
                        }
                    }
                }
                else {
                   filler(value, params);
                }
            }
        }
        else if (obj instanceof List) {
            List list = (List) obj;
            for (int i = 0, size = list.size(); i < size; i++) {
                Object subObj = list.get(i);
                if (subObj instanceof Map) {
                    filler(subObj, params);
                }
                else if (subObj instanceof String) {
                    placeholderM.reset((String) subObj);
                    if (placeholderM.find()) {
                        int index = Integer.parseInt(placeholderM.group(1));
                        if (index < params.length) {
                            list.set(i, params[index]);
                        }
                    }
                }
            }
        }
    }

    private static Object create(String resource, Object ...params) {
        Object shell = Util.gson.fromJson(getShell(resource), Object.class);
        if (params != null && params.length > 0) {
            filler(shell, params);
        }
        return shell;
    }

    public static Document doc(String resource, Object ...params) {
        Object obj = create(resource, params);
        return new Document((Map<String, Object>) obj);
    }

    public static List<Document> docs(String resource, Object ...params) {
        Object obj = create(resource, params);
        return ((List<Map<String, Object>>) obj).stream().map(Document::new).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        Object shell = create("mongo/aggregate_test.json",
                Arrays.asList("1XBI1_1526883863482", "jUm6V_1526883951882", "2506j_1526885621644")
        );
        System.out.println(Util.gsonFormat.toJson(shell));
    }

}
