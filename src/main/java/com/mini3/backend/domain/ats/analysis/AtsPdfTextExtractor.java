package com.mini3.backend.domain.ats.analysis;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * S3에서 받은 바이트가 PDF일 때 본문 텍스트를 추출한다.
 * {@code com.mini3.backend.domain.ats.analysis} 패키지는 Java 백엔드 내 이력서 문서 분석 파이프라인을 묶으며,
 * {@code domain/ats} 디렉터리의 Dockerfile(ECR document-analyzer)과 배포 경계를 맞추기 위한 위치이다.
 */
@Component
public class AtsPdfTextExtractor {

    public boolean isLikelyPdf(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        return bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
    }

    public String extractText(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }
}
