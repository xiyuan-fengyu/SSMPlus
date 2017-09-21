package com.xiyuan.template.params;

import com.xiyuan.template.params.annotation.JsExp;
import com.xiyuan.template.params.annotation.Match;
import com.xiyuan.template.params.annotation.Range;
import com.xiyuan.template.params.annotation.StrLen;

public class Test {

    private static class UserParams extends Params {

        @StrLen(min = 5, max = 10, error = "用户id有误")
        public long id;

        @JsExp(exp = "it && it.match(/^1[34578]\\d{9}$/)", error = "电话有误")
        public String phone;

        @Match(regex = "boy|girl|unknow", error = "性别有误")
        public String sex;

        @Range(min = 0, max = 150, error = "年龄有误")
        public int age;

    }

    public static void main(String[] args) {
        UserParams params = new UserParams();
        params.id = 123456L;
        params.phone = "18918284763";
        params.sex = "boy";
        params.age = 20;
        System.out.println(params.check());
    }

}