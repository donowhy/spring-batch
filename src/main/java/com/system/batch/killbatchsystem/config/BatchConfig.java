package com.system.batch.killbatchsystem.config;

import javax.sql.DataSource;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
// DefaultBatchConfiguration 상속 처리 : JobRepository, JobLauncher 등 Spring Batch의 핵심 컴포넌트들을 자동으로 구성
public class BatchConfig extends DefaultBatchConfiguration {


  // 컴포넌트들이 필요로 하는 DataSource와 PlatformTransactionManager Bean을 직접 구성
  @Bean
  public DataSource dataSource() {
    return new EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.H2)
        .addScript("org/springframework/batch/core/schema-h2.sql")
        .build();
  }

  @Bean
  public PlatformTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
  }
}
