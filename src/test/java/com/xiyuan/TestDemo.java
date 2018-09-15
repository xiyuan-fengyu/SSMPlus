package com.xiyuan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import redis.clients.jedis.Jedis;

/**
 * Created by xiyuan_fengyu on 2018/9/15 10:19.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(value = {
        "classpath:spring/applicationContext*.xml"
})
class TestDemo {

    @Autowired
    private Jedis jedis;

    @Test
    void testJedis() {
        Object res = jedis.get("");
        System.out.println(res);
    }

}
