package com.mini3.backend.domain.ats.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini3.backend.domain.ats.entity.Applicant;
import com.mini3.backend.domain.ats.entity.Resume;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini (Generative Language API)로 이력서 텍스트를 분석한다.
 * API 키는 {@code spring.ai.google.genai.api-key} / {@code GOOGLE_API_KEY}를 사용한다.
 */
@Slf4j
@Component
public class AtsGeminiResumeAnalysisClient {

    private static final int MAX_RESUME_CHARS = 120_000;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.google.genai.api-key:}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-2.5-flash}")
    private String modelId;

    public AtsGeminiResumeAnalysisClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    /**
     * @param jobCriteriaOrNull HR 직무·평가 기준 (비어 있으면 일반 채용 관점)
     * @return 모델이 반환한 JSON 객체 (overallScore, summary, strengths, risks, decision 등)
     */
    public JsonNode analyze(
            Applicant applicant,
            Resume resume,
            String resumePlainText,
            String jobCriteriaOrNull
    ) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GOOGLE_API_KEY가 비어 있습니다. GitHub Secrets / 배포 환경에 키를 설정하세요.");
        }

        String clipped = clipText(resumePlainText);
        String prompt = buildPrompt(applicant, resume, clipped, jobCriteriaOrNull);

        URI uri = UriComponentsBuilder.fromUriString("https://generativelanguage.googleapis.com")
                .path("/v1beta/models/{model}:generateContent")
                .queryParam("key", apiKey)
                .buildAndExpand(modelId)
                .toUri();

        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.25,
                "responseMimeType", "application/json"
        );
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", generationConfig
        );

        String raw = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus((s) -> s.is4xxClientError() || s.is5xxServerError(), (req, res) -> {
                    String err = new String(res.getBody().readAllBytes());
                    log.warn("Gemini API 오류 status={} body={}", res.getStatusCode(), err);
                    throw new IllegalStateException("Gemini API 호출 실패: HTTP " + res.getStatusCode());
                })
                .body(String.class);

        JsonNode root = objectMapper.readTree(raw);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini 응답에 candidates가 없습니다.");
        }
        JsonNode first = candidates.get(0);
        String finish = first.path("finishReason").asText("");
        if ("SAFETY".equals(finish) || "RECITATION".equals(finish)) {
            throw new IllegalStateException("Gemini가 콘텐츠를 생성하지 않았습니다: " + finish);
        }
        JsonNode parts = first.path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new IllegalStateException("Gemini 응답 parts가 비어 있습니다.");
        }
        String text = parts.get(0).path("text").asText();
        if (text.isBlank()) {
            throw new IllegalStateException("Gemini 응답 텍스트가 비어 있습니다.");
        }
        return objectMapper.readTree(text);
    }

    private static String clipText(String resumePlainText) {
        if (resumePlainText == null) {
            return "";
        }
        if (resumePlainText.length() <= MAX_RESUME_CHARS) {
            return resumePlainText;
        }
        return resumePlainText.substring(0, MAX_RESUME_CHARS)
                + "\n\n[이후 텍스트는 길이 제한으로 잘렸습니다.]";
    }

    private static String buildPrompt(
            Applicant applicant,
            Resume resume,
            String resumePlainText,
            String jobCriteriaOrNull
    ) {
        String jobBlock = buildJobCriteriaBlock(jobCriteriaOrNull);
        String resumeBody = resumePlainText.isBlank() ? "(본문 없음)" : resumePlainText;

        return """
                당신은 채용 담당자를 돕는 이력서 검토 어시스턴트입니다.
                아래 "시스템에 등록된 지원자 정보"와 "이력서 PDF에서 추출한 텍스트"는 동일 지원 건으로 이미 서버에서 매칭된 데이터입니다.
                지원자 프로필과 이력서 내용의 일관성(이름·경력·기술스택 등)을 참고해 평가하세요.

                [시스템에 등록된 지원자 정보]
                - applicantId: %d
                - resumeId: %d
                - 이름: %s
                - 이메일: %s
                - 전화: %s
                - 원본 파일명: %s
                - S3 객체 키(참고용): %s

                %s

                [이력서 본문 텍스트]
                %s

                ---
                반드시 유효한 JSON만 한 덩어리로 출력하세요. 다른 설명 문장은 쓰지 마세요.
                overallScore·decision·summary·strengths·risks에는 HR이 제시한 직무 기준이 있다면 그 기준과의 적합도를 반드시 반영하세요.
                키와 형식:
                {
                  "overallScore": <0~100 정수>,
                  "summary": "<한국어로 3~6문장 요약>",
                  "strengths": ["<강점1>", "..."],
                  "risks": ["<우려1>", "..."],
                  "decision": "<STRONG_FIT | REVIEW | WEAK_FIT 중 하나>",
                  "matchedApplicantProfile": <true 또는 false, 이력서 내용이 등록 지원자와 명백히 불일치하면 false>
                }
                """.formatted(
                applicant.getApplicantId(),
                resume.getResumeId(),
                nullToDash(applicant.getName()),
                nullToDash(applicant.getEmail()),
                nullToDash(applicant.getPhone()),
                nullToDash(resume.getOriginalFileName()),
                nullToDash(resume.getS3ObjectKey()),
                jobBlock,
                resumeBody
        );
    }

    private static String buildJobCriteriaBlock(String jobCriteriaOrNull) {
        if (jobCriteriaOrNull != null && !jobCriteriaOrNull.isBlank()) {
            return """
                    [HR이 이번 분석을 위해 입력한 평가 기준·채용 직무]
                    """ + jobCriteriaOrNull.strip() + """

                    위 직무·요구사항에 대한 적합도(역할·기술스택·경력의 부합 여부, 전환 가능성)를 overallScore, summary, strengths, risks, decision에 분명히 녹이세요.
                    """;
        }
        return """
                [평가 직무 기준]
                (별도 입력 없음) 일반적인 채용 검토 관점에서 이력서의 완성도·경력·리스크를 평가하세요.
                """;
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
