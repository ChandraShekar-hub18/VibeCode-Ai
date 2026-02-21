package com.vibecode.auth.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vibecode.auth.dto.LoginRequest;
import com.vibecode.auth.dto.LoginResponse;
import com.vibecode.auth.dto.LogoutRequest;
import com.vibecode.auth.dto.RefreshTokenRequest;
import com.vibecode.auth.dto.RegisterRequest;
import com.vibecode.auth.entities.RefreshToken;
import com.vibecode.auth.entities.User;
import com.vibecode.auth.repositories.RefreshTokenRepository;
import com.vibecode.auth.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    // Registration method
    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {

            throw new RuntimeException("Email already exists");

        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .emailVerified(false)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
    }

    // Login method
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());

        String refreshToken = UUID.randomUUID().toString();

        RefreshToken token = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(refreshToken)) // Use hash() here
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken) // Return plain token
                .build();
    }

    public String hash(String token) {
        return DigestUtils.sha256Hex(token);
    }

    // token rfesh method
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hash(request.getRefreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (storedToken.getRevoked()) {
            throw new RuntimeException("Refresh token revoked");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = jwtService.generateToken(user.getId(), user.getEmail());

        return LoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(request.getRefreshToken())// reuse the same refresh token
                .build();

    }

    //logout method
    public void logout(LogoutRequest request){
        String tokenHash = hash(request.getRefreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
    }

}
