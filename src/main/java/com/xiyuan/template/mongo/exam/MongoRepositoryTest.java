package com.xiyuan.template.mongo.exam;

import com.xiyuan.template.mongo.respository.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Created by xiyuan_fengyu on 2017/12/8 13:59.
 */
@Component
public class MongoRepositoryTest {

    private final Logger logger = LoggerFactory.getLogger(MongoRepositoryTest.class);

    @Autowired
    private LogRepository logRepository;

    public void execute() {
//        Log log = new Log(1, 10, "first log",  new Date(), 0);
//        logRepository.save(log);
//
//        Optional<Log> savedLog = logRepository.findById(0L);
//        savedLog.ifPresent(System.out::println);
//
//        List<Log> logsByUserId = logRepository.findLogsByUserIdEquals(10);
//        logsByUserId.forEach(System.out::println);
//
//        Date now = new Date();
//        List<Log> logsByCreateTime = logRepository.findLogsByCreateTimeBetween(new Date(now.getTime() - 3600000L * 24), now);
//        logsByCreateTime.forEach(System.out::println);
//
//        Date now = new Date();
//        List<Log> logsByCreateTime = logRepository.queryByCreateTimeBetween(new Date(now.getTime() - 3600000L * 24), now);
//        logsByCreateTime.forEach(System.out::println);
    }

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext(
                "spring/applicationContext.xml",
                "spring/springServlet.xml"
        );
        context.getBean(MongoRepositoryTest.class).execute();
    }

}
