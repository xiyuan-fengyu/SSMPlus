package com.xiyuan.template.params.annotation;

import com.xiyuan.template.params.checker.Checker;
import com.xiyuan.template.params.checker.StrLenChecker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface StrLen {

    long min() default 0L;

    long max() default Long.MAX_VALUE;

    String error();

    Class<? extends Checker> checker() default StrLenChecker.class;

}