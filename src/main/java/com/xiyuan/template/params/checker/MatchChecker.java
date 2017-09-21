package com.xiyuan.template.params.checker;


import com.xiyuan.template.params.annotation.Match;

public class MatchChecker implements Checker<Match> {

    @Override
    public boolean valid(Match anno, Object value) {
        if (value == null) return false;

        String str = value.toString();
        return str.matches(anno.regex());
    }

}
