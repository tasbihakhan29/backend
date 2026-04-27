package com.medsync.config;

import com.medsync.entity.User;
import com.medsync.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Value("${app.seed-default-admin:false}")
    private boolean seedDefaultAdmin;

    @Override
    public void run(String... args) {
        if (!seedDefaultAdmin) {
            System.out.println("Default admin seeding is disabled");
            return;
        }

        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@medsync.city");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRole(User.Role.CITY_ADMIN);
            admin.setStatus(User.AccountStatus.ACTIVE);
            userRepository.save(admin);
            System.out.println("Default admin created: username=admin, password=Admin@123");
        }
    }
}
