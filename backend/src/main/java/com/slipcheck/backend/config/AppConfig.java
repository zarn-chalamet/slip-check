package com.slipcheck.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AppConfig {

    /**
     * Bounded executor for asynchronous webhook processing: webhooks return 200 quickly,
     * slip work runs here. Bounded so a flood of webhook calls cannot exhaust threads.
     */
    @Bean(name = "slipExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor slipExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(64);
        executor.setThreadNamePrefix("slip-");
        executor.initialize();
        return executor;
    }
}
