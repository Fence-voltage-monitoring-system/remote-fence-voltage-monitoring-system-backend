package com.nerdc.elephantfence.backend.auth.service;

import com.nerdc.elephantfence.backend.auth.dto.ChangePasswordRequestDTO;
import com.nerdc.elephantfence.backend.auth.dto.LoginRequestDTO;
import com.nerdc.elephantfence.backend.auth.dto.LoginResponseDTO;
import com.nerdc.elephantfence.backend.auth.dto.RefreshTokenRequestDTO;
import com.nerdc.elephantfence.backend.common.security.JwtTokenProvider;
import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import com.nerdc.elephantfence.backend.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getId());

        User user = userRepository.findByIdWithProvincesAndDistricts(userPrincipal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userService.toUserResponseDTO(user))
                .build();
    }

    @Transactional
    public LoginResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        String token = request.getRefreshToken();
        if (!tokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        UUID userId = tokenProvider.getUserIdFromJWT(token);
        User user = userRepository.findByIdWithProvincesAndDistricts(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isEnabled()) {
            throw new IllegalStateException("User account is disabled");
        }

        String newAccessToken = tokenProvider.generateTokenFromUserId(user.getId(), 86400000L);
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId());

        return LoginResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .user(userService.toUserResponseDTO(user))
                .build();
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangeRequired(false);
        user.setPasswordChangedAt(OffsetDateTime.now());
        userRepository.save(user);
    }
}
