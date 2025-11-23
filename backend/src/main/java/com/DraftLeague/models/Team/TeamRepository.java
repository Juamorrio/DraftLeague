package com.DraftLeague.models.Team;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.DraftLeague.models.user.User;

@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {

    List<Team> findByUser(User user);
    
}
