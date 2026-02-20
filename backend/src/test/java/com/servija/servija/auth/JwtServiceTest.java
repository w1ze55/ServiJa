package com.servija.servija.auth;

import com.servija.servija.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();

        // Injetar valores via reflection (simulando @Value)
        setField(jwtService, "secretKey", "dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9ydGVzdGluZ3B1cnBvc2VzMTIzNDU2Nzg5MA==");
        setField(jwtService, "jwtExpiration", 86400000L); // 24 horas

        userDetails = User.builder()
                .username("joao@email.com")
                .password("encoded-password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("Deve gerar token válido com subject correto")
    void generateToken_returnsValidToken() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("Deve extrair username do token corretamente")
    void extractUsername_returnsCorrectUsername() {
        String token = jwtService.generateToken(userDetails);

        String extractedUsername = jwtService.extractUsername(token);

        assertThat(extractedUsername).isEqualTo("joao@email.com");
    }

    @Test
    @DisplayName("Deve validar token válido com UserDetails correto")
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateToken(userDetails);

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Deve invalidar token com username diferente")
    void isTokenValid_differentUser_returnsFalse() {
        String token = jwtService.generateToken(userDetails);

        UserDetails otherUser = User.builder()
                .username("outro@email.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        boolean isValid = jwtService.isTokenValid(token, otherUser);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve invalidar token expirado")
    void isTokenValid_expiredToken_returnsFalse() throws Exception {
        // Criar JwtService com expiração de 1ms
        JwtService shortLivedService = new JwtService();
        setField(shortLivedService, "secretKey",
                "dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9ydGVzdGluZ3B1cnBvc2VzMTIzNDU2Nzg5MA==");
        setField(shortLivedService, "jwtExpiration", 1L); // 1ms

        String token = shortLivedService.generateToken(userDetails);

        // Aguardar expiração
        Thread.sleep(50);

        boolean isValid;
        try {
            isValid = shortLivedService.isTokenValid(token, userDetails);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            isValid = false;
        }

        assertThat(isValid).isFalse();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
