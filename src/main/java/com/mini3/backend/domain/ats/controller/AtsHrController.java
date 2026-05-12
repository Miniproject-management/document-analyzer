package com.mini3.backend.domain.ats.controller;

import com.mini3.backend.domain.ats.dto.AtsAnalysisDto;
import com.mini3.backend.domain.ats.dto.AtsApplicantDto;
import com.mini3.backend.domain.ats.dto.AtsHrDashboardDto;
import com.mini3.backend.domain.ats.service.AtsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hr/applicants")
@RequiredArgsConstructor
public class AtsHrController {

    private final AtsService atsService;

    @GetMapping // 지원자 목록 조회
    public ResponseEntity<List<AtsApplicantDto.ListItem>> getApplicants() {
        return ResponseEntity.ok(atsService.getApplicants());
    }

    /** HR 대시보드: 지원자 | 최신 이력서·분석 요약 (경로 변수보다 먼저 매핑해야 함) */
    @GetMapping("/dashboard")
    public ResponseEntity<List<AtsHrDashboardDto.ApplicantRow>> getDashboardRows() {
        return ResponseEntity.ok(atsService.getHrDashboardApplicantRows());
    }

    @GetMapping("/{applicantId}") // 한 명 클릭 시 상세 정보
    public ResponseEntity<AtsApplicantDto.Detail> getApplicantDetail(@PathVariable Long applicantId) {
        return ResponseEntity.ok(atsService.getApplicantDetail(applicantId));
    }

    @PostMapping("/{applicantId}/analyze") // HR 이 분석하기 눌렀을 때 호출하는 API
    public ResponseEntity<AtsAnalysisDto.AnalyzeResult> analyzeApplicant(@PathVariable Long applicantId) {
        return ResponseEntity.ok(atsService.analyzeApplicantResume(applicantId));
    }
}
