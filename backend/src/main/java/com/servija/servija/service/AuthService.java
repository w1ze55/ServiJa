package com.servija.servija.service;

import com.servija.servija.dto.AuthResponse;
import com.servija.servija.dto.LoginRequest;
import com.servija.servija.dto.RegisterRequest;
import com.servija.servija.exception.DuplicateResourceException;
import com.servija.servija.exception.InvalidCredentialsException;
import com.servija.servija.exception.ResourceNotFoundException;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ContractorRepository contractorRepository;
    private final FreelancerRepository freelancerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Verificar se email já existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("E-mail já cadastrado");
        }

        // Verificar se CPF já existe
        if (userRepository.existsByCpf(request.getCpf())) {
            throw new DuplicateResourceException("CPF já cadastrado");
        }

        // Criar usuário baseado no tipo
        User user;
        if (request.getUserType() == User.UserType.CONTRACTOR) {
            user = createContractor(request);
        } else {
            user = createFreelancer(request);
        }

        // Gerar código de verificação
        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusHours(24));

        // Salvar usuário
        if (user instanceof Contractor) {
            user = contractorRepository.save((Contractor) user);
        } else {
            user = freelancerRepository.save((Freelancer) user);
        }

        // Enviar email de verificação
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationCode);

        // Gerar token JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(user, token);
    }

    public AuthResponse login(LoginRequest request) {
        // Autenticar
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Buscar usuário
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Verificar se está ativo
        if (!user.getActive()) {
            throw new InvalidCredentialsException("Conta desativada");
        }

        // Gerar token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(user, token);
    }

    @Transactional
    public AuthResponse verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (user.getEmailVerified()) {
            throw new IllegalStateException("E-mail já verificado");
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new IllegalArgumentException("Código de verificação inválido");
        }

        if (user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código de verificação expirado");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);

        // Gerar token JWT após verificação bem-sucedida
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(user, token);
    }

    @Transactional
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (user.getEmailVerified()) {
            throw new IllegalStateException("E-mail já verificado");
        }

        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        // Enviar email de verificação
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationCode);
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElse(null);

        // Não revelamos se o email existe ou não por segurança
        if (user == null) {
            return;
        }

        // Gerar código de reset de senha (6 dígitos)
        String resetCode = generateVerificationCode();
        user.setResetPasswordToken(resetCode);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1)); // Expira em 1 hora
        userRepository.save(user);

        // Enviar email de reset de senha
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetCode);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword, String confirmPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Verificar código
        if (user.getResetPasswordToken() == null || !user.getResetPasswordToken().equals(code)) {
            throw new IllegalArgumentException("Código de recuperação inválido");
        }

        // Verificar expiração
        if (user.getResetPasswordTokenExpiry() == null || 
            user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código de recuperação expirado. Solicite um novo.");
        }

        // Verificar se as senhas coincidem
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("As senhas não coincidem");
        }

        // Validar força da senha
        if (!isValidPassword(newPassword)) {
            throw new IllegalArgumentException(
                "A senha deve ter no mínimo 8 caracteres e conter pelo menos: " +
                "1 letra maiúscula, 1 letra minúscula, 1 número e 1 caractere especial"
            );
        }

        // Atualizar senha
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);

        // Salvar no repositório apropriado
        if (user instanceof Contractor) {
            contractorRepository.save((Contractor) user);
        } else if (user instanceof Freelancer) {
            freelancerRepository.save((Freelancer) user);
        } else {
            userRepository.save(user);
        }
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 256) {
            return false;
        }

        boolean hasUpperCase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowerCase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*(),.?\":{}|<>".indexOf(ch) >= 0);

        return hasUpperCase && hasLowerCase && hasDigit && hasSpecial;
    }

    private Contractor createContractor(RegisterRequest request) {
        return Contractor.builder()
                .name(request.getName())
                .email(request.getEmail())
                .cpf(request.getCpf())
                .phone(request.getPhone())
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
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(User.UserType.FREELANCER)
                .emailVerified(false)
                .active(true)
                .totalReviews(0)
                .completedJobs(0)
                .activeJobs(0)
                .kycCompleted(false)
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


