package com.apricorn.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.apricorn.auth.dto.LoginRequest;
import com.apricorn.auth.dto.RegisterRequest;
import com.apricorn.exception.ConflictException;
import com.apricorn.security.jwt.JwtProperties;
import com.apricorn.security.jwt.JwtService;
import com.apricorn.user.entity.Role;
import com.apricorn.user.entity.User;
import com.apricorn.user.mapper.UserMapper;
import com.apricorn.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {
    private UserRepository users;
    private AuthService service;
    @BeforeEach void setUp() {
        users = mock(UserRepository.class);
        JwtProperties properties = new JwtProperties(); properties.setSecret("01234567890123456789012345678901"); properties.setExpirationMs(3600000);
        service = new AuthService(users, new BCryptPasswordEncoder(), mock(AuthenticationManager.class), new JwtService(properties), properties, new UserMapper());
    }
    @Test void registersUserWithBcryptHash() {
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.register(new RegisterRequest("apricorn", "user@example.com", "Secure123!"));
        var saved = org.mockito.ArgumentCaptor.forClass(User.class); verify(users).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(saved.getValue().getPassword()).startsWith("$2").isNotEqualTo("Secure123!");
    }
    @Test void rejectsDuplicateEmail() {
        when(users.existsByEmail("user@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest("apricorn", "user@example.com", "Secure123!"))).isInstanceOf(ConflictException.class);
    }
    @Test void rejectsDuplicateUsername() {
        when(users.existsByUsername("apricorn")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest("apricorn", "user@example.com", "Secure123!"))).isInstanceOf(ConflictException.class);
    }
    @Test void authenticatesLoginThroughAuthenticationManager() {
        User user = new User(); user.setEmail("user@example.com"); user.setUsername("apricorn"); user.setPassword("hash"); user.setRole(Role.USER);
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        assertThat(service.login(new LoginRequest("user@example.com", "Secure123!"))).isNotNull();
    }
}
