package com.apricorn.user.service;

import com.apricorn.user.dto.UserResponse;
import com.apricorn.user.entity.User;
import com.apricorn.user.mapper.UserMapper;
import com.apricorn.user.repository.UserRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return userMapper.toResponse(user);
    }
}
