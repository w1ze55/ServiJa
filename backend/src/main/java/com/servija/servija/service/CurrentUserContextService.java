package com.servija.servija.service;

import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Contractor;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.User;
import com.servija.servija.repository.ContractorRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserContextService {

    private final UserRepository userRepository;
    private final ContractorRepository contractorRepository;
    private final FreelancerRepository freelancerRepository;

    public Optional<User> getOptionalAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return Optional.empty();
        }

        return userRepository.findByEmail(authentication.getName());
    }

    public User requireAuthenticatedUser() {
        return getOptionalAuthenticatedUser()
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado nao encontrado"));
    }

    public User requireActiveRole(User.UserType role) {
        User user = requireAuthenticatedUser();
        if (user.getUserType() != role) {
            throw new IllegalStateException(messageForRole(role));
        }
        return user;
    }

    public Contractor requireContractorProfile() {
        User user = requireActiveRole(User.UserType.CONTRACTOR);
        return contractorRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Perfil de cliente nao encontrado"));
    }

    public Freelancer requireFreelancerProfile() {
        User user = requireActiveRole(User.UserType.FREELANCER);
        return freelancerRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Perfil de freelancer nao encontrado"));
    }

    public void denyOwnFreelancerPublicAccess(Long freelancerUserId) {
        getOptionalAuthenticatedUser()
                .filter(user -> user.getId().equals(freelancerUserId))
                .filter(user -> user.getUserType() == User.UserType.CONTRACTOR)
                .ifPresent(user -> {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Voce nao pode acessar o proprio perfil publico de freelancer neste contexto");
                });
    }

    private String messageForRole(User.UserType role) {
        return switch (role) {
            case CONTRACTOR -> "Usuario nao esta no contexto de cliente";
            case FREELANCER -> "Usuario nao esta no contexto de freelancer";
            case ADMIN -> "Usuario nao esta no contexto de administrador";
        };
    }
}
