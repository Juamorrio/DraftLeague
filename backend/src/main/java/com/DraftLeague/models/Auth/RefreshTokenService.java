package com.DraftLeague.models.Auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.DraftLeague.models.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static class Pair {
        public final String raw;
        public final RefreshToken entity;
        public Pair(String raw, RefreshToken entity) { this.raw = raw; this.entity = entity; }
    }

    public Pair issue(User user) {
        String raw = generateRaw();
        String hash = sha256Hex(raw);
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(hash);
        rt.setCreatedAt(Instant.now());
        rt.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        rt.setRevoked(false);
        repository.save(rt);
        return new Pair(raw, rt);
    }

    public void revoke(Long id) {
        repository.findById(id).ifPresent(rt -> { rt.setRevoked(true); repository.save(rt); });
    }

    public void revokeRaw(String raw) {
        String hash = sha256Hex(raw);
        repository.findByTokenHash(hash).ifPresent(rt -> { rt.setRevoked(true); repository.save(rt); });
    }

    static String generateRaw() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
