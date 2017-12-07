package com.xiyuan;

import com.xiyuan.template.util.DateUtil;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * Created by xiyuan_fengyu on 2017/12/1 13:53.
 */
@Component
public class JedisWrapperTest {

    private final Logger logger = LoggerFactory.getLogger(JedisWrapperTest.class);

    @Autowired
    private Jedis jedis;

    public void execute() {

//        jedis.hset("tb_log:id:2", "create_time", "2017-12-07 10:36:19");

//        String id = "2";
//        Map<String, String> map = new HashMap<>();
//        map.put("id", id);
//        map.put("content", "hmset test; db test; " + System.currentTimeMillis());
//        map.put("user_id", "10");
//        map.put("create_time", DateUtil.format(new Date()));
//        logger.info(jedis.hmset("tb_log:id:" + id, map));


//        String res = jedis.hget("tb_log:id:2", "content");
//        logger.info(res);

//        Map<String, String> mapRes = jedis.hgetAll("tb_log:id:2");
//        logger.info(mapRes.toString());


//        Set<String> ids = jedis.zrange("tb_user:id:10:tb_log:id", 0, -1);
//        for (String id : ids) {
//            System.out.println(id);
//        }

//        Set<String> ids = jedis.zrange("all:tb_log:id", 0, -1);
//        for (String id : ids) {
//            System.out.println(id);
//        }
    }

    public static void main(String[] args) {
        PropertyConfigurator.configure(JedisWrapperTest.class.getClassLoader().getResource("property/log4j.properties"));
        ApplicationContext context = new ClassPathXmlApplicationContext(
                "spring/applicationContext.xml",
                "spring/springServlet.xml"
        );
        context.getBean(JedisWrapperTest.class).execute();

        Scanner scanner = new Scanner(System.in);
        String line;
        while ((line = scanner.nextLine()) != null && !line.equals("quit")) {}
    }

}
