package com.xiyuan.template.params.annotation;

import com.xiyuan.template.params.checker.Checker;
import com.xiyuan.template.params.checker.RangeChecker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 只对 number 字段有效
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Range {

    double min() default Double.MIN_VALUE;

    double max() default Double.MAX_VALUE;

    String error();

    Class<? extends Checker> checker() default RangeChecker.class;

}