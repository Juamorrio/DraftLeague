package com.DraftLeague.dto;

import com.DraftLeague.dto.PlayerMatchSummaryDTO;
import java.util.List;

public class JornadaMatchesDTO {
    private Integer jornada;
    private List<PlayerMatchSummaryDTO> matches;

    public JornadaMatchesDTO() {}

    public JornadaMatchesDTO(Integer jornada, List<PlayerMatchSummaryDTO> matches) {
        this.jornada = jornada;
        this.matches = matches;
    }

    public Integer getJornada() {
        return jornada;
    }

    public void setJornada(Integer jornada) {
        this.jornada = jornada;
    }

    public List<PlayerMatchSummaryDTO> getMatches() {
        return matches;
    }

    public void setMatches(List<PlayerMatchSummaryDTO> matches) {
        this.matches = matches;
    }
}
