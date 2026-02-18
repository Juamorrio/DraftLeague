package com.DraftLeague.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerGameweekPointsDTO {
    private String playerId;
    private String playerName;
    private String position; // POR, DEF, MID, DEL
    private Integer gameweekPoints; // Puntos en esta jornada (con x2 si es capitán)
    private Integer matchId; // ID del partido donde jugó
    private Boolean played; // Si jugó o no
    private Boolean isCaptain; // Si es capitán
    private String avatarUrl; // URL del avatar del jugador
}
