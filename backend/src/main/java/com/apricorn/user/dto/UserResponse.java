package com.apricorn.user.dto;

import com.apricorn.user.entity.Role;
import java.time.Instant;

public record UserResponse(Long id, String username, String email, Role role, Instant createdAt) {
}
