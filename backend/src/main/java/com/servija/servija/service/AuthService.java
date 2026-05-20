package com.servija.servija.service;

import com.servija.servija.dto.AuthResponse;
import com.servija.servija.dto.AddressRequest;
import com.servija.servija.dto.LoginRequest;
import com.servija.servija.dto.RegistrationValidationRequest;
import com.servija.servija.dto.RegisterRequest;
import com.servija.servija.exception.DuplicateResourceException;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;
    private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 10;
    private static final int RESET_PASSWORD_CODE_EXPIRY_MINUTES = 30;

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

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateRegistrationData(request);

        User user = request.getUserType() == User.UserType.CONTRACTOR
                ? createContractor(request)
                : createFreelancer(request);

        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES));

        if (user instanceof Contractor contractor) {
            user = contractorRepository.save(contractor);
        } else {
            user = freelancerRepository.save((Freelancer) user);
        }

        addressService.createAddressForUser(user, request.getAddress());

        emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationCode);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        auditLogger.recordFor(
                user,
                "USER_REGISTERED",
                "USER",
                "#USR-" + user.getId(),
                user.getName(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                Map.of("userType", user.getUserType()));

        return buildAuthResponse(user, token);
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

            if (user.getUserType() == User.UserType.ADMIN) {
                throw new InvalidCredentialsException("Credenciais invalidas");
            }

            if (!user.getActive()) {
                throw new InvalidCredentialsException("Conta suspensa");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            String token = jwtService.generateToken(userDetails);

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
                        Map.of("reason", e.getClass().getSimpleName()));
            } else if (attempted == null) {
                auditLogger.recordGuest(
                        request.getEmail(),
                        "LOGIN_FAILED",
                        "AUTH",
                        null,
                        request.getEmail(),
                        com.servija.servija.model.AuditLog.Result.FAILURE,
                        Map.of("reason", "USER_NOT_FOUND"));
            }
            throw e;
        }
    }

    public AuthResponse loginAdmin(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (user.getUserType() != User.UserType.ADMIN) {
            throw new InvalidCredentialsException("Credenciais invalidas");
        }

        if (!user.getActive()) {
            throw new InvalidCredentialsException("Conta suspensa");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(user, token);
    }

    @Transactional(noRollbackFor = { IllegalArgumentException.class, TooManyAttemptsException.class })
    public AuthResponse verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("E-mail ja verificado");
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

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

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
        saveUser(user);
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

    private User saveUser(User user) {
        if (user instanceof Contractor contractor) {
            return contractorRepository.save(contractor);
        }
        if (user instanceof Freelancer freelancer) {
            return freelancerRepository.save(freelancer);
        }
        return userRepository.save(user);
    }

    private Contractor createContractor(RegisterRequest request) {
        return Contractor.builder()
                .name(request.getName())
                .email(request.getEmail())
                .cpf(request.getCpf())
                .phone(request.getPhone())
                .birthDate(request.getBirthDate())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(User.UserType.CONTRACTOR)
                .emailVerified(false)
                .active(true)
                .totalContracts(0)
                .activeContracts(0)
                .completedContracts(0)
                .build();
    }

    private Freelancer createFreelancer(RegisterRequest request) {
        return Freelancer.builder()
                .name(request.getName())
                .email(request.getEmail())
                .cpf(request.getCpf())
                .phone(request.getPhone())
                .birthDate(request.getBirthDate())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(User.UserType.FREELANCER)
                .emailVerified(false)
                .active(true)
                .totalReviews(0)
                .completedJobs(0)
                .activeJobs(0)
                .profileCompleted(false)
                .available(true)
                .build();
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
