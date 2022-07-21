package com.checkmarx.flow.config;

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

    private static final int MAX_POOLSIZE = 200;
    private static final int CORE_POOL_SIZE = 50;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FlowAsyncConfig.class);

    @ConstructorProperties({"properties"})
    public FlowAsyncConfig(FlowProperties properties) {
        this.properties = properties;
    }

    @Bean("scanRequest")
    public ThreadPoolTaskExecutor scanRequestTaskExecutor() {

        int corePoolSize=CORE_POOL_SIZE;
        int maxPoolSize = MAX_POOLSIZE;
        int queueCapacity = QUEUE_CAPACITY;


        if(properties.getCorePoolSize() != null){
            corePoolSize = properties.getCorePoolSize();
        }

        if(properties.getMaxPoolSize() != null){
            maxPoolSize = properties.getMaxPoolSize();
        }

        if(properties.getQueuecapacityarg() != null){
            queueCapacity = properties.getQueuecapacityarg();
        }


        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("scan-results");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();

        return executor;
    }

    @Bean("webHook")
    public ThreadPoolTaskExecutor webHookTaskExecutor() {
        int corePoolSize=CORE_POOL_SIZE;
        int maxPoolSize = MAX_POOLSIZE;
        int queueCapacity = QUEUE_CAPACITY;

        if(properties.getCorePoolSize() != null){
            corePoolSize = properties.getCorePoolSize();
        }

        if(properties.getMaxPoolSize() != null){
            maxPoolSize = properties.getMaxPoolSize();
        }

        if(properties.getQueuecapacityarg() != null){
            queueCapacity = properties.getQueuecapacityarg();
        }


        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
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
