package com.DraftLeague.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.DraftLeague.dto.CreateUserRequest;
import com.DraftLeague.dto.UpdateUserRequest;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.repositories.TeamRepository;
import com.DraftLeague.models.League.League;
import com.DraftLeague.repositories.LeagueRepository;
import com.DraftLeague.models.user.User;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.League.League;
import com.DraftLeague.repositories.UserRepository;
import com.DraftLeague.repositories.TeamRepository;
import com.DraftLeague.repositories.LeagueRepository;
import com.DraftLeague.services.UserService;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;

    @Autowired
    public UserService(UserRepository userRepository, TeamRepository teamRepository, LeagueRepository leagueRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.leagueRepository = leagueRepository;
    }

    @Transactional
    public User postUser(CreateUserRequest request) throws DataAccessException {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setDisplayName(request.getDisplayName());
        user.setRole("USER");
        this.userRepository.save(user);
        return user;
    }

    @Transactional
    public User updateUser(UpdateUserRequest request, Integer userId) throws DataAccessException {
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new DataRetrievalFailureException("User not found"));
        if (request.getDisplayName() != null) userToUpdate.setDisplayName(request.getDisplayName());
        if (request.getPassword() != null)    userToUpdate.setPassword(request.getPassword());
        if (request.getUsername() != null)    userToUpdate.setUsername(request.getUsername());
        if (request.getEmail() != null)       userToUpdate.setEmail(request.getEmail());
        return this.userRepository.save(userToUpdate);
    }

    @Transactional
	public void deleteUser(Integer userId) throws DataAccessException {
		User userDelete = userRepository.findById(userId)
                .orElseThrow(() -> new DataRetrievalFailureException("User not found"));

        // 1) Detach leagues created by this user to avoid FK issues
        List<League> created = leagueRepository.findByCreatedBy(userDelete);
        if (created != null && !created.isEmpty()) {
            for (League l : created) {
                l.setCreatedBy(null);
            }
            leagueRepository.saveAll(created);
        }

        // 2) Delete user's teams (cascades PlayerTeam via Team mapping)
        List<Team> teams = teamRepository.findByUser(userDelete);
        if (teams != null && !teams.isEmpty()) {
            teamRepository.deleteAll(teams);
            teamRepository.flush();
        }

		this.userRepository.delete(userDelete);
	}

    @Transactional(readOnly = true)
    public User getUserById(Integer userId) throws DataAccessException {
        return this.userRepository.findById(userId)
                .orElseThrow(() -> new DataRetrievalFailureException("User not found"));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) throws DataAccessException {
        return this.userRepository.findUserByUsername(username)
                .orElseThrow(() -> new DataRetrievalFailureException("User not found"));
    }

    @Transactional(readOnly = true)
    public Iterable<User> getAllUsers() throws DataAccessException {
        return this.userRepository.findAll();
    }

}
