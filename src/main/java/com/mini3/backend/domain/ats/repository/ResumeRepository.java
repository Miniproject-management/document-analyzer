package com.mini3.backend.domain.ats.repository;

import com.mini3.backend.domain.ats.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByApplicant_ApplicantIdOrderByCreatedAtDesc(Long applicantId);

    Optional<Resume> findFirstByApplicant_ApplicantIdOrderByCreatedAtDesc(Long applicantId);
}
