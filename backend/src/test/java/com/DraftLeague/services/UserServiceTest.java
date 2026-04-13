package com.DraftLeague.services;

import com.DraftLeague.dto.CreateUserRequest;
import com.DraftLeague.dto.UpdateUserRequest;
import com.DraftLeague.models.League.League;
import com.DraftLeague.models.Team.Team;
import com.DraftLeague.models.user.User;
import com.DraftLeague.repositories.LeagueRepository;
import com.DraftLeague.repositories.TeamRepository;
import com.DraftLeague.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @InjectMocks
    private UserService userService;


    @Test
    @DisplayName("postUser: petición válida → guarda usuario con rol 'USER'")
    void postUser_validRequest_savesUserWithRoleUser() {
        CreateUserRequest req = buildCreateRequest("alice", "alice@mail.com", "pass12345", "Alice");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.postUser(req);

        assertThat(result.getRole()).isEqualTo("USER");
        assertThat(result.getUsername()).isEqualTo("alice");
        verify(userRepository).save(any(User.class));
    }


    @Test
    @DisplayName("updateUser: solo displayName no nulo → actualiza únicamente ese campo")
    void updateUser_onlyDisplayName_updatesOnlyDisplayName() {
        User existing = buildUser(1, "alice", "alice@mail.com", "OldName");
        when(userRepository.findById(1)).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setDisplayName("NewName");

        User result = userService.updateUser(req, 1);

        assertThat(result.getDisplayName()).isEqualTo("NewName");
        assertThat(result.getEmail()).isEqualTo("alice@mail.com"); // sin cambios
    }

    @Test
    @DisplayName("updateUser: usuario no encontrado → lanza DataRetrievalFailureException")
    void updateUser_userNotFound_throwsException() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(new UpdateUserRequest(), 99))
                .isInstanceOf(DataRetrievalFailureException.class);
    }


    @Test
    @DisplayName("deleteUser: usuario con ligas y equipos → desvincula ligas y elimina equipos")
    void deleteUser_withLeaguesAndTeams_cascadesCorrectly() {
        User user = buildUser(1, "alice", "alice@mail.com", "Alice");
        League league = new League();
        league.setCreatedBy(user);

        Team team = new Team();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(leagueRepository.findByCreatedBy(user)).thenReturn(List.of(league));
        when(teamRepository.findByUser(user)).thenReturn(List.of(team));

        userService.deleteUser(1);

        assertThat(league.getCreatedBy()).isNull();
        verify(leagueRepository).saveAll(anyList());
        verify(teamRepository).deleteAll(anyList());
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("deleteUser: usuario no encontrado → lanza DataRetrievalFailureException")
    void deleteUser_userNotFound_throwsException() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99))
                .isInstanceOf(DataRetrievalFailureException.class);
    }


    @Test
    @DisplayName("getUserById: usuario existe → devuelve el usuario")
    void getUserById_existingUser_returnsUser() {
        User user = buildUser(1, "alice", "alice@mail.com", "Alice");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1);

        assertThat(result.getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("getUserById: usuario no existe → lanza DataRetrievalFailureException")
    void getUserById_notFound_throwsException() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99))
                .isInstanceOf(DataRetrievalFailureException.class);
    }


    @Test
    @DisplayName("getUserByUsername: usuario existe → devuelve el usuario")
    void getUserByUsername_existingUser_returnsUser() {
        User user = buildUser(1, "alice", "alice@mail.com", "Alice");
        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(user));

        User result = userService.getUserByUsername("alice");

        assertThat(result.getEmail()).isEqualTo("alice@mail.com");
    }


    @Test
    @DisplayName("getAllUsers: delega en el repositorio y devuelve la lista")
    void getAllUsers_returnsAllFromRepository() {
        List<User> users = List.of(buildUser(1, "a", "a@a.com", "A"), buildUser(2, "b", "b@b.com", "B"));
        when(userRepository.findAll()).thenReturn(users);

        Iterable<User> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
    }


    private CreateUserRequest buildCreateRequest(String username, String email, String password, String displayName) {
        CreateUserRequest r = new CreateUserRequest();
        r.setUsername(username);
        r.setEmail(email);
        r.setPassword(password);
        r.setDisplayName(displayName);
        return r;
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
}
