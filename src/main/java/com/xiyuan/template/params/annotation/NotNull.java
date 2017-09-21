package com.xiyuan.template.params.annotation;

import com.xiyuan.template.params.checker.Checker;
import com.xiyuan.template.params.checker.NotNullChecker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NotNull {

    String error();

    Class<? extends Checker> checker() default NotNullChecker.class;

}