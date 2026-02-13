package com.servija.servija.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://3.131.85.191}")
    private String baseUrl;

    private Path uploadPath;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            log.info("Diretório de upload criado: {}", uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar o diretório de upload", e);
        }
    }

    public String storeFile(MultipartFile file) {
        // Validar arquivo
        validateFile(file);

        // Gerar nome único
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + extension;

        try {
            // Verificar se o nome do arquivo contém caracteres inválidos
            if (newFilename.contains("..")) {
                throw new RuntimeException("Nome de arquivo inválido: " + newFilename);
            }

            // Copiar arquivo para o diretório de destino
            Path targetLocation = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Retornar URL pública
            String fileUrl = baseUrl + "/api/files/" + newFilename;
            log.info("Arquivo salvo: {} -> {}", originalFilename, fileUrl);
            return fileUrl;

        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo: " + originalFilename, e);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            // Extrair nome do arquivo da URL
            String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path filePath = uploadPath.resolve(filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Arquivo deletado: {}", filename);
            }
        } catch (IOException e) {
            log.error("Erro ao deletar arquivo: {}", fileUrl, e);
            throw new RuntimeException("Erro ao deletar arquivo", e);
        }
    }

    public Path getFilePath(String filename) {
        return uploadPath.resolve(filename).normalize();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Arquivo vazio");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("Arquivo muito grande. Máximo permitido: 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new RuntimeException("Tipo de arquivo não permitido. Permitidos: JPEG, PNG, GIF, WEBP");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
