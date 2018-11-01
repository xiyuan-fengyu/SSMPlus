package com.xiyuan.template.thread;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xiyuan_fengyu on 2018/9/28 14:34.
 */
@Component
public class MyThreadPools {

    @Bean
    public ExecutorService fixedThreadPool() {
        return Executors.newFixedThreadPool(128);
    }

}
