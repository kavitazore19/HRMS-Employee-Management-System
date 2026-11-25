package com.iceipt.hrms.service;

import com.iceipt.hrms.dto.LoginRequest;
import com.iceipt.hrms.dto.LoginResponse;
import com.iceipt.hrms.entity.User;
import com.iceipt.hrms.repository.UserRepository;
import com.iceipt.hrms.security.JwtService;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid email or password"));

        if (!user.getEnabled()
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        Long employeeId = user.getEmployee() != null
                ? user.getEmployee().getId()
                : null;

        String employeeName = user.getEmployee() != null
                ? user.getEmployee().getFullName()
                : null;

        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                employeeId,
                user.getEmail(),
                user.getRole().name(),
                employeeName
        );
    }
}