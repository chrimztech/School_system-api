package com.srms.api.config;

import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class SystemAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@srms.zm}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@SRMS2024!}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        boolean exists = userRepository.findByEmail(adminEmail).isPresent();
        if (!exists) {
            AppUser admin = new AppUser();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setName("System Administrator");
            admin.setInitials("SA");
            admin.setRole(AppUser.UserRole.SUPER_ADMIN);
            admin.setActive(true);
            userRepository.save(admin);
            log.info("System admin created: {}", adminEmail);
        }
    }
}
