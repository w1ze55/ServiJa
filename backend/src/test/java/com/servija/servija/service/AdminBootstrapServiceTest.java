package com.servija.servija.service;

import com.servija.servija.model.User;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminBootstrapService service;

    @Test
    @DisplayName("run nao cria usuario quando bootstrap esta desabilitado")
    void runDoesNothingWhenDisabled() {
        ReflectionTestUtils.setField(service, "enabled", false);

        service.run(null);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("run cria admin quando a configuracao esta completa")
    void runCreatesAdminWhenConfigurationIsComplete() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "name", "Admin");
        ReflectionTestUtils.setField(service, "email", "admin@test.com");
        ReflectionTestUtils.setField(service, "password", "Senha@123");
        ReflectionTestUtils.setField(service, "cpf", "12345678900");
        ReflectionTestUtils.setField(service, "phone", "11999999999");
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(userRepository.existsByCpf("12345678900")).thenReturn(false);
        when(passwordEncoder.encode("Senha@123")).thenReturn("encoded");

        service.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRoles()).containsExactly(User.UserType.ADMIN);
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
    }
}
