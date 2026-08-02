package com.apricorn.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtProperties properties;
    public JwtService(JwtProperties properties) { this.properties = properties; }

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        return Jwts.builder().subject(userDetails.getUsername()).issuedAt(now)
                .expiration(new Date(now.getTime() + properties.getExpirationMs()))
                .signWith(signingKey()).compact();
    }
    public String extractSubject(String token) { return claims(token).getSubject(); }
    public Date extractExpiration(String token) { return claims(token).getExpiration(); }
    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractSubject(token).equals(userDetails.getUsername()) && extractExpiration(token).after(new Date());
    }
    private Claims claims(String token) { return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload(); }
    private SecretKey signingKey() { return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8)); }
}
