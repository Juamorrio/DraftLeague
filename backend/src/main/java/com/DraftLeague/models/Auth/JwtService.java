package com.DraftLeague.models.Auth;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;
import java.util.function.Function;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.DraftLeague.models.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Value;

@Service
public class JwtService {

    @Value("${jwt.secret:}")
    private String jwtSecretProperty;

    private static final String DEFAULT_DEV_SECRET = "dev-secret-key-change-me-32-bytes-minimum-2025";

    public String getToken(User user) {
        HashMap<String,Object> claims = new HashMap<>();
        // Claims básicos para lectura rápida en frontend
        claims.put("uid", user.getId());
        claims.put("displayName", user.getDisplayName());
        claims.put("email", user.getEmail());
        return getToken(claims, user);
    }

    public String getToken(HashMap<String, Object> claims, User user) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) 
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();

    }

    private Key getKey() {
        String secret = resolveSecret();
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                keyBytes = sha256.digest(keyBytes);
            } catch (NoSuchAlgorithmException e) {
                keyBytes = Arrays.copyOf(keyBytes, 32);
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String resolveSecret() {
        if (jwtSecretProperty != null && !jwtSecretProperty.isBlank()) {
            return jwtSecretProperty;
        }
        String env = System.getenv("JWT_SECRET");
        if (env != null && !env.isBlank()) {
            return env;
        }
        return DEFAULT_DEV_SECRET;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    
}
