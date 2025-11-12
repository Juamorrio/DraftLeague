package com.DraftLeague.models.Auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.DraftLeague.models.user.User;
import com.DraftLeague.models.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        User user = userRepository.findUserByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        RefreshTokenService.Pair pair = refreshTokenService.issue(user);
        return AuthResponse.builder()
            .token(jwtService.getToken(user))
            .refreshToken(pair.raw)
            .build();
    }

    public AuthResponse register(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName());
        
        userRepository.save(user);
        RefreshTokenService.Pair pair = refreshTokenService.issue(user);
        return AuthResponse.builder()
            .token(jwtService.getToken(user))
            .refreshToken(pair.raw)
            .build();
    }

}
