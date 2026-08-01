package com.apricorn.service;

import com.apricorn.dto.HealthResponse;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class HealthServiceImpl implements HealthService {
    @Override
    public HealthResponse getHealth() {
        return new HealthResponse("UP", Instant.now());
    }
}
