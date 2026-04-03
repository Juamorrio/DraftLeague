package com.DraftLeague.controllers;

import com.DraftLeague.dto.auth.AuthResponse;
import com.DraftLeague.dto.auth.LoginRequest;
import com.DraftLeague.dto.auth.RegisterRequest;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.auth.AuthService;
import com.DraftLeague.services.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller slice tests for AuthController.
 *
 * Uses addFilters=false to bypass the JWT filter chain (no token infrastructure needed)
 * and tests the controller's own logic via MockMvc.
 *
 * Required @MockBean set:
 *  - AuthService    — direct dependency of AuthController
 *  - UserRepository — direct dependency of AuthController + ApplicationConfig (userDetailsService)
 *  - JwtService     — required by JwtAuthenticationFilter to bootstrap the Spring context
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController — Web MVC slice tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;


    @Test
    @DisplayName("POST /auth/login — credenciales válidas → 200 con token")
    void login_validCredentials_returns200WithToken() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("jwt.token.here"));

        String body = objectMapper.writeValueAsString(new LoginRequest("alice", "password123"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"));
    }

    @Test
    @DisplayName("POST /auth/login — campos en blanco → 400 con VALIDATION_ERROR")
    void login_blankFields_returns400ValidationError() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest("", ""));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }


    @Test
    @DisplayName("POST /auth/register — request válida → 200 con token")
    void register_validRequest_returns200WithToken() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse("reg.jwt.token"));

        String body = objectMapper.writeValueAsString(
                new RegisterRequest("newuser", "password123", "new@mail.com", "NewUser"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("reg.jwt.token"));
    }

    @Test
    @DisplayName("POST /auth/register — servicio lanza RuntimeException → 500")
    void register_serviceThrowsRuntimeException_returns500() throws Exception {
        when(authService.register(any())).thenThrow(new RuntimeException("Error inesperado"));

        String body = objectMapper.writeValueAsString(
                new RegisterRequest("newuser", "password123", "new@mail.com", "NewUser"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError());
    }


    @Test
    @DisplayName("GET /auth/me — sin autenticación → 401")
    void me_noAuthentication_returns401() throws Exception {
        // With addFilters=false the anonymous filter does not run, so Authentication
        // is null. The controller checks for null and returns 401 itself.
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /auth/me — usuario autenticado → 200 con datos del usuario")
    void me_authenticatedUser_returns200WithUserData() throws Exception {
        User user = buildUser(1, "alice", "alice@mail.com", "Alice", "USER");
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("alice", null, Collections.emptyList());

        mockMvc.perform(get("/auth/me")
                        .with(request -> { request.setUserPrincipal(auth); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@mail.com"));
    }


    private User buildUser(int id, String username, String email, String displayName, String role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword("$2a$encoded");
        u.setDisplayName(displayName);
        u.setRole(role);
        return u;
    }
}
