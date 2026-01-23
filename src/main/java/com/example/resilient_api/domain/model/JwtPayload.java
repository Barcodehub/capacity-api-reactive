package com.example.resilient_api.domain.model;

public record JwtPayload(Long userId, String email, Boolean isAdmin) {
}
