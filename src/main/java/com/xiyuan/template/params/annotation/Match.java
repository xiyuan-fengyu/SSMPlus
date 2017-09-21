package com.xiyuan.template.params.annotation;

import com.xiyuan.template.params.checker.Checker;
import com.xiyuan.template.params.checker.MatchChecker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Match {

    String regex();

    String error();

    Class<? extends Checker> checker() default MatchChecker.class;

}