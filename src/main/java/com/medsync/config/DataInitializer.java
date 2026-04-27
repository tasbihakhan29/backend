package com.medsync.config;

import com.medsync.entity.User;
import com.medsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@medsync.city")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(User.Role.CITY_ADMIN)
                    .status(User.AccountStatus.ACTIVE)
                    .build();
            userRepository.save(admin);
            log.info("Default admin created: username=admin, password=Admin@123");
        }
    }
}
