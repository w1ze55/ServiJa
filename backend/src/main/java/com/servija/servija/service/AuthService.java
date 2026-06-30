package com.servija.servija.service;

import com.servija.servija.dto.AuthResponse;
import com.servija.servija.dto.AddressRequest;
import com.servija.servija.dto.LoginRequest;
import com.servija.servija.dto.RegistrationValidationRequest;
import com.servija.servija.dto.RegisterRequest;
import com.servija.servija.dto.UserRoleRequest;
import com.servija.servija.exception.DuplicateResourceException;
import com.servija.servija.exception.EmailVerificationRequiredException;
import com.servija.servija.exception.InvalidCredentialsException;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.exception.TooManyAttemptsException;
import com.servija.servija.model.Contractor;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.User;
import com.servija.servija.repository.ContractorRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;
    private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 10;
    private static final int RESET_PASSWORD_CODE_EXPIRY_MINUTES = 30;

    private static final long TOTP_CHALLENGE_TTL_MS = 5 * 60 * 1000L;

    private final UserRepository userRepository;
    private final ContractorRepository contractorRepository;
    private final FreelancerRepository freelancerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final EmailService emailService;
    private final AuditLogger auditLogger;
    private final AddressService addressService;
    private final PasswordPolicyService passwordPolicyService;
    private final AdminResetPasswordLinkService adminResetPasswordLinkService;
    private final TotpService totpService;
    private final UserRoleCompatibilityService userRoleCompatibilityService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateRegistrationData(request);

        User user = createBaseUser(request);

        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES));
        user = userRepository.save(user);
        ensureProfileForRole(user, request.getUserType());

        addressService.createAddressForUser(user, request.getAddress());

        emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationCode);

        auditLogger.recordFor(
                user,
                "USER_REGISTERED",
                "USER",
                "#USR-" + user.getId(),
                user.getName(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                Map.of("userType", user.getUserType()));

        return buildAuthResponse(user, null);
    }

    public void validateRegistrationData(RegisterRequest request) {
        validateRegistrationData(request.getUserType(), request.getEmail(), request.getCpf());
    }

    public void validateRegistrationData(RegistrationValidationRequest request) {
        validateRegistrationData(request.getUserType(), request.getEmail(), request.getCpf());
    }

    private void validateRegistrationData(User.UserType userType, String email, String cpf) {
        if (userType == User.UserType.ADMIN) {
            throw new IllegalArgumentException("Cadastro público de admin não é permitido");
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("E-mail ja cadastrado");
        }

        if (userRepository.existsByCpf(cpf)) {
            throw new DuplicateResourceException("CPF ja cadastrado");
        }
    }

    public void validateAddressData(AddressRequest request) {
        addressService.validateAddressData(request);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
            user = userRoleCompatibilityService.repairIfNeeded(user);

            if (user.getUserType() == User.UserType.ADMIN) {
                throw new InvalidCredentialsException("Credenciais inválidas");
            }

            if (!user.getActive()) {
                throw new InvalidCredentialsException("Conta suspensa");
            }

            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                throw new EmailVerificationRequiredException(user.getEmail());
            }

            if (Boolean.TRUE.equals(user.getTotpEnabled())) {
                return buildChallengeResponse(user);
            }

            String token = generateAccessToken(user);

            auditLogger.recordFor(
                    user,
                    "LOGIN",
                    "AUTH",
                    null,
                    user.getEmail(),
                    com.servija.servija.model.AuditLog.Result.SUCCESS,
                    Map.of("method", "password"));

            return buildAuthResponse(user, token);
        } catch (Exception e) {
            User attempted = userRepository.findByEmail(request.getEmail()).orElse(null);
            if (attempted != null && attempted.getUserType() != User.UserType.ADMIN) {
                auditLogger.recordFor(
                        attempted,
                        "LOGIN_FAILED",
                        "AUTH",
                        null,
                        attempted.getEmail(),
                        com.servija.servija.model.AuditLog.Result.FAILURE,
                        Map.of("reason", resolveLoginFailureReason(e, false)));
            } else if (attempted == null) {
                auditLogger.recordGuest(
                        request.getEmail(),
                        "LOGIN_FAILED",
                        "AUTH",
                        null,
                        request.getEmail(),
                        com.servija.servija.model.AuditLog.Result.FAILURE,
                        Map.of("reason", resolveLoginFailureReason(e, true)));
            }
            throw e;
        }
    }

    public AuthResponse loginAdmin(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        user = userRoleCompatibilityService.repairIfNeeded(user);

        if (user.getUserType() != User.UserType.ADMIN) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        }

        if (!user.getActive()) {
            throw new InvalidCredentialsException("Conta suspensa");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailVerificationRequiredException(user.getEmail());
        }

        if (Boolean.TRUE.equals(user.getTotpEnabled())) {
            return buildChallengeResponse(user);
        }

        String token = generateAccessToken(user);

        return buildAuthResponse(user, token);
    }

    /**
     * Completes a 2FA login by verifying the TOTP/backup code presented with a valid challenge token.
     */
    @Transactional
    public AuthResponse verifyTwoFactorChallenge(String challengeToken, String code) {
        String email;
        try {
            email = jwtService.extractUsername(challengeToken);
        } catch (Exception e) {
            throw new InvalidCredentialsException("Token de desafio inválido ou expirado");
        }

        Boolean pending = safeBoolClaim(challengeToken, "totp_pending");
        if (!Boolean.TRUE.equals(pending)) {
            throw new InvalidCredentialsException("Token de desafio inválido");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (!user.getActive() || !Boolean.TRUE.equals(user.getTotpEnabled())) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        }

        // Validate the challenge token itself against the user (signature + non-expired).
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        if (!jwtService.isTokenValid(challengeToken, userDetails)) {
            throw new InvalidCredentialsException("Token de desafio inválido ou expirado");
        }

        TotpService.VerifyResult result = totpService.verifyChallenge(user, code);
        if (result == TotpService.VerifyResult.LOCKED) {
            auditLogger.recordFor(
                    user,
                    "2FA_FAILED",
                    "AUTH",
                    null,
                    user.getEmail(),
                    com.servija.servija.model.AuditLog.Result.FAILURE,
                    Map.of("reason", "locked"));
            throw new TooManyAttemptsException(
                    "Muitas tentativas. Aguarde para tentar novamente.",
                    0,
                    user.getTotpLockedUntil());
        }
        if (result == TotpService.VerifyResult.INVALID) {
            auditLogger.recordFor(
                    user,
                    "2FA_FAILED",
                    "AUTH",
                    null,
                    user.getEmail(),
                    com.servija.servija.model.AuditLog.Result.FAILURE,
                    Map.of("reason", "invalid_code"));
            throw new InvalidCredentialsException("Código inválido");
        }

        String token = generateAccessToken(user);

        String method = result == TotpService.VerifyResult.OK_BACKUP ? "password+backup" : "password+totp";
        auditLogger.recordFor(
                user,
                "LOGIN",
                "AUTH",
                null,
                user.getEmail(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                Map.of("method", method));

        if (result == TotpService.VerifyResult.OK_BACKUP) {
            auditLogger.recordFor(
                    user,
                    "2FA_BACKUP_USED",
                    "AUTH",
                    null,
                    user.getEmail(),
                    com.servija.servija.model.AuditLog.Result.SUCCESS,
                    Map.of());
        }

        return buildAuthResponse(user, token);
    }

    private Boolean safeBoolClaim(String token, String name) {
        try {
            return jwtService.extractClaim(token, c -> c.get(name, Boolean.class));
        } catch (Exception e) {
            return null;
        }
    }

    private AuthResponse buildChallengeResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        Map<String, Object> claims = new java.util.HashMap<>(buildTokenClaims(user));
        claims.put("totp_pending", true);
        String challenge = jwtService.generateToken(userDetails, claims, TOTP_CHALLENGE_TTL_MS);
        return AuthResponse.builder()
                .token(challenge)
                .tokenType("Bearer")
                .requires2fa(true)
                .build();
    }

    @Transactional(noRollbackFor = { IllegalArgumentException.class, TooManyAttemptsException.class })
    public AuthResponse verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        int attemptsUsed = safeAttempts(user.getVerificationAttempts()) + 1;

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("E-mail já verificado");
        }

        if (user.getVerificationLockUntil() != null && user.getVerificationLockUntil().isAfter(LocalDateTime.now())) {
            throw new TooManyAttemptsException(
                    "Muitas tentativas. Aguarde para tentar novamente.",
                    0,
                    user.getVerificationLockUntil());
        }

        if (user.getVerificationLockUntil() != null && user.getVerificationLockUntil().isBefore(LocalDateTime.now())) {
            user.setVerificationAttempts(0);
            user.setVerificationLockUntil(null);
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            int attempts = safeAttempts(user.getVerificationAttempts()) + 1;
            user.setVerificationAttempts(attempts);

            if (attempts >= MAX_VERIFICATION_ATTEMPTS) {
                user.setVerificationLockUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                userRepository.save(user);
                throw new TooManyAttemptsException(
                        "Muitas tentativas. Aguarde para tentar novamente.",
                        0,
                        user.getVerificationLockUntil());
            }

            userRepository.save(user);
            int remaining = MAX_VERIFICATION_ATTEMPTS - attempts;
            throw new IllegalArgumentException(
                    remaining + " tentativa" + (remaining != 1 ? "s" : "") + " restante" + (remaining != 1 ? "s" : ""));
        }

        if (user.getVerificationCodeExpiry() == null || user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código de verificação expirado, solicite um novo");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        user.setVerificationAttempts(0);
        user.setVerificationLockUntil(null);
        userRepository.save(user);

        String token = generateAccessToken(user);

        auditLogger.recordFor(
                user,
                "EMAIL_VERIFIED",
                "AUTH",
                null,
                user.getEmail(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                Map.of("attempts", attemptsUsed));

        return buildAuthResponse(user, token);
    }

    @Transactional
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("E-mail já verificado");
        }

        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES));
        user.setVerificationAttempts(0);
        user.setVerificationLockUntil(null);
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationCode);
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return;
        }

        String resetCode = generateVerificationCode();
        user.setResetPasswordToken(resetCode);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(RESET_PASSWORD_CODE_EXPIRY_MINUTES));
        user.setResetPasswordAttempts(0);
        user.setResetPasswordLockUntil(null);
        userRepository.save(user);

        emailService.sendPasswordResetCodeEmail(user.getEmail(), user.getName(), resetCode);
    }

    @Transactional(noRollbackFor = { IllegalArgumentException.class, TooManyAttemptsException.class })
    public void verifyResetPasswordCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        validateResetPasswordCode(user, code, true);
        user.setResetPasswordAttempts(0);
        user.setResetPasswordLockUntil(null);
        userRepository.save(user);
    }

    @Transactional(noRollbackFor = { IllegalArgumentException.class, TooManyAttemptsException.class })
    public void resetPassword(String email, String code, String newPassword, String confirmPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        validateResetPasswordCode(user, code, false);

        passwordPolicyService.validateNewPassword(newPassword, confirmPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        adminResetPasswordLinkService.clearAdminResetLink(user);
        clearResetPasswordChallenge(user);

        boolean had2fa = Boolean.TRUE.equals(user.getTotpEnabled());
        if (had2fa) {
            totpService.disable(user);
        }

        // H6: invalida todos os tokens emitidos antes do reset de senha.
        user.setTokenVersion((user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1);

        saveUser(user);

        if (had2fa) {
            auditLogger.recordFor(
                    user,
                    "2FA_DISABLED",
                    "AUTH",
                    null,
                    user.getEmail(),
                    com.servija.servija.model.AuditLog.Result.SUCCESS,
                    Map.of("reason", "password_reset"));
        }
    }

    private void validateResetPasswordCode(User user, String code, boolean consumeAttemptsOnFailure) {
        LocalDateTime now = LocalDateTime.now();

        if (user.getResetPasswordLockUntil() != null && user.getResetPasswordLockUntil().isAfter(now)) {
            throw new TooManyAttemptsException(
                    "Muitas tentativas. Aguarde para tentar novamente.",
                    0,
                    user.getResetPasswordLockUntil());
        }

        if (user.getResetPasswordLockUntil() != null && user.getResetPasswordLockUntil().isBefore(now)) {
            user.setResetPasswordAttempts(0);
            user.setResetPasswordLockUntil(null);
            userRepository.save(user);
        }

        if (user.getResetPasswordToken() == null || !user.getResetPasswordToken().equals(code)) {
            if (!consumeAttemptsOnFailure) {
                throw new IllegalArgumentException("Código de recuperação inválido");
            }

            int attempts = safeAttempts(user.getResetPasswordAttempts()) + 1;
            user.setResetPasswordAttempts(attempts);

            if (attempts >= MAX_VERIFICATION_ATTEMPTS) {
                user.setResetPasswordLockUntil(now.plusMinutes(LOCKOUT_MINUTES));
                userRepository.save(user);
                throw new TooManyAttemptsException(
                        "Muitas tentativas. Aguarde para tentar novamente.",
                        0,
                        user.getResetPasswordLockUntil());
            }

            userRepository.save(user);
            int remaining = MAX_VERIFICATION_ATTEMPTS - attempts;
            throw new IllegalArgumentException(
                    remaining + " tentativa" + (remaining != 1 ? "s" : "") + " restante" + (remaining != 1 ? "s" : ""));
        }

        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(now)) {
            throw new IllegalArgumentException("Código de recuperação expirado. Solicite um novo.");
        }
    }

    private void clearResetPasswordChallenge(User user) {
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        user.setResetPasswordAttempts(0);
        user.setResetPasswordLockUntil(null);
    }

    private int safeAttempts(Integer attempts) {
        return attempts == null ? 0 : attempts;
    }

    private String resolveLoginFailureReason(Exception exception, boolean userNotFound) {
        if (userNotFound || exception instanceof ResourceNotFoundException) {
            return "user_not_found";
        }
        if (exception instanceof TooManyAttemptsException) {
            return "too_many_attempts";
        }
        if (exception instanceof EmailVerificationRequiredException) {
            return "email_not_verified";
        }
        if (exception instanceof InvalidCredentialsException && "Conta suspensa".equalsIgnoreCase(exception.getMessage())) {
            return "account_suspended";
        }
        return "invalid_credentials";
    }

    @Transactional
    public AuthResponse switchRole(String email, UserRoleRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        User.UserType requestedRole = request.getRole();
        validateInteractiveRole(requestedRole);

        if (!user.hasRole(requestedRole)) {
            throw new IllegalArgumentException("Este perfil não está habilitado para a conta");
        }

        user.setActiveRole(requestedRole);
        user = userRepository.save(user);
        return buildAuthResponse(user, generateAccessToken(user));
    }

    private User saveUser(User user) {
        return userRepository.save(user);
    }

    private User createBaseUser(RegisterRequest request) {
        Set<User.UserType> initialRoles = new LinkedHashSet<>();
        initialRoles.add(request.getUserType());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .cpf(request.getCpf())
                .phone(request.getPhone())
                .birthDate(request.getBirthDate())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(request.getUserType())
                .activeRole(request.getUserType())
                .roles(initialRoles)
                .emailVerified(false)
                .active(true)
                .build();
        user.addRole(request.getUserType());
        return user;
    }

    private void ensureProfileForRole(User user, User.UserType role) {
        validateInteractiveRole(role);

        if (role == User.UserType.CONTRACTOR) {
            contractorRepository.findById(user.getId())
                    .orElseGet(() -> contractorRepository.save(Contractor.builder()
                            .user(user)
                            .totalContracts(0)
                            .activeContracts(0)
                            .completedContracts(0)
                            .build()));
            user.setContractorProfile(contractorRepository.findById(user.getId()).orElse(null));
        } else if (role == User.UserType.FREELANCER) {
            freelancerRepository.findById(user.getId())
                    .orElseGet(() -> freelancerRepository.save(Freelancer.builder()
                            .user(user)
                            .totalReviews(0)
                            .completedJobs(0)
                            .activeJobs(0)
                            .profileCompleted(false)
                            .available(true)
                            .build()));
            user.setFreelancerProfile(freelancerRepository.findById(user.getId()).orElse(null));
        }

        user.addRole(role);
        userRepository.save(user);
    }

    private void validateInteractiveRole(User.UserType role) {
        if (role == null || role == User.UserType.ADMIN) {
            throw new IllegalArgumentException("Perfil inválido para esta operação");
        }
    }

    private String generateAccessToken(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return jwtService.generateToken(buildTokenClaims(user), userDetails);
    }

    private Map<String, Object> buildTokenClaims(User user) {
        Set<User.UserType> effectiveRoles = user.getRoles() == null || user.getRoles().isEmpty()
                ? Set.of(user.getUserType())
                : user.getRoles();

        return Map.of(
                "roles", effectiveRoles.stream().map(Enum::name).toList(),
                "active_role", user.getUserType().name(),
                "tv", user.getTokenVersion() != null ? user.getTokenVersion() : 0);
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .cpf(user.getCpf())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate())
                .userType(user.getUserType())
                .activeRole(user.getActiveRole())
                .roles(user.getRoles())
                .emailVerified(user.getEmailVerified())
                .avatarUrl(user.getAvatarUrl())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userDto)
                .build();
    }
}
