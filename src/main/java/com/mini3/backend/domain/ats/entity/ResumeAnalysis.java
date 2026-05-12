package com.mini3.backend.domain.ats.entity;

import com.mini3.backend.domain.ats.enums.AnalysisStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// 이력서 분석 엔티티
@Entity
@Table(name = "resume_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeAnalysis {

    @Id // PK 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id") // 이력서 분석 ID
    private Long analysisId;

    @ManyToOne(fetch = FetchType.LAZY) // 여러 개의 ResumeAnalysis가 하나의 Resume 에 속함.
    @JoinColumn(name = "resume_id", nullable = false) // Resume ID
    private Resume resume;

    @Enumerated(EnumType.STRING) // DB에는 숫자가 아니라 문자열로
    @Column(name = "status", nullable = false, length = 20)
    private AnalysisStatus status;

    @Column(name = "model", length = 100) // 모델 이름
    private String model;

    @Lob // Large Object
    @Column(name = "summary", columnDefinition = "TEXT") // 요약
    private String summary;

    @Lob // Large Object
    @Column(name = "result_json", columnDefinition = "TEXT") // 결과 JSON
    private String resultJson;

    /** HR 대시보드·정렬용 총점 (0~100 등). 세부 항목은 {@link #resultJson} */
    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "failure_message", length = 2000) // 실패 메시지
    private String failureMessage;

    @Column(name = "analyzed_at") // 분석 일시
    private LocalDateTime analyzedAt;

    @PrePersist // 엔티티가 DB에 INSERT 되기 전에 호출
    void prePersist() {
        if (this.status == null) {
            this.status = AnalysisStatus.PENDING;
        }
    }
}
