package com.DraftLeague.models.Auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.Map;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;
import com.DraftLeague.models.Auth.dto.TokenView;
import com.DraftLeague.models.Auth.RefreshTokenRepository;
import com.DraftLeague.models.Auth.RefreshTokenService;
import com.DraftLeague.models.Auth.JwtService;
import com.DraftLeague.models.user.UserRepository;
import com.DraftLeague.models.user.User;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        var pair = refreshTokenService.rotate(request.getRefreshToken());
        var user = pair.entity.getUser();
        return ResponseEntity.ok(AuthResponse.builder()
            .token(jwtService.getToken(user))
            .refreshToken(pair.raw)
            .build());
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String,Object>> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        User user = userRepository.findUserByUsername(username)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Map<String,Object> body = Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "displayName", user.getDisplayName(),
            "email", user.getEmail()
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping("/tokens/{userId}")
    public ResponseEntity<List<TokenView>> listTokens(@PathVariable Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        List<TokenView> tokens = refreshTokenRepository.findAllByUserId(user.getId()).stream()
                .map(rt -> new TokenView(rt.getId(), rt.getCreatedAt(), rt.getExpiresAt(), rt.getRevoked(), tail(rt.getTokenHash())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(tokens);
    }

    @DeleteMapping("/tokens/{tokenId}")
    public ResponseEntity<Void> revokeToken(@PathVariable Long tokenId) {
        refreshTokenRepository.findById(tokenId).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
        return ResponseEntity.noContent().build();
    }

    private String tail(String hash) {
        if (hash == null || hash.length() < 6) return hash;
        return hash.substring(hash.length() - 6);
    }
}