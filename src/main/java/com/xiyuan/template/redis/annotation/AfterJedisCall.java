package com.xiyuan.template.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by xiyuan_fengyu on 2018/9/6 14:47.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AfterJedisCall {

    String method();

    int[] argIndexsToMatchAsKey();

    String[] keyPatterns();

}
