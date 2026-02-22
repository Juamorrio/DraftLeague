package com.DraftLeague.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.DraftLeague.models.Player.Position;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerGameweekPointsDTO {
    private String playerId;
    private String playerName;
    private String position; // POR, DEF, MID, DEL
    private Integer gameweekPoints; // Puntos en esta jornada (con x2 si es capitÃƒÆ’Ã‚Â¡n)
    private Integer matchId; // ID del partido donde jugÃƒÆ’Ã‚Â³
    private Boolean played; // Si jugÃƒÆ’Ã‚Â³ o no
    private Boolean isCaptain; // Si es capitÃƒÆ’Ã‚Â¡n
    private String avatarUrl; // URL del avatar del jugador
}
