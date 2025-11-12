package com.DraftLeague.models.Auth.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenView {
    private Long id;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean revoked;
    private String fingerprint; // últimos 6 chars del hash
}
