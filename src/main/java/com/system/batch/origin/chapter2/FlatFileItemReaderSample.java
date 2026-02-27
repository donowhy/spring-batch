package com.system.batch.origin.chapter2;

import lombok.Data;

import java.util.List;

/**
 * FlatFileItemReader는 파일의 한 줄을 읽어 객체로 변환한다. 여기서 파일의 한 줄을 객체로 변환하는 역할은 DefaultLineMapper가 담당한다.
 * DefaultLineMapper는 LineTokenizer를 사용해 문자열을 각 필드로 토큰화하고, 토큰화 결과인 FieldSet을 FieldSetMapper에 전달한다.
 * FieldSetMapper의 기본 구현체인 BeanWrapperFieldSetMapper는 FieldSet에 지정된 필드 이름과 매핑할 객체의 프로퍼티 이름을 매핑해 최종적으로 우리가 사용할 객체를 생성한다.
 * 이때 객체의 setter 메서드를 사용한다.
 */
public class FlatFileItemReaderSample {
    public static class FiledSet {
        List<String> tokens;
        List<String> names;

        public FiledSet(List<String> tokens, List<String> names) {
            this.tokens = tokens;
            this.names = names;
        }
    }

    @Data
    public static class SystemFailure {
        private String errorId;        // "ERR001" 매핑
        private String errorDateTime;  // "2024-01-19 10:15:23" 매핑
        private String severity;       // "CRITICAL" 매핑
        private Integer processId;     // "1234" -> 1234로 타입 변환 후 매핑
        private String errorMessage;   // "SYSTEM_CRASH" 매핑
    }

    public static FiledSet filedSet = new FiledSet(List.of("ERR001", "2024-01-19 10:15:23", "CRITICAL", "1234", "SYSTEM_CRASH"),
            List.of("errorId", "errorDateTime", "severity", "processId", "errorMessage"));
}
