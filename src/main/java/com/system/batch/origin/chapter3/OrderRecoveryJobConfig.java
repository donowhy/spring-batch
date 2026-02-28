package com.system.batch.origin.chapter3;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 개요
 * JdbcPagingItemReader로 해커가 조작한 주문들을 식별한다. (주문 상태가 비정상적으로 SHIPPED 또는 CANCELLED로 변경된 주문)
 * ItemProcessor에서 각 주문의 실제 배송 이력(shipping_id 존재 여부)을 기준으로 올바른 상태를 판단한다.
 * (shipping_id가 없다면 READY_FOR_SHIPMENT로, 있다면 SHIPPED로 변경)
 * JdbcBatchItemWriter로 ItemProcessor에서 결정된 정상 상태를 데이터베이스에 반영한다.
 */
@Configuration
@RequiredArgsConstructor
public class OrderRecoveryJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Bean
    public Job orderRecoveryJob() {
        return new JobBuilder("orderRecoveryJob", jobRepository)
                .start(orderRecoveryStep())
                .build();
    }

    @Bean
    public Step orderRecoveryStep() {
        return new StepBuilder("orderRecoveryStep", jobRepository)
                .<HackedOrder, HackedOrder>chunk(10, transactionManager)
                .reader(compromisedOrderReader())
                .processor(orderStatusProcessor())
                .writer(orderStatusWriter())
                .build();
    }

    @Bean
    public JdbcPagingItemReader<HackedOrder> compromisedOrderReader() {
        return new JdbcPagingItemReaderBuilder<HackedOrder>()
                .name("compromisedOrderReader")
                .dataSource(dataSource)
                .pageSize(10)
                .selectClause("SELECT id, customer_id, order_datetime, status, shipping_id")
                .fromClause("FROM orders")
                .whereClause("WHERE (status = 'SHIPPED' and shipping_id is null) " +
                        "or (status = 'CANCELLED' and shipping_id is not null)")
                .sortKeys(Map.of("id", Order.ASCENDING))
                // BeanPropertyRowMapper : Order클래스의 필드명과 DB 컬럼명을 자동으로 매핑한다.
                .beanRowMapper(HackedOrder.class)
                .build();
    }

    @Bean
    public ItemProcessor<HackedOrder, HackedOrder> orderStatusProcessor() {
        return order -> {
            if (order.getShippingId() == null) {
                order.setStatus(OrderStatus.READY_FOR_SHIPMENT.name());
            } else {
                order.setStatus(OrderStatus.SHIPPED.name());
            }
            return order;
        };
    }

    @Bean
    public JdbcBatchItemWriter<HackedOrder> orderStatusWriter() {
        return new JdbcBatchItemWriterBuilder<HackedOrder>()
                .dataSource(dataSource)
                .sql("UPDATE orders SET status = :status WHERE id = :id")
                .beanMapped()
                /**
                 * true (기본값)
                 * - 단 하나의 데이터라도 업데이트(또는 추가)에 실패하면 즉시 예외를 던져 작전을 중단한다.
                 * - 우리의 복구 작전처럼 모든 데이터가 반드시 정상화되어야 하는 경우에 사용
                 * false
                 * - 일부 데이터가 업데이트되지 않아도 작전을 계속 진행한다
                 * - 중복 데이터 처리 구문(INSERT IGNORE 또는 INSERT ... ON CONFLICT DO NOTHING)을 사용할 때나
                 *   조건부 UPDATE처럼 일부 데이터는 변경되지 않아도 되는 상황에서 사용
                 */
                .assertUpdates(true)
                .build();
    }

    @Data
    @NoArgsConstructor
    public static class HackedOrder {
        private Long id;
        private Long customerId;
        private LocalDateTime orderDateTime;
        private String status;
        private String shippingId;
    }

    public enum OrderStatus {
        READY_FOR_SHIPMENT, SHIPPED, CANCELLED
    }
}
