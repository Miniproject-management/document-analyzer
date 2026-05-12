package com.mini3.backend.domain.ats.repository;

import com.mini3.backend.domain.ats.entity.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {

    List<Applicant> findAllByOrderByCreatedAtDesc();
}
