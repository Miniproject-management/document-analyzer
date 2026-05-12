package com.mini3.backend.domain.ats.entity;

import com.mini3.backend.domain.ats.enums.ResumeSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// 이력서 엔티티
@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id // PK 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resume_id") // 이력서 ID
    private Long resumeId;

    @ManyToOne(fetch = FetchType.LAZY) // 여러 개의 Resume가 하나의 Applicant 에 속함.
    @JoinColumn(name = "applicant_id", nullable = false) // Applicant ID
    private Applicant applicant;

    @Column(name = "title", length = 200) // 이력서 제목
    private String title;

    /**
     * 추출 텍스트 등(선택). 파일 본문은 S3 원본({@link #s3ObjectKey})을 사용한다.
     * 지원자가 입력하지 않으며, 서버에서만 채운다.
     */
    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING) // DB에는 숫자가 아니라 문자열로
    @Column(name = "source", nullable = false, length = 20)
    private ResumeSource source;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    /**
     * S3에 저장된 원본 파일 객체 키. 업로드 API에서 서버가 생성·저장한다(지원자 입력 아님).
     * 버킷은 {@code cloud.aws.s3.bucket} 설정을 사용한다.
     */
    @Column(name = "s3_object_key", length = 1024)
    private String s3ObjectKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
