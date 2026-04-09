package com.servija.servija.service;

import com.servija.servija.model.User;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${servija.admin.bootstrap.enabled:false}")
    private boolean enabled;

    @Value("${servija.admin.bootstrap.name:}")
    private String name;

    @Value("${servija.admin.bootstrap.email:}")
    private String email;

    @Value("${servija.admin.bootstrap.password:}")
    private String password;

    @Value("${servija.admin.bootstrap.cpf:}")
    private String cpf;

    @Value("${servija.admin.bootstrap.phone:}")
    private String phone;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        if (!StringUtils.hasText(email)
                || !StringUtils.hasText(password)
                || !StringUtils.hasText(name)
                || !StringUtils.hasText(cpf)
                || !StringUtils.hasText(phone)) {
            log.warn("Bootstrap de admin habilitado, mas configuracao incompleta. Nenhum admin foi criado.");
            return;
        }

        if (userRepository.existsByEmail(email.trim())) {
            log.info("Bootstrap de admin ignorado. Usuario {} ja existe.", email.trim());
            return;
        }

        User admin = User.builder()
                .name(name.trim())
                .email(email.trim())
                .cpf(cpf.trim())
                .phone(phone.trim())
                .password(passwordEncoder.encode(password))
                .userType(User.UserType.ADMIN)
                .emailVerified(true)
                .active(true)
                .build();

        userRepository.save(admin);
        log.info("Usuario admin bootstrap criado: {}", admin.getEmail());
    }
}
