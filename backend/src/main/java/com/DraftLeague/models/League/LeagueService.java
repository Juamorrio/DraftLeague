package com.DraftLeague.models.League;

import org.springframework.stereotype.Service;

@Service
public class LeagueService {

    private final LeagueRepository leagueRepository;

    public LeagueService(LeagueRepository leagueRepository) {
        this.leagueRepository = leagueRepository;
    }

    public League createLeague(League league) {
        return leagueRepository.save(league);
    }

    public League getLeagueById(Long id) {
        return leagueRepository.findById(id).orElseThrow(() -> new RuntimeException("League not found"));
    }

    public League updateLeague(Long id, League league) {
        League existingLeague = getLeagueById(id);
        existingLeague.setName(league.getName());
        existingLeague.setDescription(league.getDescription());
        return leagueRepository.save(existingLeague);
    }

    public void deleteLeague(Long id) {
        League league = getLeagueById(id);
        leagueRepository.delete(league);
    }




}
