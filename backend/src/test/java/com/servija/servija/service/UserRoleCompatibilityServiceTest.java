package com.servija.servija.service;

import com.servija.servija.model.User;
import com.servija.servija.repository.ContractorRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleCompatibilityServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContractorRepository contractorRepository;

    @Mock
    private FreelancerRepository freelancerRepository;

    @InjectMocks
    private UserRoleCompatibilityService service;

    @Test
    @DisplayName("repairIfNeeded retorna o mesmo usuario quando nada precisa ser ajustado")
    void repairIfNeededReturnsUserWhenAlreadyConsistent() {
        User user = User.builder()
                .id(1L)
                .email("user@test.com")
                .userType(User.UserType.CONTRACTOR)
                .activeRole(User.UserType.CONTRACTOR)
                .roles(new LinkedHashSet<>(Set.of(User.UserType.CONTRACTOR)))
                .build();
        when(contractorRepository.existsById(1L)).thenReturn(true);
        when(freelancerRepository.existsById(1L)).thenReturn(false);

        User result = service.repairIfNeeded(user);

        assertThat(result).isSameAs(user);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("repairIfNeeded adiciona roles inferidas e persiste quando encontra divergencia")
    void repairIfNeededPersistsResolvedRoles() {
        User user = User.builder()
                .id(1L)
                .email("freela@test.com")
                .userType(User.UserType.CONTRACTOR)
                .activeRole(User.UserType.CONTRACTOR)
                .roles(new LinkedHashSet<>(Set.of(User.UserType.CONTRACTOR)))
                .build();
        when(contractorRepository.existsById(1L)).thenReturn(true);
        when(freelancerRepository.existsById(1L)).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);

        User saved = service.repairIfNeeded(user);

        assertThat(saved.getRoles()).contains(User.UserType.CONTRACTOR, User.UserType.FREELANCER);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("run captura falhas de compatibilidade sem propagar excecao")
    void runSwallowsCompatibilityFailures() {
        when(jdbcTemplate.update(any(String.class))).thenThrow(new RuntimeException("boom"));

        service.run(null);
    }
}
