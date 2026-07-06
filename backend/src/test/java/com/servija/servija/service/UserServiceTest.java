package com.servija.servija.service;

import com.servija.servija.dto.AuthResponse;
import com.servija.servija.dto.ChangePasswordRequest;
import com.servija.servija.dto.UpdateAvatarRequest;
import com.servija.servija.exception.InvalidCredentialsException;
import com.servija.servija.model.Contractor;
import com.servija.servija.model.User;
import com.servija.servija.repository.ContractorRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ContractorRepository contractorRepository;
    @Mock
    private FreelancerRepository freelancerRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EntityManager entityManager;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private Query nativeQuery;

    @InjectMocks
    private UserService service;

    @Test
    @DisplayName("changePassword atualiza a senha e registra auditoria")
    void changePasswordUpdatesPasswordAndAudits() {
        User user = baseUser();
        user.setUpdatedAt(LocalDateTime.of(2026, 1, 10, 10, 0));
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old")
                .newPassword("NovaSenha@123")
                .confirmPassword("NovaSenha@123")
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("encoded-new");
        when(userRepository.save(user)).thenAnswer(invocation -> {
            user.setUpdatedAt(LocalDateTime.of(2026, 1, 11, 11, 0));
            return user;
        });

        service.changePassword(user.getEmail(), request);

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        verify(auditLogger).recordSemantic(
                anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("changePassword rejeita senha atual incorreta")
    void changePasswordRejectsWrongCurrentPassword() {
        User user = baseUser();
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrong")
                .newPassword("NovaSenha@123")
                .confirmPassword("NovaSenha@123")
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(user.getEmail(), request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Senha atual");
    }

    @Test
    @DisplayName("updateAvatar aceita base64, atualiza o usuário e publica websocket")
    void updateAvatarUpdatesAvatarAndNotifiesSocket() {
        User user = baseUser();
        User updated = baseUser();
        updated.setAvatarUrl("data:image/png;base64,abc123");
        UpdateAvatarRequest request = UpdateAvatarRequest.builder()
                .avatarBase64("data:image/png;base64,abc123")
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.executeUpdate()).thenReturn(1);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(updated));

        AuthResponse.UserDto response = service.updateAvatar(user.getEmail(), request);

        assertThat(response.getAvatarUrl()).isEqualTo("data:image/png;base64,abc123");
        verify(webSocketService).notifyProfileImageUpdate(user.getEmail(), "data:image/png;base64,abc123");
    }

    @Test
    @DisplayName("activateProfile cria perfil contractor ausente e adiciona role")
    void activateProfileCreatesMissingContractorProfile() {
        User user = baseUser();
        user.setRoles(new java.util.LinkedHashSet<>(Set.of(User.UserType.FREELANCER)));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        Contractor contractor = Contractor.builder().id(1L).user(user).build();
        when(contractorRepository.findById(1L)).thenReturn(Optional.empty(), Optional.of(contractor));
        when(contractorRepository.save(any(Contractor.class))).thenReturn(contractor);
        when(userRepository.save(user)).thenReturn(user);
        when(reviewRepository.findAverageRatingByReviewedId(any())).thenReturn(null);
        when(reviewRepository.countByReviewedId(any())).thenReturn(0L);
        when(serviceRequestRepository.countByContractorIdAndStatus(any(), any())).thenReturn(0L);
        when(serviceRequestRepository.countByContractorIdAndStatusIn(any(), any())).thenReturn(0L);

        var response = service.activateProfile(user.getEmail(), User.UserType.CONTRACTOR);

        assertThat(user.getRoles()).contains(User.UserType.CONTRACTOR);
        assertThat(response.getContractorProfile()).isNotNull();
    }

    private User baseUser() {
        return User.builder()
                .id(1L)
                .name("User")
                .email("user@test.com")
                .cpf("12345678900")
                .phone("11999999999")
                .password("encoded-old")
                .userType(User.UserType.FREELANCER)
                .activeRole(User.UserType.FREELANCER)
                .roles(new java.util.LinkedHashSet<>(Set.of(User.UserType.FREELANCER)))
                .active(true)
                .build();
    }
}
