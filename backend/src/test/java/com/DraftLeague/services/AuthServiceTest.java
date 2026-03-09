package com.DraftLeague.services;

import com.DraftLeague.dto.auth.AuthResponse;
import com.DraftLeague.dto.auth.LoginRequest;
import com.DraftLeague.dto.auth.RegisterRequest;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.auth.AuthService;
import com.DraftLeague.services.auth.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // ─── login ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: credenciales válidas → devuelve token JWT")
    void login_validCredentials_returnsToken() {
        LoginRequest req = new LoginRequest("alice", "password123");
        User user = buildUser(1, "alice", "alice@mail.com");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.getToken(user)).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: contraseña incorrecta → propaga excepción del AuthenticationManager")
    void login_wrongPassword_throwsException() {
        LoginRequest req = new LoginRequest("alice", "wrongpass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login: usuario no encontrado → RuntimeException")
    void login_userNotFound_throwsRuntimeException() {
        LoginRequest req = new LoginRequest("ghost", "password123");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findUserByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }

    // ─── register ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: request válida → guarda usuario y devuelve token")
    void register_validRequest_savesUserAndReturnsToken() {
        RegisterRequest req = new RegisterRequest("bob", "password123", "bob@mail.com", "Bob");

        when(userRepository.findUserByUsername("bob")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("bob@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.getToken(any(User.class))).thenReturn("reg.jwt.token");

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("reg.jwt.token");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("register: nombre de usuario duplicado → IllegalStateException")
    void register_duplicateUsername_throwsIllegalStateException() {
        RegisterRequest req = new RegisterRequest("alice", "password123", "new@mail.com", "Alice2");

        when(userRepository.findUserByUsername("alice"))
                .thenReturn(Optional.of(buildUser(1, "alice", "alice@mail.com")));

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("usuario ya está en uso");
    }

    @Test
    @DisplayName("register: email duplicado → IllegalStateException")
    void register_duplicateEmail_throwsIllegalStateException() {
        RegisterRequest req = new RegisterRequest("newuser", "password123", "alice@mail.com", "New");

        when(userRepository.findUserByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("alice@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("email ya está registrado");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────────

    private User buildUser(int id, String username, String email) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword("$2a$encoded");
        u.setDisplayName(username);
        u.setRole("USER");
        return u;
    }
}
