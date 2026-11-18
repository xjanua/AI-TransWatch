package me.xjanua.aitranswatch.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.xjanua.aitranswatch.dto.auth.LoginRequest;
import me.xjanua.aitranswatch.dto.auth.LoginResponse;
import me.xjanua.aitranswatch.dto.auth.UserMeResponse;
import me.xjanua.aitranswatch.enums.UserStatus;
import me.xjanua.aitranswatch.model.User;
import me.xjanua.aitranswatch.repository.UserRepository;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng"));
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng");
        }
        user.markLoggedIn();
        String token = jwtService.generateAccessToken(user);
        return LoginResponse.bearer(token, jwtService.getAccessTokenValidityInSeconds(), UserMeResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserMeResponse me(Authentication authentication) {
        User user = userRepository.findByUsernameIgnoreCase(authentication.getName())
                .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản không tồn tại hoặc không còn hoạt động"));
        return UserMeResponse.from(user);
    }
}
