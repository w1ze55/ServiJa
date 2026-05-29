package com.servija.servija.service;

import com.servija.servija.dto.AuthResponse;
import com.servija.servija.dto.ChangePasswordRequest;
import com.servija.servija.dto.DeleteAccountRequest;
import com.servija.servija.dto.ProfileResponse;
import com.servija.servija.dto.UpdateAvatarRequest;
import com.servija.servija.dto.UpdateUserRequest;
import com.servija.servija.exception.InvalidCredentialsException;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Contractor;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.ContractorRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ContractorRepository contractorRepository;
    private final FreelancerRepository freelancerRepository;
    private final ReviewRepository reviewRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final FileStorageService fileStorageService;
    private final WebSocketService webSocketService;
    private final AuditLogger auditLogger;

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        LocalDateTime previousUpdatedAt = user.getUpdatedAt();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Senha atual incorreta");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("As senhas nao coincidem");
        }

        String newPassword = request.getNewPassword();
        if (!isValidPassword(newPassword)) {
            throw new IllegalArgumentException(
                    "A senha deve ter no minimo 8 caracteres e conter pelo menos: "
                            + "1 letra maiuscula, 1 letra minuscula, 1 numero e 1 caractere especial");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user = userRepository.save(user);

        auditLogger.recordSemantic(
                "PASSWORD_CHANGED",
                "USER",
                String.valueOf(user.getId()),
                user.getName(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                Map.of("fieldsChanged", List.of("updatedAt")),
                singleFieldState("updatedAt", previousUpdatedAt),
                singleFieldState("updatedAt", user.getUpdatedAt()));
    }

    @Transactional
    public AuthResponse.UserDto updateAvatar(String email, UpdateAvatarRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        String oldAvatarUrl = user.getAvatarUrl();
        String finalAvatarUrl = null;

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
            finalAvatarUrl = request.getAvatarUrl();
        } else if (request.getAvatarBase64() != null && !request.getAvatarBase64().isEmpty()) {
            String base64 = request.getAvatarBase64();
            if (base64.contains(",")) {
                base64 = base64.substring(base64.indexOf(",") + 1);
            }

            if (base64.length() > 6_700_000) {
                throw new IllegalArgumentException("Imagem muito grande. Tamanho maximo: 5MB");
            }

            if (!request.getAvatarBase64().startsWith("data:image")) {
                finalAvatarUrl = "data:image/jpeg;base64," + base64;
            } else {
                finalAvatarUrl = request.getAvatarBase64();
            }
        }

        if (finalAvatarUrl == null) {
            throw new IllegalArgumentException("Nenhuma imagem fornecida (URL ou Base64)");
        }

        entityManager.createNativeQuery(
                        "UPDATE users SET avatar_url = :avatarUrl, updated_at = NOW() WHERE id = :userId")
                .setParameter("avatarUrl", finalAvatarUrl)
                .setParameter("userId", user.getId())
                .executeUpdate();

        if (oldAvatarUrl != null && !oldAvatarUrl.equals(finalAvatarUrl) && isS3Url(oldAvatarUrl)) {
            try {
                fileStorageService.deleteFile(oldAvatarUrl);
            } catch (Exception e) {
                log.warn("Falha ao deletar avatar antigo do S3: {}. Erro: {}", oldAvatarUrl, e.getMessage());
            }
        }

        entityManager.flush();
        entityManager.clear();

        User updatedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado apos atualizacao"));

        webSocketService.notifyProfileImageUpdate(email, finalAvatarUrl);
        if (!Objects.equals(summarizeAvatarValue(oldAvatarUrl), summarizeAvatarValue(updatedUser.getAvatarUrl()))) {
            auditLogger.recordSemantic(
                    "AVATAR_UPDATED",
                    "USER",
                    String.valueOf(updatedUser.getId()),
                    updatedUser.getName(),
                    com.servija.servija.model.AuditLog.Result.SUCCESS,
                    Map.of("fieldsChanged", List.of("avatarUrl")),
                    singleFieldState("avatarUrl", summarizeAvatarValue(oldAvatarUrl)),
                    singleFieldState("avatarUrl", summarizeAvatarValue(updatedUser.getAvatarUrl())));
        }

        return buildUserDto(updatedUser);
    }

    @Transactional
    public void deleteAccount(String email, DeleteAccountRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Senha incorreta. A conta nao foi excluida.");
        }

        Map<String, Object> deletedUser = buildUserAuditState(user);
        userRepository.delete(user);

        auditLogger.recordSemantic(
                "ACCOUNT_DELETED",
                "USER",
                String.valueOf(user.getId()),
                user.getName(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                null,
                deletedUser,
                null,
                null);
    }

    public AuthResponse.UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        return buildUserDto(user);
    }

    @Transactional
    public AuthResponse.UserDto updateUser(String email, UpdateUserRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        Map<String, Object> beforeState = buildUserAuditState(user);
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        if (request.getCpf() != null && !request.getCpf().isEmpty()) {
            user.setCpf(request.getCpf());
        }

        user = userRepository.save(user);

        Map<String, Object> afterState = buildUserAuditState(user);
        List<String> changedFields = AuditStateDiffUtils.changedFields(beforeState, afterState);
        if (!changedFields.isEmpty()) {
            auditLogger.recordSemantic(
                    "PROFILE_UPDATED",
                    "USER",
                    String.valueOf(user.getId()),
                    user.getName(),
                    com.servija.servija.model.AuditLog.Result.SUCCESS,
                    Map.of("fieldsChanged", changedFields),
                    AuditStateDiffUtils.filterFields(beforeState, changedFields),
                    AuditStateDiffUtils.filterFields(afterState, changedFields));
        }

        return buildUserDto(user);
    }

    public ProfileResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        return buildProfileResponse(user);
    }

    @Transactional
    public ProfileResponse activateProfile(String email, User.UserType role) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        if (role == null || role == User.UserType.ADMIN) {
            throw new IllegalArgumentException("Perfil invalido para ativacao");
        }

        if (role == User.UserType.CONTRACTOR) {
            contractorRepository.findById(user.getId()).orElseGet(() -> contractorRepository.save(
                    Contractor.builder()
                            .user(user)
                            .totalContracts(0)
                            .activeContracts(0)
                            .completedContracts(0)
                            .build()));
        } else {
            freelancerRepository.findById(user.getId()).orElseGet(() -> freelancerRepository.save(
                    Freelancer.builder()
                            .user(user)
                            .totalReviews(0)
                            .completedJobs(0)
                            .activeJobs(0)
                            .profileCompleted(false)
                            .available(true)
                            .build()));
        }

        user.addRole(role);
        userRepository.save(user);
        return buildProfileResponse(user);
    }

    private ProfileResponse buildProfileResponse(User user) {
        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
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
                .updatedAt(user.getUpdatedAt());

        contractorRepository.findById(user.getId()).ifPresent(contractor -> {
            Double contractorAvg = reviewRepository.findAverageRatingByReviewedId(contractor.getId());
            long contractorReviewCount = reviewRepository.countByReviewedId(contractor.getId());
            long completed = serviceRequestRepository.countByContractorIdAndStatus(
                    contractor.getId(),
                    ServiceRequest.Status.COMPLETED);
            long active = serviceRequestRepository.countByContractorIdAndStatusIn(
                    contractor.getId(),
                    List.of(
                            ServiceRequest.Status.ACCEPTED,
                            ServiceRequest.Status.IN_PROGRESS,
                            ServiceRequest.Status.DELIVERED));
            long total = active + completed;

            builder.contractorProfile(ProfileResponse.ContractorProfile.builder()
                    .preferredLocation(contractor.getPreferredLocation())
                    .totalContracts((int) total)
                    .activeContracts((int) active)
                    .completedContracts((int) completed)
                    .contractorRating(contractorAvg != null ? BigDecimal.valueOf(contractorAvg) : null)
                    .contractorTotalReviews(contractorReviewCount)
                    .build());
        });

        freelancerRepository.findById(user.getId()).ifPresent(freelancer -> {
            Double averageRating = reviewRepository.findAverageRatingByReviewedId(freelancer.getId());
            long totalReviewsCount = reviewRepository.countByReviewedId(freelancer.getId());

            builder.freelancerProfile(ProfileResponse.FreelancerProfile.builder()
                    .bio(freelancer.getBio())
                    .professionalTitle(freelancer.getProfessionalTitle())
                    .location(freelancer.getLocation())
                    .rating(averageRating != null ? BigDecimal.valueOf(averageRating) : null)
                    .totalReviews((int) totalReviewsCount)
                    .completedJobs(freelancer.getCompletedJobs())
                    .activeJobs(freelancer.getActiveJobs())
                    .profileCompleted(freelancer.getProfileCompleted())
                    .available(freelancer.getAvailable())
                    .build());
        });

        return builder.build();
    }

    private AuthResponse.UserDto buildUserDto(User user) {
        return AuthResponse.UserDto.builder()
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

    private boolean isS3Url(String url) {
        return url != null && url.contains(".s3.") && url.contains("amazonaws.com");
    }

    private Map<String, Object> buildUserAuditState(User user) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("name", user.getName());
        state.put("phone", user.getPhone());
        state.put("cpf", user.getCpf());
        return state;
    }

    private String summarizeAvatarValue(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return null;
        }
        if (avatarUrl.startsWith("data:image")) {
            return "[INLINE_IMAGE]";
        }
        return avatarUrl.length() <= 120 ? avatarUrl : avatarUrl.substring(0, 117) + "...";
    }

    private Map<String, Object> singleFieldState(String fieldName, Object value) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put(fieldName, value);
        return state;
    }
}
