package com.DraftLeague.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import com.DraftLeague.dto.CreateUserRequest;
import com.DraftLeague.dto.UpdateUserRequest;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.services.UserService;
import com.DraftLeague.services.LeagueService;

import java.util.List;


@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {

    private final UserService userService;
    private final LeagueService leagueService;
    private final UserRepository userRepository;

    @Autowired
    public UserRestController(UserService userService,
                              LeagueService leagueService,
                              UserRepository userRepository) {
        this.userService = userService;
        this.leagueService = leagueService;
        this.userRepository = userRepository;
    }

    /**
     * Returns true if the given authenticated caller is either the ADMIN role
     * or the owner of the target user id.
     */
    private boolean isSelfOrAdmin(Authentication auth, Integer targetUserId) {
        if (auth == null || !auth.isAuthenticated() || targetUserId == null) {
            return false;
        }
        boolean isAdmin = auth.getAuthorities() != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return true;
        }
        return userRepository.findUserByUsername(auth.getName())
                .map(u -> targetUserId.equals(u.getId()))
                .orElse(false);
    }

    @PostMapping()
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        // Note: self-service user creation should go through /auth/register;
        // this admin-style endpoint now requires ADMIN via the security filter chain
        // (/api/v1/users/** falls under `.anyRequest().authenticated()` + the ownership
        // checks below for mutation). Creating arbitrary users is not self-service.
        User createdUser = userService.postUser(request);
        return ResponseEntity.ok(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id,
                                       @Valid @RequestBody UpdateUserRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isSelfOrAdmin(auth, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        User updatedUser = userService.updateUser(request, id);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isSelfOrAdmin(auth, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<Iterable<User>> getAllUsers() {
        Iterable<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}/leagues")
    public ResponseEntity<List<java.util.Map<String,Object>>> getLeaguesByUserId(@PathVariable Integer id) {
        List<java.util.Map<String,Object>> leagues = leagueService.getLeaguesByUserId(id);
        return ResponseEntity.ok(leagues);
    }

}
