package mx.edu.utch.dgs.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AsyncConfig {

    @Bean(name = "upstreamExecutor", destroyMethod = "shutdown")
    ExecutorService upstreamExecutor() {
        return new ThreadPoolExecutor(
                8,
                16,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(40),
                threadFactory("upstream-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean
    AsyncTaskExecutor gatewayAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("gateway-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    WebMvcConfigurer asyncWebMvc(AsyncTaskExecutor gatewayAsyncExecutor) {
        return new WebMvcConfigurer() {
            @Override
            public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
                configurer.setTaskExecutor(gatewayAsyncExecutor);
                configurer.setDefaultTimeout(15_000);
            }
        };
    }

    private static ThreadFactory threadFactory(String prefix) {
        AtomicInteger sequence = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + sequence.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        };
    }
}
