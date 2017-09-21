package com.xiyuan.template.params.checker;


public interface Checker<T> {

    boolean valid(T anno, Object value);

}
