package com.stan.stancore.extended.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@Component
@EnableAsync
public class VendingTaskExecutor {

    @Value("${vending-engine.corepoolsize:10}")
    private int notificationCore;

    @Value("${vending-engine.maxpoolsize:100}")
    private int notificationMax;

    @Value("${vending-engine.queuecapacity:100}")
    private int queueCapacity;


    @Bean(name = "disBurseFundsTaskExecutor")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(notificationCore);
        executor.setMaxPoolSize(notificationMax);
        executor.setQueueCapacity(queueCapacity);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setThreadNamePrefix("disBurseFundsTaskExecutor-");
        executor.initialize();
        return executor;
    }
}
