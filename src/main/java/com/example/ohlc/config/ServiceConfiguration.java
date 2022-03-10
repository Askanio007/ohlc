package com.example.ohlc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ServiceConfiguration {

    @Value("${executor.core.thread.pool}")
    private int coreThreadPool;
    @Value("${executor.max.pool.size}")
    private int maxPoolSize;
    @Value("${executor.queue.capacity}")
    private int queueCapacity;

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(coreThreadPool);
        taskExecutor.setMaxPoolSize(maxPoolSize);
        taskExecutor.setQueueCapacity(queueCapacity);
        taskExecutor.afterPropertiesSet();
        return taskExecutor;
    }
}
