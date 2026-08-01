package com.apricorn.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.apricorn.dto.HealthResponse;
import com.apricorn.service.HealthService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

class HealthControllerTest {
    @Test
    void returnsServiceHealthResponse() {
        HealthResponse expected = new HealthResponse("UP", Instant.parse("2026-01-01T00:00:00Z"));
        HealthService healthService = Mockito.mock(HealthService.class);
        Mockito.when(healthService.getHealth()).thenReturn(expected);

        ResponseEntity<HealthResponse> response = new HealthController(healthService).health();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(expected);
    }
}
