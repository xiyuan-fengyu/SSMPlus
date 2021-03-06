package com.xiyuan.template.controller;

import com.baomidou.mybatisplus.plugins.Page;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class TemplateController {

    private final Logger logger = LoggerFactory.getLogger(TemplateController.class);

    @Autowired
    private Jedis jedis;

    @Autowired
    private LogDao logDao;

    private LoadingCache<String, String> cacheExample = CacheBuilder.newBuilder()
            .maximumSize(2048)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String s) throws Exception {
                    return null;
                }
            });

    @RequestMapping(value = "test", produces = "text/plain;charset=utf-8")
    @ResponseBody
    public Object test() {
        Map<String, String> data = new HashMap<>();
        data.put("id", "123");
        jedis.hmset("tb_log:id:1", data);
        return jedis.hgetAll("tb_log:id:1");
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
