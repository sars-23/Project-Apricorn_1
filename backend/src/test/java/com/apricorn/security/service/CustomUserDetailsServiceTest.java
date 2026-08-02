package com.apricorn.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.apricorn.user.entity.Role;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class CustomUserDetailsServiceTest {
    private UserRepository userRepository;
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    void loadsUserByUsernameSuccessfully() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);
        user.setEnabled(true);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("user@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("user@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void loadsAdminUserWithAdminRoleAuthority() {
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setPassword("adminPassword");
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin@example.com");

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void throwsUsernameNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }
}
