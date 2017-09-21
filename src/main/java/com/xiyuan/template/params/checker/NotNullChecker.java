package com.xiyuan.template.params.checker;

import com.xiyuan.template.params.annotation.NotNull;

public class NotNullChecker implements Checker<NotNull> {

    @Override
    public boolean valid(NotNull anno, Object value, Object ctx) {
        return value != null;
    }

}
