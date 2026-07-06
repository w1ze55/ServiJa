package com.servija.servija.service;

import com.servija.servija.model.User;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

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
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedCpf = cpf == null ? "" : cpf.trim();
        String normalizedName = name == null ? "" : name.trim();
        String normalizedPhone = phone == null ? "" : phone.trim();

        if (!StringUtils.hasText(normalizedEmail)
                || !StringUtils.hasText(password)
                || !StringUtils.hasText(normalizedName)
                || !StringUtils.hasText(normalizedCpf)
                || !StringUtils.hasText(normalizedPhone)) {
            log.warn("Bootstrap de admin habilitado, mas configuração incompleta. Nenhum admin foi criado.");
            return;
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.info("Bootstrap de admin ignorado. Usuário {} já existe.", normalizedEmail);
            return;
        }

        if (userRepository.existsByCpf(normalizedCpf)) {
            log.warn("Bootstrap de admin ignorado. CPF {} já está cadastrado em outro usuário.", normalizedCpf);
            return;
        }

        User admin = User.builder()
                .name(normalizedName)
                .email(normalizedEmail)
                .cpf(normalizedCpf)
                .phone(normalizedPhone)
                .password(passwordEncoder.encode(password))
                .userType(User.UserType.ADMIN)
                .activeRole(User.UserType.ADMIN)
                .roles(Set.of(User.UserType.ADMIN))
                .emailVerified(true)
                .active(true)
                .build();

        try {
            userRepository.saveAndFlush(admin);
            log.info("Usuário admin bootstrap criado: {}", admin.getEmail());
        } catch (DataIntegrityViolationException ex) {
            log.error("Falha ao criar admin bootstrap {}. O startup seguirá sem esse usuário.", admin.getEmail(), ex);
        }
    }
}
