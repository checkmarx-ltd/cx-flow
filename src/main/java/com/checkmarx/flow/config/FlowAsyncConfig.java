package com.checkmarx.flow.config;

import com.checkmarx.flow.config.properties.FlowProperties;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.beans.ConstructorProperties;

@Configuration
public class FlowAsyncConfig implements AsyncConfigurer {

    private final FlowProperties properties;
    private static final int QUEUE_CAPACITY = 10000;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FlowAsyncConfig.class);

    @ConstructorProperties({"properties"})
    public FlowAsyncConfig(FlowProperties properties) {
        this.properties = properties;
    }

    @Bean("scanRequest")
    public ThreadPoolTaskExecutor scanRequestTaskExecutor() {
        int capacity = QUEUE_CAPACITY;
        if(properties.getScanResultQueue() != null){
            capacity = properties.getScanResultQueue();
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(capacity);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("scan-results");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean("webHook")
    public ThreadPoolTaskExecutor webHookTaskExecutor() {
        int capacity = QUEUE_CAPACITY;
        if(properties.getWebHookQueue() != null){
            capacity = properties.getWebHookQueue();
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(capacity);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("flow-web");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler () {
        return (throwable, method, objects) -> {
            log.error("Error has occurred calling {}. {}", method.getName(), ExceptionUtils.getMessage(throwable));
            for (Object param : objects) {
                log.error("Parameter value - {}", param);
            }
            log.error("Error details:", throwable);
        };
    }
}
