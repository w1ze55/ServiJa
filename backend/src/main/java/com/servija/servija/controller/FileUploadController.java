package com.servija.servija.controller;

import com.servija.servija.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "Endpoints para upload de arquivos")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de uma imagem")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = fileStorageService.storeFile(file);
            Map<String, String> response = new HashMap<>();
            response.put("url", url);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de múltiplas imagens")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        try {
            List<String> urls = new ArrayList<>();
            for (MultipartFile file : files) {
                String url = fileStorageService.storeFile(file);
                urls.add(url);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("urls", urls);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/image")
    @Operation(summary = "Deletar uma imagem")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, String>> deleteImage(@RequestParam("url") String url) {
        try {
            fileStorageService.deleteFile(url);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Imagem deletada com sucesso");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
