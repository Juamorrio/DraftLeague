package com.DraftLeague.models.user;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;

@Service
public class UserService {

    private UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
	public User postUser(@Valid User user) throws DataAccessException {
		this.userRepository.save(user);
		return user;
	}

    @Transactional
    public User updateUser(@Valid User user, Integer userId) throws DataAccessException {
        Optional<User> existingUser = userRepository.findById(userId);

        if (existingUser.isPresent()) {
            User userToUpdate = existingUser.get();
            userToUpdate.setDisplayName(user.getDisplayName());
            userToUpdate.setPassword(user.getPassword());
            userToUpdate.setUsername(user.getUsername());
            userToUpdate.setEmail(user.getEmail());
            return this.userRepository.save(userToUpdate);
        } else {
            throw new DataRetrievalFailureException("User not found");
        }
    }

    @Transactional
	public void deleteUser(Integer userId) throws DataAccessException {
		User userDelete = userRepository.findById(userId)
                .orElseThrow(() -> new DataRetrievalFailureException("User not found"));
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
