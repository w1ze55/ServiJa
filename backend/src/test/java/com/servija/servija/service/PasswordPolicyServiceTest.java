package com.servija.servija.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyServiceTest {

    private final PasswordPolicyService service = new PasswordPolicyService();

    @Test
    @DisplayName("validateNewPassword aceita senha forte com confirmacao igual")
    void validateNewPasswordAcceptsStrongPassword() {
        service.validateNewPassword("Senha@123", "Senha@123");
    }

    @Test
    @DisplayName("validateNewPassword rejeita confirmacao divergente")
    void validateNewPasswordRejectsMismatchedConfirmation() {
        assertThatThrownBy(() -> service.validateNewPassword("Senha@123", "Outra@123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não coincidem");
    }

    @Test
    @DisplayName("isValidPassword exige maiuscula minuscula numero e especial")
    void isValidPasswordRequiresAllCharacterGroups() {
        assertThat(service.isValidPassword("Senha@123")).isTrue();
        assertThat(service.isValidPassword("senha@123")).isFalse();
        assertThat(service.isValidPassword("SENHA@123")).isFalse();
        assertThat(service.isValidPassword("SenhaSemEspecial1")).isFalse();
        assertThat(service.isValidPassword("Senha@SemNumero")).isFalse();
    }
}
