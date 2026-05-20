package com.servija.servija.service;

import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    public void validateNewPassword(String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("As senhas não coincidem");
        }

        if (!isValidPassword(newPassword)) {
            throw new IllegalArgumentException(
                    "A senha deve ter no mínimo 8 caracteres e conter pelo menos: "
                            + "1 letra maiúscula, 1 letra minúscula, 1 número e 1 caractere especial");
        }
    }

    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 256) {
            return false;
        }

        boolean hasUpperCase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowerCase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*(),.?\":{}|<>".indexOf(ch) >= 0);

        return hasUpperCase && hasLowerCase && hasDigit && hasSpecial;
    }
}
