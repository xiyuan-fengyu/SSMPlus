package com.xiyuan.template.controller;

import com.xiyuan.template.redis.JedisWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

@Controller
public class TemplateController {

    private final Logger logger = LoggerFactory.getLogger(TemplateController.class);

    @Autowired
    private Jedis jedis;

    @RequestMapping(value = "test", produces = "text/plain;charset=utf-8")
    @ResponseBody
    public String test() {
        logger.info(jedis.hget("tb_log:id:0", "content"));
        logger.info(jedis.hgetAll("tb_log:id:0").toString());
        return "测试";
    }

}
