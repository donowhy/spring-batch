package com.system.batch.origin.chapter3;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Data
public class Victim {
    private Long id;
    private String name;
    private String processId;
    private LocalDateTime terminatedAt;
    private String status;
}
