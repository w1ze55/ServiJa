package com.servija.servija.service;

import com.servija.servija.model.User;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    @DisplayName("loadUserByUsername retorna o usuário encontrado")
    void loadUserByUsernameReturnsUser() {
        User user = User.builder()
                .id(1L)
                .email("user@test.com")
                .password("secret")
                .name("User")
                .cpf("123")
                .phone("99999")
                .userType(User.UserType.CONTRACTOR)
                .activeRole(User.UserType.CONTRACTOR)
                .roles(Set.of(User.UserType.CONTRACTOR))
                .active(true)
                .build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername("user@test.com")).isSameAs(user);
    }

    @Test
    @DisplayName("loadUserByUsername lança UsernameNotFoundException quando o email não existe")
    void loadUserByUsernameThrowsWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("missing@test.com");
    }
}
