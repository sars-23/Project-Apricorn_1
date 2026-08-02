package com.apricorn.user.service;

import com.apricorn.user.dto.UserResponse;

public interface UserService {
    UserResponse getCurrentUser(String email);
}
