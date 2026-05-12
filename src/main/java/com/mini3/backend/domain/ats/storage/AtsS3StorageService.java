package com.mini3.backend.domain.ats.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * ATS 이력서 파일의 S3 업로드·조회. PDF 로드 후 분석까지의 흐름은 ats 서비스 계층에서 이 클래스를 사용한다.
 */
@Service
@RequiredArgsConstructor
public class AtsS3StorageService {

    private static final Pattern SAFE_NAME = Pattern.compile("[^a-zA-Z0-9._-]");

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 지원 이력서 원본 파일을 S3에 업로드하고 객체 키를 반환한다.
     */
    public String uploadApplicantResume(MultipartFile file, Long applicantId) throws IOException {
        String original = file.getOriginalFilename();
        String safe = sanitizeFileName(original != null ? original : "resume");
        String key = String.format("ats/resumes/%d/%s_%s", applicantId, UUID.randomUUID(), safe);

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        long size = file.getSize();
        if (size >= 0) {
            try (var input = file.getInputStream()) {
                s3Client.putObject(request, RequestBody.fromInputStream(input, size));
            }
        } else {
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        }

        return key;
    }

    /**
     * S3 객체 전체 바이트를 읽는다. PDF 등 이력서 분석 파이프라인에서 사용한다.
     */
    public byte[] getObjectBytes(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        try (var response = s3Client.getObject(request)) {
            return response.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("S3 객체를 읽지 못했습니다.", e);
        }
    }

    private static String sanitizeFileName(String name) {
        String base = name.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = SAFE_NAME.matcher(base).replaceAll("_");
        if (base.isBlank()) {
            base = "file";
        }
        if (base.length() > 200) {
            base = base.substring(0, 200);
        }
        return base.toLowerCase(Locale.ROOT);
    }
}
