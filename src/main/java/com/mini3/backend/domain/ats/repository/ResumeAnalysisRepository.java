package com.mini3.backend.domain.ats.repository;

import com.mini3.backend.domain.ats.entity.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {

    List<ResumeAnalysis> findByResume_ResumeIdOrderByAnalyzedAtDesc(Long resumeId);

    Optional<ResumeAnalysis> findFirstByResume_ResumeIdOrderByAnalyzedAtDesc(Long resumeId);
}
