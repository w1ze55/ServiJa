package com.servija.servija.service;

import com.servija.servija.model.User;
import com.servija.servija.model.UserTwoFactorBackupCode;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.repository.UserTwoFactorBackupCodeRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TotpService {

    private static final int BACKUP_CODE_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 10;
    private static final String BACKUP_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int SETUP_TTL_MINUTES = 10;

    private final UserRepository userRepository;
    private final UserTwoFactorBackupCodeRepository backupCodeRepository;
    private final CryptoService cryptoService;
    private final PasswordEncoder passwordEncoder;

    @Value("${totp.issuer:ServiJa}")
    private String issuer;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final SecureRandom random = new SecureRandom();

    public record SetupResult(String secret, String qrCodeDataUri) {}

    @Transactional
    public SetupResult beginSetup(User user) {
        String secret = secretGenerator.generate();
        user.setPendingTotpSecretEncrypted(cryptoService.encrypt(secret));
        user.setPendingTotpCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        String dataUri = buildQrCodeDataUri(user.getEmail(), secret);
        return new SetupResult(secret, dataUri);
    }

    private String buildQrCodeDataUri(String email, String secret) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        QrGenerator qrGenerator = new ZxingPngQrGenerator();
        try {
            byte[] image = qrGenerator.generate(data);
            return Utils.getDataUriForImage(image, qrGenerator.getImageMimeType());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    @Transactional
    public List<String> confirmSetup(User user, String code) {
        if (user.getPendingTotpSecretEncrypted() == null) {
            throw new IllegalStateException("No pending 2FA setup");
        }
        if (user.getPendingTotpCreatedAt() != null
                && user.getPendingTotpCreatedAt().isBefore(LocalDateTime.now().minusMinutes(SETUP_TTL_MINUTES))) {
            user.setPendingTotpSecretEncrypted(null);
            user.setPendingTotpCreatedAt(null);
            userRepository.save(user);
            throw new IllegalStateException("Pending 2FA setup expired");
        }

        String secret = cryptoService.decrypt(user.getPendingTotpSecretEncrypted());
        if (!verifyTotpCode(secret, code)) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        user.setTotpSecretEncrypted(user.getPendingTotpSecretEncrypted());
        user.setPendingTotpSecretEncrypted(null);
        user.setPendingTotpCreatedAt(null);
        user.setTotpEnabled(true);
        user.setTotpEnabledAt(LocalDateTime.now());
        user.setTotpFailedAttempts(0);
        user.setTotpLockedUntil(null);
        userRepository.save(user);

        return rotateBackupCodes(user);
    }

    @Transactional
    public List<String> rotateBackupCodes(User user) {
        backupCodeRepository.deleteByUserId(user.getId());
        List<String> plain = new ArrayList<>(BACKUP_CODE_COUNT);
        List<UserTwoFactorBackupCode> entities = new ArrayList<>(BACKUP_CODE_COUNT);
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            String code = generateBackupCode();
            plain.add(code);
            entities.add(UserTwoFactorBackupCode.builder()
                    .user(user)
                    .codeHash(passwordEncoder.encode(code))
                    .build());
        }
        backupCodeRepository.saveAll(entities);
        return plain;
    }

    private String generateBackupCode() {
        StringBuilder sb = new StringBuilder(BACKUP_CODE_LENGTH + 1);
        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            if (i == 5) sb.append('-');
            sb.append(BACKUP_ALPHABET.charAt(random.nextInt(BACKUP_ALPHABET.length())));
        }
        return sb.toString();
    }

    @Transactional
    public void disable(User user) {
        user.setTotpEnabled(false);
        user.setTotpSecretEncrypted(null);
        user.setPendingTotpSecretEncrypted(null);
        user.setPendingTotpCreatedAt(null);
        user.setTotpEnabledAt(null);
        user.setTotpFailedAttempts(0);
        user.setTotpLockedUntil(null);
        backupCodeRepository.deleteByUserId(user.getId());
        userRepository.save(user);
    }

    public boolean isLocked(User user) {
        return user.getTotpLockedUntil() != null
                && user.getTotpLockedUntil().isAfter(LocalDateTime.now());
    }

    /**
     * Verifies a 2FA challenge using either the TOTP code or an unused backup code.
     * Increments failure counter; locks the account's 2FA for LOCK_MINUTES after MAX_FAILED_ATTEMPTS.
     * Returns true on success.
     */
    @Transactional
    public VerifyResult verifyChallenge(User user, String input) {
        if (isLocked(user)) {
            return VerifyResult.LOCKED;
        }
        if (input == null) {
            return registerFailure(user);
        }
        String trimmed = input.trim();

        // Try TOTP first (6 digits, all numeric)
        if (trimmed.length() == 6 && trimmed.chars().allMatch(Character::isDigit)
                && user.getTotpSecretEncrypted() != null) {
            String secret = cryptoService.decrypt(user.getTotpSecretEncrypted());
            if (verifyTotpCode(secret, trimmed)) {
                resetFailures(user);
                return VerifyResult.OK_TOTP;
            }
        }

        // Try backup code
        String normalized = trimmed.toUpperCase().replace("-", "");
        if (normalized.length() == BACKUP_CODE_LENGTH) {
            String formatted = normalized.substring(0, 5) + "-" + normalized.substring(5);
            for (UserTwoFactorBackupCode bc : backupCodeRepository.findByUserAndUsedAtIsNull(user)) {
                if (passwordEncoder.matches(formatted, bc.getCodeHash())) {
                    bc.setUsedAt(LocalDateTime.now());
                    backupCodeRepository.save(bc);
                    resetFailures(user);
                    return VerifyResult.OK_BACKUP;
                }
            }
        }

        return registerFailure(user);
    }

    private VerifyResult registerFailure(User user) {
        int attempts = Optional.ofNullable(user.getTotpFailedAttempts()).orElse(0) + 1;
        user.setTotpFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setTotpLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            user.setTotpFailedAttempts(0);
            userRepository.save(user);
            return VerifyResult.LOCKED;
        }
        userRepository.save(user);
        return VerifyResult.INVALID;
    }

    private void resetFailures(User user) {
        user.setTotpFailedAttempts(0);
        user.setTotpLockedUntil(null);
        userRepository.save(user);
    }

    public boolean verifyTotpCode(String secret, String code) {
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        verifier.setTimePeriod(30);
        verifier.setAllowedTimePeriodDiscrepancy(1);
        return verifier.isValidCode(secret, code);
    }

    public enum VerifyResult {
        OK_TOTP,
        OK_BACKUP,
        INVALID,
        LOCKED
    }
}
