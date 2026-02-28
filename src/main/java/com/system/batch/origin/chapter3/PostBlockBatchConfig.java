package com.system.batch.origin.chapter3;

import jakarta.persistence.EntityManagerFactory;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PostBlockBatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job postBlockBatchJob(Step postBlockStep) {
        return new JobBuilder("postBlockBatchJob", jobRepository)
                .start(postBlockStep)
                .build();
    }

    @Bean
    public Step postBlockStep(
            JpaCursorItemReader<Post> postBlockReader,
            PostBlockProcessor postBlockProcessor,
            ItemWriter<BlockedPost> postBlockWriter
    ) {
        return new StepBuilder("postBlockStep", jobRepository)
                .<Post, BlockedPost>chunk(5, transactionManager)
                .reader(postBlockReader)
                .processor(postBlockProcessor)
                .writer(postBlockWriter)
                .build();
    }

    @Bean
    @StepScope
    public JpaCursorItemReader<Post> postBlockReader(
            @Value("#{jobParameters['startDateTime']}") String startDateTime,
            @Value("#{jobParameters['endDateTime']}") String endDateTime
    ) {
        // JobParameter로 전달된 String을 LocalDateTime으로 변환 (없으면 기본값 설정)
        LocalDateTime start = (startDateTime != null) ? LocalDateTime.parse(startDateTime) : LocalDateTime.now().minusDays(1);
        LocalDateTime end = (endDateTime != null) ? LocalDateTime.parse(endDateTime) : LocalDateTime.now();

        Map<String, Object> params = new HashMap<>();
        params.put("startDateTime", start);
        params.put("endDateTime", end);

        return new JpaCursorItemReaderBuilder<Post>()
                .name("postBlockReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT p FROM Post p JOIN FETCH p.reports r WHERE r.reportedAt >= :startDateTime AND r.reportedAt < :endDateTime")
                .parameterValues(params) // Null 안전한 HashMap 사용
                .build();
    }

    @Bean
    public ItemWriter<BlockedPost> postBlockWriter() {
        return items -> {
            items.forEach(blockedPost -> {
                log.info("💀 TERMINATED: [ID:{}] '{}' by {} | 신고:{}건 | 점수:{} | kill -9 at {}",
                        blockedPost.getPostId(),
                        blockedPost.getTitle(),
                        blockedPost.getWriter(),
                        blockedPost.getReportCount(),
                        String.format("%.2f", blockedPost.getBlockScore()),
                        blockedPost.getBlockedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            });
        };
    }

    @Getter
    @Builder
    @ToString
    public static class BlockedPost {
        private Long postId;
        private String writer;
        private String title;
        private int reportCount;
        private double blockScore;
        private LocalDateTime blockedAt;
    }

    @Component
    public static class PostBlockProcessor implements ItemProcessor<Post, BlockedPost> {
        @Override
        public BlockedPost process(Post post) {
            double blockScore = calculateBlockScore(post.getReports());
            if (blockScore >= 7.0) {
                return BlockedPost.builder()
                        .postId(post.getId())
                        .writer(post.getWriter())
                        .title(post.getTitle())
                        .reportCount(post.getReports().size())
                        .blockScore(blockScore)
                        .blockedAt(LocalDateTime.now())
                        .build();
            }
            return null;
        }

        private double calculateBlockScore(List<Report> reports) {
            return Math.random() * 10;
        }
    }
}
