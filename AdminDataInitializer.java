package com.iceipt.hrms.config;

import com.iceipt.hrms.entity.User;
import com.iceipt.hrms.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminDataInitializer {

    @Bean
    public CommandLineRunner createAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String email,
            @Value("${app.admin.password}") String password) {

        return args -> {

            if (userRepository.findByEmail(email).isEmpty()) {

                User admin = new User();

                admin.setEmail(email);
                admin.setPassword(passwordEncoder.encode(password));
                admin.setRole(User.Role.ADMIN);
                admin.setEnabled(true);

                userRepository.save(admin);

                System.out.println("Admin account created successfully!");
            }
        };
    }
}