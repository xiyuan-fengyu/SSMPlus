package com.xiyuan.template.mongo.exam;

import com.xiyuan.template.mongo.model.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by xiyuan_fengyu on 2017/12/8 11:41.
 */
@Component
public class MongoTemplateTest {

    private final Logger logger = LoggerFactory.getLogger(MongoTemplateTest.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    public void execute() {
//        Log log = new Log(0, 10, "first log",  new Date(), 0);
//        mongoTemplate.save(log);
//
//        Log savedLog = mongoTemplate.findById(0L, Log.class);
//        System.out.println(savedLog);

        List<Log> logs = mongoTemplate.find(new BasicQuery("{'id': {'$gte': 0}}"), Log.class);
        logs.forEach(System.out::println);
    }

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext(
                "spring/applicationContext.xml",
                "spring/springServlet.xml"
        );
        context.getBean(MongoTemplateTest.class).execute();
    }

}
