package com.system.batch.origin.chapter3;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class VictimRecordConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Bean
    public Job processVictimJob() {
        return new JobBuilder("victimRecordJob", jobRepository)
                .start(processVictimStep())
                .build();
    }

    @Bean
    public Step processVictimStep() {
        return new StepBuilder("victimRecordStep", jobRepository)
                .<Victim, Victim>chunk(5, transactionManager)
                .reader(terminatedVictimReader())
                .writer(victimWriter())
                .build();
    }

//    @Bean
//    public JdbcCursorItemReader<Victim> terminatedVictimReader() {
//        return new JdbcCursorItemReaderBuilder<Victim>()
//                .name("terminatedVictimReader")
//                .dataSource(dataSource)
//                .sql("SELECT * FROM victims WHERE status = ? AND terminated_at <= ?")
//                .queryArguments(List.of("TERMINATED", LocalDateTime.now()))
//                // 쿼리 결과(ResultSet)를 Java 객체(Victim)로 변환하는 역할
//                // 커스텀 변환 로직이 필요하다면 빌더의 rowMapper() 메서드에 커스텀 RowMapper 구현체를 지정
//                .beanRowMapper(Victim.class)
//                .build();
//    }

//    @Bean
//    public JdbcPagingItemReader<Victim> terminatedVictimReader() {
//        return new JdbcPagingItemReaderBuilder<Victim>()
//                .name("terminatedVictimReader")
//                .dataSource(dataSource)
//                .pageSize(5)
//                .selectClause("select id, name, process_id, terminated_at, status")
//                .fromClause("where status = :status and terminated_at <= :terminatedAt")
//                // Keyset Pagination 방식 지원
//                .sortKeys(Map.of("id", Order.ASCENDING))
//                .parameterValues(Map.of(
//                        "status", "TERMINATED",
//                        "terminatedAt", LocalDateTime.now()
//                ))
//                .beanRowMapper(Victim.class)
//                .build();
//    }

    @Bean
    public JdbcPagingItemReader<Victim> terminatedVictimReader() {
        return new JdbcPagingItemReaderBuilder<Victim>()
                .name("terminatedVictimReader")
                .dataSource(dataSource)
                .pageSize(5)
                .queryProvider(pagingQueryProvider(dataSource)) // 커스텀 PagingQueryProvider 적용
                .parameterValues(Map.of(
                        "status", "TERMINATED",
                        "terminatedAt", LocalDateTime.now()
                ))
                .beanRowMapper(Victim.class)
                .build();
    }

    private PagingQueryProvider pagingQueryProvider(DataSource dataSource) {
        SqlPagingQueryProviderFactoryBean queryProviderFactory = new SqlPagingQueryProviderFactoryBean();

        queryProviderFactory.setDataSource(dataSource);
        queryProviderFactory.setSelectClause("SELECT id, name, process_id, terminated_at, status");
        queryProviderFactory.setFromClause("FROM victims");
        queryProviderFactory.setWhereClause("WHERE status = :status AND terminated_at <= :terminatedAt");
        queryProviderFactory.setSortKeys(Map.of("id", Order.ASCENDING));

        try {
            return queryProviderFactory.getObject();
        } catch (Exception e) {
            throw new IllegalStateException("PagingQueryProvider 빈 생성에 실패했습니다.", e);
        }
    }

    @Bean
    public ItemWriter<Victim> victimWriter() {
        return items -> {
            for (Victim victim : items) {
                log.info("{}", victim);
            }
        };
    }
}
