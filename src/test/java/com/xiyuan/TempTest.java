package com.xiyuan;

import redis.clients.jedis.Jedis;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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


        System.out.println(Collection.class.isAssignableFrom(Set.class));


//        System.out.println("a123b".matches("123"));
    }
}
