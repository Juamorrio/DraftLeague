package com.DraftLeague.models.Player;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class PlayerImportController {

    private final PlayerImportService importService;

    public PlayerImportController(PlayerImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/import-players")
    public String importPlayers() {
        try {
            int count = importService.importFromJsonResource();
            return "Se importaron " + count + " jugadores.";
        } catch (Exception e) {
            return "Error al importar jugadores: " + e.getMessage();
        }
    }
}
