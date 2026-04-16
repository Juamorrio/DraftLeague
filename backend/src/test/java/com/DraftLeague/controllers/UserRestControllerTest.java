package com.DraftLeague.controllers;

import com.DraftLeague.dto.CreateUserRequest;
import com.DraftLeague.dto.UpdateUserRequest;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.LeagueService;
import com.DraftLeague.services.UserService;
import com.DraftLeague.services.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserRestController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserRestController — Web MVC slice tests")
class UserRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @MockBean
    private LeagueService leagueService;


    @Test
    @DisplayName("POST /api/v1/users — petición válida → 200 con usuario creado")
    void createUser_validRequest_returns200() throws Exception {
        User user = buildUser(1, "alice", "alice@mail.com", "Alice");
        when(userService.postUser(any())).thenReturn(user);

        String body = objectMapper.writeValueAsString(buildCreateRequest());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }


    @Test
    @DisplayName("PUT /api/v1/users/1 — self → 200 con usuario actualizado")
    @WithMockUser(username = "alice", roles = "USER")
    void updateUser_self_returns200() throws Exception {
        User user = buildUser(1, "alice", "alice@mail.com", "NewName");
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));
        when(userService.updateUser(any(), eq(1))).thenReturn(user);

        UpdateUserRequest req = new UpdateUserRequest();
        req.setDisplayName("NewName");

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("NewName"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/2 — usuario distinto → 403 Forbidden")
    @WithMockUser(username = "alice", roles = "USER")
    void updateUser_otherUser_returns403() throws Exception {
        User alice = buildUser(1, "alice", "alice@mail.com", "Alice");
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(alice));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setDisplayName("Hacked");

        mockMvc.perform(put("/api/v1/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("DELETE /api/v1/users/1 — self → 204")
    @WithMockUser(username = "alice", roles = "USER")
    void deleteUser_self_returns204() throws Exception {
        User user = buildUser(1, "alice", "alice@mail.com", "Alice");
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));
        doNothing().when(userService).deleteUser(1);

        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/2 — usuario distinto → 403 Forbidden")
    @WithMockUser(username = "alice", roles = "USER")
    void deleteUser_otherUser_returns403() throws Exception {
        User alice = buildUser(1, "alice", "alice@mail.com", "Alice");
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(alice));

        mockMvc.perform(delete("/api/v1/users/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/7 — ADMIN → 204")
    @WithMockUser(username = "root", roles = "ADMIN")
    void deleteUser_admin_returns204() throws Exception {
        doNothing().when(userService).deleteUser(7);

        mockMvc.perform(delete("/api/v1/users/7"))
                .andExpect(status().isNoContent());
    }


    @Test
    @DisplayName("GET /api/v1/users/1 → 200 con usuario")
    void getUserById_returns200() throws Exception {
        User user = buildUser(1, "alice", "alice@mail.com", "Alice");
        when(userService.getUserById(1)).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }


    @Test
    @DisplayName("GET /api/v1/users/username/alice → 200 con usuario")
    void getUserByUsername_returns200() throws Exception {
        User user = buildUser(1, "alice", "alice@mail.com", "Alice");
        when(userService.getUserByUsername("alice")).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/username/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@mail.com"));
    }


    @Test
    @DisplayName("GET /api/v1/users → 200 con lista de usuarios")
    void getAllUsers_returns200() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(buildUser(1, "a", "a@a.com", "A")));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("GET /api/v1/users/1/leagues → 200 con ligas del usuario")
    void getLeaguesByUserId_returns200() throws Exception {
        when(leagueService.getLeaguesByUserId(1))
                .thenReturn(List.of(Map.of("id", 1, "name", "Liga1")));

        mockMvc.perform(get("/api/v1/users/1/leagues"))
                .andExpect(status().isOk());
    }


    private User buildUser(int id, String username, String email, String displayName) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(email);
        u.setDisplayName(displayName);
        u.setPassword("$2a$encoded");
        u.setRole("USER");
        return u;
    }

    private CreateUserRequest buildCreateRequest() {
        CreateUserRequest r = new CreateUserRequest();
        r.setUsername("alice");
        r.setEmail("alice@mail.com");
        r.setPassword("password123");
        r.setDisplayName("Alice");
        return r;
    }
}
