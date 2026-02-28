package com.system.batch.origin.chapter3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j

public class HelloWorldJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public HelloWorldJobConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public Job helloWorldJob() {
        return new JobBuilder("helloWorldJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(helloWorldStep())
                .build();
    }

    @Bean
    public Step helloWorldStep() {
        return new StepBuilder("helloWorldStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("*******************************************************");
                    log.info("⭐ System Check Complete: Execution Chamber Ready. ⭐");
                    log.info("*******************************************************");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
