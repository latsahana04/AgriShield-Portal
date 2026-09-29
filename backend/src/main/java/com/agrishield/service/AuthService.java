package com.agrishield.service;

import com.agrishield.config.JwtTokenProvider;
import com.agrishield.dto.AuthResponse;
import com.agrishield.dto.LoginRequest;
import com.agrishield.entity.User;
import com.agrishield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or license ID"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials provided");
        }

        String token = jwtTokenProvider.generateToken(
                user.getUsername(),
                user.getRole(),
                user.getLicenseId(),
                user.getOrgName()
        );

        log.info("Issued 15-minute JWT session for user: {} with role: {}", user.getUsername(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtTokenProvider.getExpirationMs())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .licenseId(user.getLicenseId())
                .orgName(user.getOrgName())
                .build();
    }
}
