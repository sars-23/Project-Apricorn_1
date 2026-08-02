package com.apricorn.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {
    private final UserDetails user = User.withUsername("user@example.com").password("unused").roles("USER").build();
    @Test void generatesAndValidatesToken() {
        JwtProperties props = properties(60_000); JwtService service = new JwtServiceImpl(props);
        String token = service.generateToken(user);
        assertThat(service.extractSubject(token)).isEqualTo("user@example.com");
        assertThat(service.isTokenValid(token, user)).isTrue();
    }
    @Test void rejectsTamperedToken() {
        JwtService service = new JwtServiceImpl(properties(60_000));
        String token = service.generateToken(user);
        assertThatThrownBy(() -> service.extractSubject(token + "x")).isInstanceOf(JwtException.class);
    }
    private JwtProperties properties(long expiration) {
        JwtProperties props = new JwtProperties(); props.setSecret("01234567890123456789012345678901"); props.setExpirationMs(expiration); return props;
    }
}
