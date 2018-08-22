package com.xiyuan.template.controller;

import com.baomidou.mybatisplus.plugins.Page;
import com.xiyuan.template.mybatis.dao.LogDao;
import com.xiyuan.template.mybatis.entity.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

@Controller
public class TemplateController {

    private final Logger logger = LoggerFactory.getLogger(TemplateController.class);

    @Autowired
    private Jedis jedis;

    @Autowired
    private LogDao logDao;

    @RequestMapping(value = "test", produces = "text/plain;charset=utf-8")
    @ResponseBody
    public String test() {
        logger.info(jedis.hget("tb_log:id:0", "content"));
        logger.info(jedis.hgetAll("tb_log:id:0").toString());
        return "测试";
    }

    @RequestMapping(value = "test/ftl")
    public String testFtl(Model model) {
        model.addAttribute("user", "Tom");
        return "testFtl";
    }

    @RequestMapping(value = "test/jsp")
    public String testJsp(Model model) {
        model.addAttribute("title", "test");
        model.addAttribute("msg", "Hello, JSP!");
        return "testJsp";
    }

    @RequestMapping(value = "test/mybatis/page")
    @ResponseBody
    public Object testMybatisPage(int current, int pageSize) {
        Page<Log> logPage = new Page<>(current, pageSize, "time", false);
        logPage.setRecords(logDao.selectPage(logPage));
        return logPage;
    }

}
