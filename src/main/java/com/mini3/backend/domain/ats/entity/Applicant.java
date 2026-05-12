package com.mini3.backend.domain.ats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// 지원자 엔티티
@Entity
@Table(name = "applicants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Applicant {

    @Id // PK 기본키
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "applicant_id") // 지원자 ID
    private Long applicantId;

    @Column(name = "name", nullable = false, length = 100) // 지원자 이름
    private String name;

    @Column(name = "email", length = 255) // 지원자 이메일
    private String email;

    @Column(name = "phone", length = 50) // 지원자 전화번호
    private String phone;

    @Column(name = "created_at", nullable = false) // 지원자 생성 일시
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false) // 지원자 수정 일시
    private LocalDateTime updatedAt;

    @PrePersist // 엔티티가 DB에 INSERT 되기 전에 호출
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate // 엔티티가 DB에 UPDATE 되기 전에 호출
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
