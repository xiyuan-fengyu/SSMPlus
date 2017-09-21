package com.xiyuan.template.params.checker;


import com.xiyuan.template.params.annotation.Range;

public class RangeChecker implements Checker<Range> {

    @Override
    public boolean valid(Range anno, Object value) {
        if (value == null) return false;

        if (value instanceof Number) {
            Number num = (Number) value;
            return num.doubleValue() >= anno.min() && num.doubleValue() <= anno.max();
        }
        return true;
    }

}
