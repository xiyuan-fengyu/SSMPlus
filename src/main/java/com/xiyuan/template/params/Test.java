package com.xiyuan.template.params;

import com.xiyuan.template.params.annotation.JsExp;
import com.xiyuan.template.params.annotation.Match;
import com.xiyuan.template.params.annotation.Range;
import com.xiyuan.template.params.annotation.StrLen;

public class Test {

    public static class PersonParams extends Params {

        @StrLen(min = 5, max = 10, error = "用户id有误")
        private long id;

        @JsExp(exp = "it && it[0] != '_'", error = "名字有误")
        private String name;

        @JsExp(exp = "it && it.match(/^1[34578]\\d{9}$/)", error = "电话有误")
        private String phone;

        @Match(regex = "boy|girl|unknow", error = "性别有误")
        private String sex;

        @Range(min = 0, max = 150, error = "年龄有误")
        private int age;

        @JsExp(exp = "it == null || it.sex != ctx.sex", error = "配偶信息有误")
        private PersonParams spouse;

        @JsExp(exp = "" +
                "var result = true;" +
                "for (var i = 0; i < it.length; i++) {" +
                "   if (it[i].contains('游戏')) {" +
                "       result = false;" +
                "       break;" +
                "   }" +
                "}" +
                "result;",
                error = "兴趣信息有误")
        private String[] interests;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getSex() {
            return sex;
        }

        public void setSex(String sex) {
            this.sex = sex;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public PersonParams getSpouse() {
            return spouse;
        }

        public void setSpouse(PersonParams spouse) {
            this.spouse = spouse;
        }

        public String[] getInterests() {
            return interests;
        }

        public void setInterests(String[] interests) {
            this.interests = interests;
        }
    }

    public static void main(String[] args) {
        PersonParams params = new PersonParams();
        params.id = 123456L;
        params.name = "xiyuan";
        params.phone = "18918284763";
        params.sex = "boy";
        params.age = 20;
        params.interests = new String[]{"游戏", "电影"};

        PersonParams another = new PersonParams();
        another.id = 123459L;
        another.phone = "18918284123";
        another.sex = "girl";
        another.age = 20;

        params.spouse = another;
        another.spouse = params;

        System.out.println(params.check());
        System.out.println(another.check());
    }

}