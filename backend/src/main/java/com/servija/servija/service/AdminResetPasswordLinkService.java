package com.servija.servija.service;

import com.servija.servija.dto.AdminResetPasswordLinkValidationResponse;
import com.servija.servija.model.User;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AdminResetPasswordLinkService {

    private static final int ADMIN_RESET_LINK_EXPIRY_MINUTES = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;

    @Value("${servija.frontend.public-url}")
    private String publicFrontendUrl;

    @Transactional
    public void sendResetLink(User user) {
        String rawToken = generateToken();

        user.setAdminResetPasswordTokenHash(hashToken(rawToken));
        user.setAdminResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(ADMIN_RESET_LINK_EXPIRY_MINUTES));
        userRepository.save(user);

        emailService.sendAdminPasswordResetLinkEmail(
                user.getEmail(),
                user.getName(),
                buildResetLink(rawToken));
    }

    @Transactional(readOnly = true)
    public AdminResetPasswordLinkValidationResponse validateToken(String token) {
        User user = findUserByToken(token);
        ensureLinkIsUsable(user);

        return AdminResetPasswordLinkValidationResponse.builder()
                .emailHint(maskEmail(user.getEmail()))
                .userType(user.getUserType())
                .build();
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        User user = findUserByToken(token);
        ensureLinkIsUsable(user);
        passwordPolicyService.validateNewPassword(newPassword, confirmPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        markLinkAsUsed(user);
        clearCodeResetChallenge(user);
        userRepository.save(user);
    }

    public void clearAdminResetLink(User user) {
        user.setAdminResetPasswordTokenHash(null);
        user.setAdminResetPasswordTokenExpiry(null);
    }

    private User findUserByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Link de redefinição inválido.");
        }

        return userRepository.findByAdminResetPasswordTokenHash(hashToken(token))
                .orElseThrow(() -> new IllegalArgumentException("Link de redefinição inválido."));
    }

    private void ensureLinkIsUsable(User user) {
        if (user.getAdminResetPasswordTokenHash() == null) {
            throw new IllegalArgumentException("Link de redefinição inválido.");
        }

        if (user.getAdminResetPasswordTokenExpiry() == null) {
            throw new IllegalArgumentException("Este link de redefinição já foi utilizado.");
        }

        if (user.getAdminResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Este link de redefinição expirou.");
        }
    }

    private void markLinkAsUsed(User user) {
        user.setAdminResetPasswordTokenExpiry(null);
    }

    private void clearCodeResetChallenge(User user) {
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        user.setResetPasswordAttempts(0);
        user.setResetPasswordLockUntil(null);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo de hash indisponível", ex);
        }
    }

    private String buildResetLink(String token) {
        String baseUrl = publicFrontendUrl == null || publicFrontendUrl.isBlank()
                ? "http://localhost:5173"
                : publicFrontendUrl.trim().replaceAll("/+$", "");

        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/login")
                .queryParam("resetToken", token)
                .build(true)
                .toUriString();
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }

        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        int visibleChars = Math.min(3, username.length());
        return username.substring(0, visibleChars) + "***" + domain;
    }
}
