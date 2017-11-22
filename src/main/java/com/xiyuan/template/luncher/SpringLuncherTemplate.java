package com.xiyuan.template.luncher;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Created by xiyuan_fengyu on 2017/11/22 11:03.
 */
@Component
public class SpringLuncherTemplate {

    private final Logger logger = LoggerFactory.getLogger(SpringLuncherTemplate.class);

    public void execute() {

    }

    public static void main(String[] args) {
        PropertyConfigurator.configure(SpringLuncherTemplate.class.getClassLoader().getResource("property/log4j.properties"));
        ApplicationContext context = new ClassPathXmlApplicationContext(
                "spring/applicationContext.xml",
                "spring/springServlet.xml"
        );
        context.getBean(SpringLuncherTemplate.class).execute();
    }

}
