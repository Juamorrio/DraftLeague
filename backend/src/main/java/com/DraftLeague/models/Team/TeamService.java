package com.DraftLeague.models.Team;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;

public class TeamService {

    private TeamRepository teamRepository;

    @Autowired
    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Transactional
    public Team postTeam(@Valid Team team) {
        this.teamRepository.save(team);
        return team;
    }

    @Transactional
    public Team updateTeam(@Valid Team team, Integer teamId) {
        Team existingTeam = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        existingTeam.setBudget(team.getBudget());
        existingTeam.setGameweekPoints(team.getGameweekPoints());
        existingTeam.setTotalPoints(team.getTotalPoints());
        existingTeam.setCaptainId(team.getCaptainId());
        return this.teamRepository.save(existingTeam);
    }

    @Transactional
    public void deleteTeam(Integer teamId) {
        Team teamDelete = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        this.teamRepository.delete(teamDelete);
    }

    @Transactional(readOnly = true)
    public Team getTeamById(Integer teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
    }

    @Transactional(readOnly = true)
    public Team getTeamByUserId(Integer userId) {
        return teamRepository.findAll().stream()
                .filter(team -> team.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Team not found for user"));
    }

    @Transactional(readOnly = true)
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    @Transactional
    public void resetGameweekPoints() {
        List<Team> teams = teamRepository.findAll();
        for (Team team : teams) {
            team.setGameweekPoints(0);
        }
        teamRepository.saveAll(teams);
    }

    
}
