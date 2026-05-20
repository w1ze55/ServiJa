package com.servija.servija.service;

import com.servija.servija.dto.AdminServiceSummaryDto;
import com.servija.servija.dto.AdminUserDetailDto;
import com.servija.servija.dto.AdminUserEditRequest;
import com.servija.servija.dto.AdminUserPageResponse;
import com.servija.servija.dto.AdminUserStatusRequest;
import com.servija.servija.dto.AdminUserSummaryDto;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Contractor;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.User;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10,11}$");

    private final UserRepository userRepository;
    private final FreelancerRepository freelancerRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ReviewRepository reviewRepository;
    private final AdminResetPasswordLinkService adminResetPasswordLinkService;

    public AdminUserPageResponse listUsers(
            String search,
            User.UserType type,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size,
            boolean includeDeleted) {

        Specification<User> spec = buildSpecification(search, type, status, dateFrom, dateTo, includeDeleted);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> result = userRepository.findAll(spec, pageable);

        List<AdminUserSummaryDto> content = result.getContent()
                .stream()
                .map(this::toSummaryDto)
                .toList();

        return AdminUserPageResponse.builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .build();
    }

    public AdminUserDetailDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return toDetailDto(user);
    }

    @Transactional
    public AdminUserSummaryDto updateStatus(Long id, AdminUserStatusRequest request, String adminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (user.getUserType() == User.UserType.ADMIN) {
            throw new IllegalArgumentException("Não é possível bloquear um administrador");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin não encontrado"));

        if (admin.getId().equals(id)) {
            throw new IllegalArgumentException("Não é possível bloquear a própria conta");
        }

        user.setActive(request.getActive());
        User saved = userRepository.save(user);
        return toSummaryDto(saved);
    }

    @Transactional
    public AdminUserDetailDto updateUser(Long id, AdminUserEditRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        validateEditRequest(request, user);

        user.setName(request.getName());

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new IllegalArgumentException("Este e-mail já está em uso");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getUserType() != null && !request.getUserType().equals(user.getUserType())) {
            applyUserTypeChange(user, request.getUserType());
        }

        if (user instanceof Freelancer freelancer && request.getBio() != null) {
            freelancer.setBio(request.getBio());
            freelancerRepository.save(freelancer);
            return toDetailDto(freelancer);
        }

        User saved = userRepository.save(user);
        return toDetailDto(saved);
    }

    @Transactional
    public void deleteUser(Long id, boolean hard) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (user.getUserType() == User.UserType.ADMIN) {
            throw new IllegalArgumentException("Não é possível excluir um administrador");
        }

        if (hard) {
            userRepository.delete(user);
        } else {
            user.setActive(false);
            user.setDeletedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    @Transactional
    public void resetPasswordByAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        adminResetPasswordLinkService.sendResetLink(user);
    }

    private void validateEditRequest(AdminUserEditRequest request, User existing) {
        if (request.getName() == null || request.getName().trim().length() < 3) {
            throw new IllegalArgumentException("Nome deve ter pelo menos 3 caracteres");
        }

        if (request.getEmail() != null && !EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new IllegalArgumentException("Formato de e-mail inválido");
        }

        if (request.getPhone() != null) {
            String digits = request.getPhone().replaceAll("\\D", "");
            if (!PHONE_PATTERN.matcher(digits).matches()) {
                throw new IllegalArgumentException("Telefone deve ter 10 ou 11 dígitos");
            }
            request.setPhone(digits);
        }

        if (request.getBio() != null && request.getBio().length() > 1000) {
            throw new IllegalArgumentException("Bio deve ter no máximo 1000 caracteres");
        }

        if (request.getUserType() == User.UserType.ADMIN) {
            throw new IllegalArgumentException("Não é possível alterar o tipo de usuário para ADMIN");
        }

        if (existing.getUserType() == User.UserType.ADMIN && request.getUserType() != null) {
            throw new IllegalArgumentException("Não é possível alterar o tipo de um administrador");
        }
    }

    private void applyUserTypeChange(User user, User.UserType newType) {
        Long userId = user.getId();

        if (newType == User.UserType.FREELANCER) {
            userRepository.updateUserTypeNative(userId, "FREELANCER");
            userRepository.insertFreelancerRecordIfNotExists(userId);
            user.setUserType(User.UserType.FREELANCER);
        } else if (newType == User.UserType.CONTRACTOR) {
            userRepository.updateUserTypeNative(userId, "CONTRACTOR");
            userRepository.insertContractorRecordIfNotExists(userId);
            user.setUserType(User.UserType.CONTRACTOR);
        }
    }

    private Specification<User> buildSpecification(
            String search,
            User.UserType type,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            boolean includeDeleted) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null) {
                query.distinct(true);
            }

            if (!includeDeleted) {
                predicates.add(cb.isNull(root.get("deletedAt")));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("userType"), type));
            }

            if ("active".equals(status)) {
                predicates.add(cb.equal(root.get("active"), true));
            } else if ("inactive".equals(status)) {
                predicates.add(cb.equal(root.get("active"), false));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), dateTo.plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AdminUserSummaryDto toSummaryDto(User user) {
        return AdminUserSummaryDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .userType(user.getUserType())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .avatarUrl(user.getAvatarUrl())
                .deletedAt(user.getDeletedAt())
                .build();
    }

    AdminUserDetailDto toDetailDto(User user) {
        AdminUserDetailDto.AdminUserDetailDtoBuilder builder = AdminUserDetailDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .cpf(user.getCpf())
                .userType(user.getUserType())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt());

        if (user instanceof Freelancer f) {
            Double freelancerAvgRating = reviewRepository.findAverageRatingByReviewedId(f.getId());
            long freelancerReviewCount = reviewRepository.countByReviewedId(f.getId());
            long completedCount = serviceRequestRepository.countByFreelancerIdAndStatus(f.getId(), ServiceRequest.Status.COMPLETED);
            long activeCount = serviceRequestRepository.countByFreelancerIdAndStatusIn(f.getId(),
                    List.of(ServiceRequest.Status.ACCEPTED, ServiceRequest.Status.IN_PROGRESS, ServiceRequest.Status.DELIVERED));
            builder.bio(f.getBio())
                    .professionalTitle(f.getProfessionalTitle())
                    .rating(freelancerAvgRating != null ? java.math.BigDecimal.valueOf(freelancerAvgRating) : null)
                    .totalReviews((int) freelancerReviewCount)
                    .completedJobs((int) completedCount)
                    .activeJobs((int) activeCount)
                    .profileCompleted(f.getProfileCompleted())
                    .category(f.getCategory())
                    .available(f.getAvailable())
                    .services(serviceRepository.findByFreelancerIdOrderByCreatedAtDesc(f.getId())
                            .stream()
                            .map(s -> AdminServiceSummaryDto.builder()
                                    .id(s.getId())
                                    .title(s.getTitle())
                                    .category(s.getCategory())
                                    .price(s.getPrice())
                                    .priceType(s.getPriceType() != null ? s.getPriceType().name() : null)
                                    .isActive(s.getIsActive())
                                    .createdAt(s.getCreatedAt())
                                    .build())
                            .toList());
        } else if (user instanceof Contractor) {
            Double avg = reviewRepository.findAverageRatingByReviewedId(user.getId());
            long reviewCount = reviewRepository.countByReviewedId(user.getId());
            long completed = serviceRequestRepository.countByContractorIdAndStatus(user.getId(), ServiceRequest.Status.COMPLETED);
            long active = serviceRequestRepository.countByContractorIdAndStatusIn(user.getId(),
                    List.of(ServiceRequest.Status.ACCEPTED, ServiceRequest.Status.IN_PROGRESS, ServiceRequest.Status.DELIVERED));
            long total = serviceRequestRepository.countByContractorIdAndStatusIn(user.getId(),
                    List.of(ServiceRequest.Status.ACCEPTED, ServiceRequest.Status.IN_PROGRESS,
                            ServiceRequest.Status.DELIVERED, ServiceRequest.Status.COMPLETED));
            builder.totalContracts((int) total)
                    .activeContracts((int) active)
                    .completedContracts((int) completed)
                    .contractorRating(avg != null ? java.math.BigDecimal.valueOf(avg) : null)
                    .contractorTotalReviews(reviewCount);
        }

        return builder.build();
    }
}
