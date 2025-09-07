package com.spaced_repetition_ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Número de threads para manter no pool
        executor.setMaxPoolSize(10); // Número máximo de threads
        executor.setQueueCapacity(25); // Tamanho da fila de espera
        executor.setThreadNamePrefix("Async-");
        executor.initialize();

        // Este é o passo mágico:
        // Envolve o nosso executor com um decorador que propaga o SecurityContext
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
