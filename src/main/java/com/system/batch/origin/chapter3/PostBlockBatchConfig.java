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
import java.util.List;
import java.util.Map;

/**
 * JpaCursorItemReader
 *     │
 *     ├────── queryString(JPQL) or JpaQueryProvider
 *     │        └─ (Query 생성에 사용됨)
 *     │
 *     ├────── EntityManager
 *     │        └─ (JPA 핵심 엔진)
 *     │
 *     └────── Query
 *              └─ (EntityManager가 생성하는 실행 가능한 쿼리 인스턴스)
 */
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
            @Value("#{jobParameters['startDateTime']}") LocalDateTime startDateTime,
            @Value("#{jobParameters['endDateTime']}") LocalDateTime endDateTime
    ) {
        return new JpaCursorItemReaderBuilder<Post>()
                .name("postBlockReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("""
                        SELECT p FROM Post p JOIN FETCH p.reports r
                        WHERE r.reportedAt >= :startDateTime AND r.reportedAt < :endDateTime
                        """)
                .parameterValues(Map.of(
                        "startDateTime", startDateTime,
                        "endDateTime", endDateTime
                ))
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

    /**
     * 차단된 게시글 - 처형 결과 보고서
     */
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
            // 각 신고의 신뢰도를 기반으로 차단 점수 계산
            double blockScore = calculateBlockScore(post.getReports());

            // 차단 점수가 기준치를 넘으면 처형 결정
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

            return null;  // 무죄 방면
        }

        private double calculateBlockScore(List<Report> reports) {
            // 각 신고들의 정보를 시그니처에 포함시켜 마치 사용하는 것처럼 보이지만...
            for (Report report : reports) {
                analyzeReportType(report.getReportType());            // 신고 유형 분석
                checkReporterTrust(report.getReporterLevel());        // 신고자 신뢰도 확인
                validateEvidence(report.getEvidenceData());           // 증거 데이터 검증
                calculateTimeValidity(report.getReportedAt());        // 시간 가중치 계산
            }

            // 실제로는 그냥 랜덤 값을 반환
            return Math.random() * 10;  // 0~10 사이의 랜덤 값
        }

        // 아래는 실제로는 아무것도 하지 않는 메서드들
        private void analyzeReportType(String reportType) {
            // 신고 유형 분석하는 척
        }

        private void checkReporterTrust(int reporterLevel) {
            // 신고자 신뢰도 확인하는 척
        }

        private void validateEvidence(String evidenceData) {
            // 증거 검증하는 척
        }

        private void calculateTimeValidity(LocalDateTime reportedAt) {
            // 시간 가중치 계산하는 척
        }
    }
}
