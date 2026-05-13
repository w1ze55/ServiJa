package com.servija.servija.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain");

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg",
            ".jpeg",
            ".png",
            ".gif",
            ".webp",
            ".pdf",
            ".doc",
            ".docx",
            ".xls",
            ".xlsx",
            ".ppt",
            ".pptx",
            ".txt");

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @PostConstruct
    public void init() {
        if (bucketName != null) {
            bucketName = bucketName.trim();
        }
        if (region != null) {
            region = region.trim();
        }
        log.info("FileStorageService inicializado com balde: {} na regiao: {}", bucketName, region);
    }

    public String storeFile(MultipartFile file) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(newFilename)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, newFilename);
            log.info("Arquivo salvo no S3: {} -> {}", originalFilename, fileUrl);
            return fileUrl;
        } catch (IOException ex) {
            log.error("Erro ao salvar arquivo no S3: {}", originalFilename, ex);
            throw new RuntimeException("Erro ao salvar arquivo no S3: " + originalFilename, ex);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String key = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Arquivo deletado do S3: {}", key);
        } catch (Exception ex) {
            log.error("Erro ao deletar arquivo do S3: {}", fileUrl, ex);
            throw new RuntimeException("Erro ao deletar arquivo do S3", ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Arquivo vazio");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("Arquivo muito grande. Maximo permitido: 10MB");
        }

        String contentType = file.getContentType();
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        boolean isAllowedByContentType = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);
        boolean isAllowedByExtension = ALLOWED_EXTENSIONS.contains(extension);

        if (!isAllowedByContentType && !isAllowedByExtension) {
            throw new RuntimeException(
                    "Tipo de arquivo nao permitido. Permitidos: JPEG, PNG, GIF, WEBP, PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX e TXT");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
