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
import com.servija.servija.model.User;
import com.servija.servija.repository.ContractorRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ContractorRepository contractorRepository;
    private final FreelancerRepository freelancerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Validar senha atual
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Senha atual incorreta");
        }

        // Validar que as senhas coincidem
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("As senhas não coincidem");
        }

        // Validar força da senha (mesmas regras do registro)
        String newPassword = request.getNewPassword();
        if (!isValidPassword(newPassword)) {
            throw new IllegalArgumentException(
                "A senha deve ter no mínimo 8 caracteres e conter pelo menos: " +
                "1 letra maiúscula, 1 letra minúscula, 1 número e 1 caractere especial"
            );
        }

        // Atualizar senha
        user.setPassword(passwordEncoder.encode(newPassword));

        // Salvar no repositório apropriado
        if (user instanceof Contractor) {
            contractorRepository.save((Contractor) user);
        } else if (user instanceof Freelancer) {
            freelancerRepository.save((Freelancer) user);
        } else {
            userRepository.save(user);
        }
    }

    @Transactional
    public AuthResponse.UserDto updateAvatar(String email, UpdateAvatarRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Validar formato base64
        String base64 = request.getAvatarBase64();
        if (base64 == null || base64.isEmpty()) {
            throw new IllegalArgumentException("Imagem inválida");
        }

        // Remover prefixo data:image se existir
        if (base64.contains(",")) {
            base64 = base64.substring(base64.indexOf(",") + 1);
        }

        // Validar tamanho (limite de 2MB em base64 = ~1.5MB original)
        if (base64.length() > 2_800_000) { // ~2MB em base64
            throw new IllegalArgumentException("Imagem muito grande. Tamanho máximo: 2MB");
        }

        // Salvar avatar em base64
        // Adicionar prefixo data:image/jpeg para facilitar exibição no frontend
        String finalAvatarUrl;
        if (!request.getAvatarBase64().startsWith("data:image")) {
            finalAvatarUrl = "data:image/jpeg;base64," + base64;
        } else {
            finalAvatarUrl = request.getAvatarBase64();
        }
        
        // Usar query nativa para garantir que o campo TEXT seja reconhecido
        entityManager.createNativeQuery(
            "UPDATE users SET avatar_url = :avatarUrl, updated_at = NOW() WHERE id = :userId"
        )
        .setParameter("avatarUrl", finalAvatarUrl)
        .setParameter("userId", user.getId())
        .executeUpdate();
        
        // Forçar flush para garantir que foi salvo
        entityManager.flush();
        entityManager.clear(); // Limpar cache do Hibernate
        
        // Recarregar o usuário atualizado do banco
        User updatedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado após atualização"));

        return buildUserDto(updatedUser);
    }

    @Transactional
    public void deleteAccount(String email, DeleteAccountRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Validar senha antes de excluir
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Senha incorreta. A conta não foi excluída.");
        }

        // Deletar do repositório apropriado
        // Com JOINED inheritance, deletar do UserRepository deve deletar também a entidade filha
        if (user instanceof Contractor) {
            contractorRepository.delete((Contractor) user);
        } else if (user instanceof Freelancer) {
            freelancerRepository.delete((Freelancer) user);
        }
        // O Hibernate gerencia a exclusão da tabela pai (users) automaticamente
        // Mas para garantir, vamos deletar explicitamente do UserRepository também
        userRepository.delete(user);
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

    public AuthResponse.UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return buildUserDto(user);
    }

    @Transactional
    public AuthResponse.UserDto updateUser(String email, UpdateUserRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Atualizar dados básicos
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        if (request.getCpf() != null && !request.getCpf().isEmpty()) {
            user.setCpf(request.getCpf());
        }

        // Salvar no repositório apropriado
        if (user instanceof com.servija.servija.model.Contractor) {
            user = contractorRepository.save((com.servija.servija.model.Contractor) user);
        } else if (user instanceof com.servija.servija.model.Freelancer) {
            user = freelancerRepository.save((com.servija.servija.model.Freelancer) user);
        } else {
            user = userRepository.save(user);
        }

        return buildUserDto(user);
    }

    public ProfileResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
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
                .updatedAt(user.getUpdatedAt());

        // Adicionar dados específicos baseado no tipo
        if (user instanceof Contractor) {
            Contractor contractor = (Contractor) user;
            builder.contractorProfile(ProfileResponse.ContractorProfile.builder()
                    .preferredLocation(contractor.getPreferredLocation())
                    .totalContracts(contractor.getTotalContracts())
                    .activeContracts(contractor.getActiveContracts())
                    .completedContracts(contractor.getCompletedContracts())
                    .build());
        } else if (user instanceof Freelancer) {
            Freelancer freelancer = (Freelancer) user;
            builder.freelancerProfile(ProfileResponse.FreelancerProfile.builder()
                    .bio(freelancer.getBio())
                    .professionalTitle(freelancer.getProfessionalTitle())
                    .location(freelancer.getLocation())
                    .rating(freelancer.getRating())
                    .totalReviews(freelancer.getTotalReviews())
                    .completedJobs(freelancer.getCompletedJobs())
                    .activeJobs(freelancer.getActiveJobs())
                    .kycCompleted(freelancer.getKycCompleted())
                    .profileCompleted(freelancer.getProfileCompleted())
                    .available(freelancer.getAvailable())
                    .build());
        }

        return builder.build();
    }

    private AuthResponse.UserDto buildUserDto(User user) {
        return AuthResponse.UserDto.builder()
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
    }
}

