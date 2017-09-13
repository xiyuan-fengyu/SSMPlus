package com.xiyuan.template.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TemplateController {

    @RequestMapping(value = "test", produces = "text/plain;charset=utf-8")
    @ResponseBody
    public String test() {
        return "测试";
    }

}
