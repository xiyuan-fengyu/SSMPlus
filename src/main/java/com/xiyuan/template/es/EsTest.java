package com.xiyuan.template.es;

import com.xiyuan.template.util.JsonTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xiyuan_fengyu on 2018/7/18 16:20.
 */
public class EsTest {

    public static void main(String[] args) {
        List<Map> userInfos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", i);
            userInfo.put("name", "user_" + i);
            userInfo.put("age", i + 10);
            userInfos.add(userInfo);
        }

        Object testBulk = JsonTemplate.parseResourceTemplate("es/test_bulk.json", userInfos);
        System.out.println(JsonTemplate.gsonPretty.toJson(testBulk));

//        ElasticSearch es = new ElasticSearch("http://192.168.1.150:9200");
//        List<Map> res = es.evalResource("es/test_bulk.json", userInfos);
//        System.out.println(JsonTemplate.gsonPretty.toJson(res));
    }

}
