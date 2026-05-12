package com.mini3.backend.domain.ats.controller;

import com.mini3.backend.domain.ats.dto.AtsSubmitDto;
import com.mini3.backend.domain.ats.service.AtsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/public/applications")
@RequiredArgsConstructor
public class AtsPublicController {

    private final AtsService atsService;

    @PostMapping
    public ResponseEntity<AtsSubmitDto.SubmitResponse> submitApplication(
            @Valid @ModelAttribute AtsSubmitDto.SubmitRequest request,
            @ModelAttribute("file") MultipartFile file
    ) {
        AtsSubmitDto.SubmitResponse response = atsService.submitApplication(request, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
