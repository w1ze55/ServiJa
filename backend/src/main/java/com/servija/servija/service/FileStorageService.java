package com.servija.servija.service;

import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.UploadedFile;
import com.servija.servija.repository.UploadedFileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
            "image/jfif",
            "image/png",
            "image/gif",
            "image/webp",
            "audio/mpeg",
            "audio/mp3",
            "audio/mp4",
            "audio/x-m4a",
            "audio/wav",
            "audio/x-wav",
            "audio/ogg",
            "audio/webm",
            "audio/aac",
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
            ".jfif",
            ".png",
            ".gif",
            ".webp",
            ".mp3",
            ".m4a",
            ".wav",
            ".ogg",
            ".webm",
            ".aac",
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
    private final UploadedFileRepository uploadedFileRepository;

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

        // H4: nunca confiar no content-type enviado pelo cliente. Deriva o content-type
        // da extensão (allow-list) e força download para tipos não-imagem, eliminando o
        // vetor de XSS armazenado (HTML/SVG servidos como executáveis).
        String safeContentType = resolveSafeContentType(extension);
        String disposition = isImageExtension(extension) ? "inline" : "attachment";

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(newFilename)
                    .contentType(safeContentType)
                    .contentDisposition(disposition)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // H3: registra o dono do objeto para autorizar exclusões futuras.
            recordOwnership(newFilename);

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

    /**
     * H3: exclui um arquivo verificando a propriedade. Apenas o usuário que enviou o
     * objeto (dono registrado) pode excluí-lo. Use este método em endpoints expostos
     * ao cliente; chamadas internas já autorizadas podem usar {@link #deleteFile}.
     */
    @Transactional
    public void deleteOwnedFile(String fileUrl, String requesterEmail) {
        String key = extractKey(fileUrl);
        UploadedFile record = uploadedFileRepository.findByS3Key(key)
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo não encontrado"));

        if (requesterEmail == null || !requesterEmail.equals(record.getOwnerEmail())) {
            // Não revela existência para não-donos.
            throw new ResourceNotFoundException("Arquivo não encontrado");
        }

        deleteFile(fileUrl);
        uploadedFileRepository.delete(record);
    }

    private void recordOwnership(String s3Key) {
        try {
            String email = currentUserEmail();
            if (email == null) {
                return; // upload sem contexto autenticado: sem registro de dono
            }
            if (uploadedFileRepository.findByS3Key(s3Key).isEmpty()) {
                uploadedFileRepository.save(UploadedFile.builder()
                        .s3Key(s3Key)
                        .ownerEmail(email)
                        .build());
            }
        } catch (Exception ex) {
            // Best-effort: não falhar o upload por causa do registro de propriedade.
            log.warn("Não foi possível registrar a propriedade do arquivo {}: {}", s3Key, ex.getMessage());
        }
    }

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String name = auth.getName();
        return (name != null && !name.isBlank() && !"anonymousUser".equals(name)) ? name : null;
    }

    private String extractKey(String fileUrl) {
        if (fileUrl == null) {
            return "";
        }
        return fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Arquivo vazio");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("Arquivo muito grande. Máximo permitido: 10MB");
        }

        // H4: a extensão (allow-list) é autoritativa. O content-type do cliente NÃO é
        // usado para autorizar nem para servir o arquivo — é derivado da extensão no
        // servidor (resolveSafeContentType), evitando XSS armazenado via content-type
        // malicioso (ex.: text/html, image/svg+xml).
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException(
                    "Tipo de arquivo não permitido. Permitidos: JPEG, JFIF, PNG, GIF, WEBP, MP3, M4A, WAV, OGG, WEBM, AAC, PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX e TXT");
        }
    }

    private static final java.util.Map<String, String> EXTENSION_CONTENT_TYPES = java.util.Map.ofEntries(
            java.util.Map.entry(".jpg", "image/jpeg"),
            java.util.Map.entry(".jpeg", "image/jpeg"),
            java.util.Map.entry(".jfif", "image/jpeg"),
            java.util.Map.entry(".png", "image/png"),
            java.util.Map.entry(".gif", "image/gif"),
            java.util.Map.entry(".webp", "image/webp"),
            java.util.Map.entry(".mp3", "audio/mpeg"),
            java.util.Map.entry(".m4a", "audio/mp4"),
            java.util.Map.entry(".wav", "audio/wav"),
            java.util.Map.entry(".ogg", "audio/ogg"),
            java.util.Map.entry(".webm", "audio/webm"),
            java.util.Map.entry(".aac", "audio/aac"),
            java.util.Map.entry(".pdf", "application/pdf"),
            java.util.Map.entry(".doc", "application/msword"),
            java.util.Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            java.util.Map.entry(".xls", "application/vnd.ms-excel"),
            java.util.Map.entry(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            java.util.Map.entry(".ppt", "application/vnd.ms-powerpoint"),
            java.util.Map.entry(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            java.util.Map.entry(".txt", "text/plain"));

    private static final java.util.List<String> IMAGE_EXTENSIONS =
            java.util.Arrays.asList(".jpg", ".jpeg", ".jfif", ".png", ".gif", ".webp");

    private String resolveSafeContentType(String extension) {
        return EXTENSION_CONTENT_TYPES.getOrDefault(
                extension != null ? extension.toLowerCase() : "", "application/octet-stream");
    }

    private boolean isImageExtension(String extension) {
        return extension != null && IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
