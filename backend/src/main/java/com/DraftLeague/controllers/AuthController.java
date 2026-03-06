package com.DraftLeague.controllers;

import com.DraftLeague.dto.auth.AuthResponse;
import com.DraftLeague.dto.auth.LoginRequest;
import com.DraftLeague.dto.auth.RegisterRequest;
import com.DraftLeague.services.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.Map;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.models.user.User;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
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
            "email", user.getEmail(),
            "role", user.getRole() != null ? user.getRole() : "USER"
        );
        return ResponseEntity.ok(body);
    }

  
}
