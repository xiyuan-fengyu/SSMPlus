package com.xiyuan.template.mongo.util;

import com.xiyuan.template.util.JsonUtil;
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

    private static Object create(String resource, Object ...params) {
        return JsonUtil.parseResourceTemplate(resource, params);
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
