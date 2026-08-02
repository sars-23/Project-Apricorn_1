package com.apricorn.security.jwt;

import java.util.Date;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String generateToken(UserDetails userDetails);
    String extractSubject(String token);
    Date extractExpiration(String token);
    boolean isTokenValid(String token, UserDetails userDetails);
}
