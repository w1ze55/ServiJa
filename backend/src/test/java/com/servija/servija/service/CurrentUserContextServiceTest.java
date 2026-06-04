package com.servija.servija.service;

import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Contractor;
import com.servija.servija.model.User;
import com.servija.servija.repository.ContractorRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserContextServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContractorRepository contractorRepository;

    @Mock
    private FreelancerRepository freelancerRepository;

    @InjectMocks
    private CurrentUserContextService service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getOptionalAuthenticatedUser retorna vazio para autenticacao anonima")
    void getOptionalAuthenticatedUserReturnsEmptyForAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANON")));

        assertThat(service.getOptionalAuthenticatedUser()).isEmpty();
    }

    @Test
    @DisplayName("requireContractorMarketplaceAccess retorna o usuario autenticado no contexto contractor")
    void requireContractorMarketplaceAccessReturnsContractor() {
        User user = user(User.UserType.CONTRACTOR);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user.getEmail(), "pwd", AuthorityUtils.createAuthorityList("ROLE_CONTRACTOR")));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThat(service.requireContractorMarketplaceAccess()).isSameAs(user);
    }

    @Test
    @DisplayName("requireContractorProfile falha quando o perfil nao existe")
    void requireContractorProfileFailsWhenProfileIsMissing() {
        User user = user(User.UserType.CONTRACTOR);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user.getEmail(), "pwd", AuthorityUtils.createAuthorityList("ROLE_CONTRACTOR")));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(contractorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(service::requireContractorProfile)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cliente");
    }

    @Test
    @DisplayName("denyFreelancerMarketplaceAccessIfAuthenticated bloqueia freelancer autenticado")
    void denyFreelancerMarketplaceAccessIfAuthenticatedBlocksFreelancer() {
        User user = user(User.UserType.FREELANCER);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user.getEmail(), "pwd", AuthorityUtils.createAuthorityList("ROLE_FREELANCER")));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(service::denyFreelancerMarketplaceAccessIfAuthenticated)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Marketplace indisponivel");
    }

    @Test
    @DisplayName("requireAuthenticatedUser falha quando o usuario autenticado nao pode ser carregado")
    void requireAuthenticatedUserFailsWhenRepositoryDoesNotReturnUser() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "ghost@test.com", "pwd", AuthorityUtils.createAuthorityList("ROLE_CONTRACTOR")));
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(service::requireAuthenticatedUser)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("autenticado");
    }

    private User user(User.UserType role) {
        return User.builder()
                .id(1L)
                .name("User")
                .email(role.name().toLowerCase() + "@test.com")
                .cpf("123")
                .phone("99999")
                .password("secret")
                .userType(role)
                .activeRole(role)
                .roles(Set.of(role))
                .active(true)
                .build();
    }
}
