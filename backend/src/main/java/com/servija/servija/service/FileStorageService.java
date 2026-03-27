package com.servija.servija.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
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

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @PostConstruct
    public void init() {
        if (bucketName != null)
            bucketName = bucketName.trim();
        if (region != null)
            region = region.trim();
        log.info("FileStorageService inicializado com balde: {} na região: {}", bucketName, region);
    }

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf");

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public String storeFile(MultipartFile file) {
        // Validar arquivo
        validateFile(file);

        // Gerar nome único
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + extension;

        try {
            // Upload para S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(newFilename)
                    .contentType(file.getContentType())
                    // Removido ACL pois o bucket já tem política pública
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // Retornar URL pública (formato padrão do S3)
            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, newFilename);
            log.info("Arquivo salvo no S3: {} -> {}", originalFilename, fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Erro ao salvar arquivo no S3: {}", originalFilename, e);
            throw new RuntimeException("Erro ao salvar arquivo no S3: " + originalFilename, e);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            // Extrair a chave (nome do arquivo) da URL
            // Ex: https://servija-s3.s3.sa-east-1.amazonaws.com/uuid.png -> uuid.png
            String key = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Arquivo deletado do S3: {}", key);
        } catch (Exception e) {
            log.error("Erro ao deletar arquivo do S3: {}", fileUrl, e);
            throw new RuntimeException("Erro ao deletar arquivo do S3", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Arquivo vazio");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("Arquivo muito grande. Máximo permitido: 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new RuntimeException("Tipo de arquivo não permitido. Permitidos: JPEG, PNG, GIF, WEBP, PDF");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
