package com.example.gitreview.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步配置
 * 配置代码审查异步执行的线程池
 *
 * @author zhourui(V33215020)
 * @since 2025/10/03
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * 代码审查异步执行器
     * 配置专用的线程池用于代码审查任务
     */
    @Bean(name = "reviewExecutor")
    public Executor reviewExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：同时最多5个审查任务
        executor.setCorePoolSize(5);

        // 最大线程数：高峰时最多10个审查任务
        executor.setMaxPoolSize(10);

        // 队列容量：等待队列最多50个任务
        executor.setQueueCapacity(50);

        // 线程名前缀
        executor.setThreadNamePrefix("review-async-");

        // 拒绝策略：队列满时由调用线程执行
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：最多等待60秒
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        logger.info("Review executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * 默认异步执行器
     */
    @Override
    public Executor getAsyncExecutor() {
        return reviewExecutor();
    }

    /**
     * 异步异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            logger.error("异步任务执行失败: method={}, params={}",
                    method.getName(), params, throwable);
        };
    }
}
