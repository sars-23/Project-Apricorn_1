package com.apricorn.auth.service;

import com.apricorn.auth.dto.AuthResponse;
import com.apricorn.auth.dto.LoginRequest;
import com.apricorn.auth.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
