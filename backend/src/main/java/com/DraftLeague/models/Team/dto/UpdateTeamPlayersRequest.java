package com.DraftLeague.models.Team.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateTeamPlayersRequest {
    
    @NotNull
    private List<PlayerSelection> players;
    
    @Getter
    @Setter
    public static class PlayerSelection {
        @NotNull
        private String playerId;
        
        @NotNull
        private String position;
        
        @NotNull
        private Boolean lined;
        
        private Boolean isCaptain = false;
    }
}
