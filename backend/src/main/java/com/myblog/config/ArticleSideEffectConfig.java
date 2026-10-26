package com.myblog.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Optional projections and auditing must never borrow a request thread when their queues fill. */
@Configuration
public class ArticleSideEffectConfig {
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor articleProjectionExecutor() {
        return worker("article-projection", 128);
    }

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor operationAuditExecutor() {
        return worker("operation-audit", 256);
    }

    private ThreadPoolExecutor worker(String name, int queueCapacity) {
        return new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity), task -> {
                    Thread thread = new Thread(task, name);
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }
}
