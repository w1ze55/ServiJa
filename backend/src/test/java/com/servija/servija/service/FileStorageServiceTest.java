package com.servija.servija.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private FileStorageService service;

    @Test
    @DisplayName("storeFile envia o arquivo ao S3 e retorna a URL pública")
    void storeFileUploadsFileToS3() {
        ReflectionTestUtils.setField(service, "bucketName", "servija-bucket");
        ReflectionTestUtils.setField(service, "region", "sa-east-1");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "img".getBytes());

        String url = service.storeFile(file);

        assertThat(url)
                .startsWith("https://servija-bucket.s3.sa-east-1.amazonaws.com/")
                .endsWith(".png");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("servija-bucket");
        assertThat(captor.getValue().contentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("storeFile aceita áudio suportado e preserva o content type")
    void storeFileUploadsSupportedAudio() {
        ReflectionTestUtils.setField(service, "bucketName", "servija-bucket");
        ReflectionTestUtils.setField(service, "region", "sa-east-1");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "gravacao.webm",
                "audio/webm",
                "audio".getBytes());

        String url = service.storeFile(file);

        assertThat(url)
                .startsWith("https://servija-bucket.s3.sa-east-1.amazonaws.com/")
                .endsWith(".webm");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().contentType()).isEqualTo("audio/webm");
    }

    @Test
    @DisplayName("storeFile rejeita arquivo vazio")
    void storeFileRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.storeFile(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Arquivo vazio");
    }

    @Test
    @DisplayName("storeFile rejeita tipo de arquivo não permitido")
    void storeFileRejectsUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.sh",
                "application/x-sh",
                "echo hi".getBytes());

        assertThatThrownBy(() -> service.storeFile(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tipo de arquivo");
    }

    @Test
    @DisplayName("deleteFile remove o objeto do S3 com a key extraída da URL")
    void deleteFileDeletesObjectUsingKeyFromUrl() {
        ReflectionTestUtils.setField(service, "bucketName", "servija-bucket");

        service.deleteFile("https://servija-bucket.s3.sa-east-1.amazonaws.com/file-123.png");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("servija-bucket");
        assertThat(captor.getValue().key()).isEqualTo("file-123.png");
    }
}
