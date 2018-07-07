package com.xiyuan.template.mongo.util;

import com.xiyuan.template.util.JsonTemplate;
import org.bson.Document;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by xiyuan_fengyu on 2018/5/24 10:06.
 */
@SuppressWarnings("unchecked")
public class MongoShell {

    private static Object create(String resource, Object ...params) {
        return JsonTemplate.parseResourceTemplate(resource, params);
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
        Object shell = create("mongo/aggregateTest.json",
                Arrays.asList("1XBI1_1526883863482", "jUm6V_1526883951882", "2506j_1526885621644")
        );
        System.out.println(JsonTemplate.gsonPretty.toJson(shell));

        long now = System.currentTimeMillis();
        long oneDay = 1000L * 3600 * 24;
        {
            Object obj = create("mongo/queryByIdCreateTime.json", ".*_.*", now - oneDay * 7, now);
            System.out.println(JsonTemplate.gsonPretty.toJson(obj));
        }
    }

}
