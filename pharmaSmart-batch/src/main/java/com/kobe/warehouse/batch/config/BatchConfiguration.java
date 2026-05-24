package com.kobe.warehouse.batch.config;

import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfiguration {

    @Bean
    public JobOperator asyncJobOperator(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) throws Exception {
        JobOperatorFactoryBean factory = new JobOperatorFactoryBean();
        factory.setJobRepository(jobRepository);
        factory.setTransactionManager(transactionManager);
        factory.setTaskExecutor(new SimpleAsyncTaskExecutor("batch-"));
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
