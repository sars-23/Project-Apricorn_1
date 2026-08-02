package com.apricorn.auth.dto;

import com.apricorn.user.dto.UserResponse;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
