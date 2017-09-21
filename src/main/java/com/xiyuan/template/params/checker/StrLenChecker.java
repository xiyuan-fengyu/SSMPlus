package com.xiyuan.template.params.checker;


import com.xiyuan.template.params.annotation.StrLen;

public class StrLenChecker implements Checker<StrLen> {

    @Override
    public boolean valid(StrLen anno, Object value) {
        if (value == null) return false;

        int strLen = value.toString().length();
        return strLen >= anno.min() && strLen <= anno.max();
    }

}
