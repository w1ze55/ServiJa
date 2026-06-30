package com.servija.servija.service;

import com.servija.servija.model.User;
import com.servija.servija.repository.ContractorRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class UserRoleCompatibilityService implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final ContractorRepository contractorRepository;
    private final FreelancerRepository freelancerRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            repairLegacyData();
        } catch (Exception ex) {
            log.warn("Não foi possível reparar active_role/user_roles legados automaticamente.", ex);
        }
    }

    @Transactional
    public User repairIfNeeded(User user) {
        if (user == null || user.getId() == null) {
            return user;
        }

        User.UserType resolvedActiveRole = user.resolveEffectiveActiveRole();
        LinkedHashSet<User.UserType> resolvedRoles = resolveRoles(user, resolvedActiveRole);

        boolean shouldUpdateActiveRole = user.getStoredActiveRole() != resolvedActiveRole
                || user.getStoredUserType() != resolvedActiveRole;
        boolean shouldUpdateRoles = !normalized(user.getStoredRoles()).equals(resolvedRoles);

        if (!shouldUpdateActiveRole && !shouldUpdateRoles) {
            return user;
        }

        user.setRoles(resolvedRoles);
        user.setActiveRole(resolvedActiveRole);

        User saved = userRepository.save(user);
        log.info("Compatibilidade de perfis aplicada para o usuário {}", saved.getEmail());
        return saved;
    }

    void repairLegacyData() {
        int fixedActiveRole = jdbcTemplate.update("""
                UPDATE users
                SET active_role = user_type
                WHERE active_role IS NULL
                   OR (active_role = 'ADMIN' AND user_type <> 'ADMIN')
                """);

        int removedInvalidAdminRoles = jdbcTemplate.update("""
                DELETE ur
                FROM user_roles ur
                JOIN users u ON u.id = ur.user_id
                WHERE ur.role = 'ADMIN'
                  AND u.user_type <> 'ADMIN'
                """);

        int insertedBaseRoles = jdbcTemplate.update("""
                INSERT IGNORE INTO user_roles (user_id, role)
                SELECT id, user_type
                FROM users
                WHERE user_type IS NOT NULL
                """);

        int insertedContractorRoles = jdbcTemplate.update("""
                INSERT IGNORE INTO user_roles (user_id, role)
                SELECT user_id, 'CONTRACTOR'
                FROM contractors
                """);

        int insertedFreelancerRoles = jdbcTemplate.update("""
                INSERT IGNORE INTO user_roles (user_id, role)
                SELECT user_id, 'FREELANCER'
                FROM freelancers
                """);

        if (fixedActiveRole > 0
                || removedInvalidAdminRoles > 0
                || insertedBaseRoles > 0
                || insertedContractorRoles > 0
                || insertedFreelancerRoles > 0) {
            log.info(
                    "Compatibilidade de perfis aplicada: active_role={}, adminRolesRemovidos={}, baseRoles={}, contractorRoles={}, freelancerRoles={}",
                    fixedActiveRole,
                    removedInvalidAdminRoles,
                    insertedBaseRoles,
                    insertedContractorRoles,
                    insertedFreelancerRoles);
        }
    }

    private LinkedHashSet<User.UserType> resolveRoles(User user, User.UserType resolvedActiveRole) {
        LinkedHashSet<User.UserType> resolvedRoles = normalized(user.getStoredRoles());

        if (user.hasLegacyInvalidActiveRole()) {
            resolvedRoles.remove(User.UserType.ADMIN);
        }

        if (user.getStoredUserType() == User.UserType.ADMIN || user.getStoredActiveRole() == User.UserType.ADMIN) {
            if (user.getStoredUserType() == User.UserType.ADMIN) {
                resolvedRoles.add(User.UserType.ADMIN);
            }
        }

        if (contractorRepository.existsById(user.getId())) {
            resolvedRoles.add(User.UserType.CONTRACTOR);
        }

        if (freelancerRepository.existsById(user.getId())) {
            resolvedRoles.add(User.UserType.FREELANCER);
        }

        if (resolvedActiveRole != null) {
            resolvedRoles.add(resolvedActiveRole);
        }

        return resolvedRoles;
    }

    private LinkedHashSet<User.UserType> normalized(Set<User.UserType> roles) {
        LinkedHashSet<User.UserType> normalized = new LinkedHashSet<>();
        if (roles == null) {
            return normalized;
        }

        roles.stream()
                .filter(Objects::nonNull)
                .forEach(normalized::add);

        return normalized;
    }
}
