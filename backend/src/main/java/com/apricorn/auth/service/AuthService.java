package com.apricorn.auth.service;

import com.apricorn.auth.dto.AuthResponse;
import com.apricorn.auth.dto.LoginRequest;
import com.apricorn.auth.dto.RegisterRequest;
import com.apricorn.exception.ConflictException;
import com.apricorn.security.jwt.JwtProperties;
import com.apricorn.security.jwt.JwtService;
import com.apricorn.user.entity.Role;
import com.apricorn.user.entity.User;
import com.apricorn.user.mapper.UserMapper;
import com.apricorn.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService, JwtProperties jwtProperties, UserMapper userMapper) {
        this.userRepository = userRepository; this.passwordEncoder = passwordEncoder; this.authenticationManager = authenticationManager;
        this.jwtService = jwtService; this.jwtProperties = jwtProperties; this.userMapper = userMapper;
    }
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase();
        if (userRepository.existsByEmail(email)) throw new ConflictException("Email is already registered");
        if (userRepository.existsByUsername(request.username())) throw new ConflictException("Username is already registered");
        User user = new User();
        user.setUsername(request.username()); user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password())); user.setRole(Role.USER); user.setEnabled(true);
        User saved = userRepository.save(user);
        log.info("Registered user {}", saved.getEmail());
        return responseFor(saved);
    }
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        User user = userRepository.findByEmail(request.email().toLowerCase()).orElseThrow();
        log.info("Successful login for {}", user.getEmail());
        return responseFor(user);
    }
    private AuthResponse responseFor(User user) {
        UserDetails details = org.springframework.security.core.userdetails.User.withUsername(user.getEmail()).password(user.getPassword()).roles(user.getRole().name()).build();
        return new AuthResponse(jwtService.generateToken(details), "Bearer", jwtProperties.getExpirationMs() / 1000, userMapper.toResponse(user));
    }
}
