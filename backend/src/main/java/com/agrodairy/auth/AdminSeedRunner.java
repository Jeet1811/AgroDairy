package com.agrodairy.auth;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeedRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedEmail;
    private final String seedPassword;

    public AdminSeedRunner(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.admin-seed.email}") String seedEmail,
                            @Value("${app.admin-seed.password}") String seedPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEmail = seedEmail;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(String... args) {
        if (seedEmail.isBlank() || seedPassword.isBlank()) {
            return;
        }
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        User admin = User.builder()
                .email(seedEmail)
                .passwordHash(passwordEncoder.encode(seedPassword))
                .fullName("Administrator")
                .role(Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(admin);
    }
}
