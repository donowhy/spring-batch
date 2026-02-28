package com.system.batch.origin;

import com.system.batch.origin.chapter1.SystemTerminationConfig;
import com.system.batch.origin.chapter2.SystemFailureJobConfig;
import com.system.batch.origin.chapter3.HelloWorldJobConfig;
import com.system.batch.origin.chapter3.VictimRecordConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    SystemTerminationConfig.class,
    SystemFailureJobConfig.class,
    HelloWorldJobConfig.class,
    VictimRecordConfig.class
})
public class BatchConfig {
    // 중앙에서 모든 Chapter 설정을 명시적으로 관리합니다.
}
