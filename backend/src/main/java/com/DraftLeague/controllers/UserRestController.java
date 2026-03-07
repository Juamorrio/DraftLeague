package com.DraftLeague.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import com.DraftLeague.models.user.User;
import com.DraftLeague.services.UserService;
import com.DraftLeague.services.LeagueService;

import java.util.List;


@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {

    private UserService userService;
    private LeagueService leagueService;

    @Autowired
    public UserRestController(UserService userService, LeagueService leagueService) {
        this.userService = userService;
        this.leagueService = leagueService;
    }

    @PostMapping()
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User createdUser = userService.postUser(user);
        return ResponseEntity.ok(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @Valid @RequestBody User user) {
        User updatedUser = userService.updateUser(user, id);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
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
