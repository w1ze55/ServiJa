package com.servija.servija.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoServiceTest {

    @Test
    @DisplayName("encrypt e decrypt preservam o texto original")
    void encryptAndDecryptRoundTrip() {
        CryptoService service = configuredService();

        byte[] encrypted = service.encrypt("segredo");

        assertThat(encrypted).isNotNull().isNotEmpty();
        assertThat(service.decrypt(encrypted)).isEqualTo("segredo");
    }

    @Test
    @DisplayName("encrypt e decrypt retornam null para payload nulo")
    void encryptAndDecryptReturnNullForNullValues() {
        CryptoService service = configuredService();

        assertThat(service.encrypt(null)).isNull();
        assertThat(service.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("decrypt rejeita payload menor do que o IV")
    void decryptRejectsShortPayload() {
        CryptoService service = configuredService();

        assertThatThrownBy(() -> service.decrypt(new byte[12]))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("payload too short");
    }

    @Test
    @DisplayName("init rejeita chave que não decodifica 32 bytes")
    void initRejectsInvalidKeyLength() {
        CryptoService service = new CryptoService();
        ReflectionTestUtils.setField(service, "base64Key", Base64.getEncoder().encodeToString(new byte[16]));

        assertThatThrownBy(service::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    private CryptoService configuredService() {
        CryptoService service = new CryptoService();
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        ReflectionTestUtils.setField(service, "base64Key", Base64.getEncoder().encodeToString(key));
        service.init();
        return service;
    }
}
