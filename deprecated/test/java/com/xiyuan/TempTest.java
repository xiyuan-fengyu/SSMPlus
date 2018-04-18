package com.xiyuan;

import redis.clients.jedis.Jedis;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xiyuan_fengyu on 2017/12/1 14:09.
 */
public class TempTest {
    public static void main(String[] args) {
//        Class<Jedis> jedisClass = Jedis.class;
//        for (Method method : jedisClass.getDeclaredMethods()) {
//            int modifier = method.getModifiers();
//            if (!Modifier.isPublic(modifier) || Modifier.isStatic(modifier) || method.getReturnType() == Void.class) {
//                continue;
//            }
//
//            if (method.getReturnType() == Set.class || method.getReturnType() == List.class) {
//                System.out.println(method);
//            }
//        }


//        System.out.println(Collection.class.isAssignableFrom(Set.class));


//        System.out.println("a123b".matches("123"));

        Matcher fieldAndValueMatcher = Pattern.compile("\\$\\{(.+?)\\}").matcher("${user_id}:valid:${status=0}:tb_log:id");
        String str = null;
        while (fieldAndValueMatcher.find()) {
            String fieldAndValue = fieldAndValueMatcher.group(1);
            int equalIndex = fieldAndValue.indexOf('=');
            String field;
            if (equalIndex > -1) {
                field = fieldAndValue.substring(0, equalIndex);
                str = fieldAndValueMatcher.replaceAll(fieldAndValue);
            }
            else {
                field = fieldAndValue;
                str = fieldAndValueMatcher.replaceAll(field);
            }
        }
        System.out.println(str);


    }
}
