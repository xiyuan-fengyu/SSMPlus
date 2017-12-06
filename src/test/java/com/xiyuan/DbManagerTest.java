package com.xiyuan;

import com.xiyuan.template.redis.manager.DbManager;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xiyuan_fengyu on 2017/12/5 16:08.
 */
@Component
public class DbManagerTest {

    private final Logger logger = LoggerFactory.getLogger(DbManagerTest.class);

    @Autowired
    private DbManager dbManager;

    public void execute() {
        Map<String, String> map = new HashMap<>();
        map.put("id", "0");
        map.put("content", "hmset " + System.currentTimeMillis());
        map.put("user_id", "17");
        map.put("create_time", "2017-12-05 12:00:00");
        dbManager.addToQueue("tb_log:id:0", map);
    }

    public static void main(String[] args) {
        PropertyConfigurator.configure(DbManagerTest.class.getClassLoader().getResource("property/log4j.properties"));
        ApplicationContext context = new ClassPathXmlApplicationContext(
                "spring/applicationContext.xml",
                "spring/springServlet.xml"
        );
        context.getBean(DbManagerTest.class).execute();
    }

}
