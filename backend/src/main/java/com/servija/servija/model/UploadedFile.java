package com.servija.servija.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * H3: registro de propriedade de arquivos no S3. Permite verificar, na exclusão,
 * se quem solicita é o dono do objeto (evita deleção arbitrária via URL).
 */
@Entity
@Table(name = "uploaded_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "s3_key", nullable = false, unique = true, length = 512)
    private String s3Key;

    @Column(name = "owner_email", nullable = false, length = 256)
    private String ownerEmail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
