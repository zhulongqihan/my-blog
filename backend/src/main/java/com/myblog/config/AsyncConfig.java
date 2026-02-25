package com.myblog.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * 设计目标：
 * 1. 配置异步任务线程池
 * 2. 支持@Async注解
 * 3. 合理设置线程池参数，避免资源浪费
 * 
 * 使用场景：
 * - 操作日志异步写入
 * - 邮件异步发送
 * - 数据统计异步计算
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    
    /**
     * 配置异步任务线程池
     * 
     * 参数设计理由：
     * - 核心线程数5：日常操作日志写入足够
     * - 最大线程数10：高峰期可扩展
     * - 队列容量100：缓冲突发请求
     * - 拒绝策略CallerRunsPolicy：队列满时由调用线程执行，避免任务丢失
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：线程池创建时的初始线程数
        executor.setCorePoolSize(5);
        
        // 最大线程数：线程池最多能创建的线程数
        executor.setMaxPoolSize(10);
        
        // 队列容量：核心线程都在工作时，新任务会放入队列
        executor.setQueueCapacity(100);
        
        // 线程名称前缀：便于日志追踪
        executor.setThreadNamePrefix("async-task-");
        
        // 空闲线程存活时间：超过核心线程数的线程，空闲60秒后销毁
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略：队列满时，由调用线程执行任务（避免任务丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间：最多等待60秒
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("异步任务线程池初始化完成，核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                 executor.getCorePoolSize(), 
                 executor.getMaxPoolSize(), 
                 executor.getQueueCapacity());
        
        return executor;
    }
}
