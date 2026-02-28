package com.system.batch.origin.chapter3;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    private String reportType;     // SPAM, ABUSE, ILLEGAL, FAKE_NEWS ...
    private int reporterLevel;     // 신고자 신뢰도 (1~5)
    private String evidenceData;   // 증거 데이터 (URL 등)
    private LocalDateTime reportedAt;
}
