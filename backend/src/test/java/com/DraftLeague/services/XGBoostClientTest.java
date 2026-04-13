package com.DraftLeague.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XGBoostClient Unit Tests")
class XGBoostClientTest {

    private XGBoostClient client;

    @BeforeEach
    void setUp() {
        client = new XGBoostClient();
        // Puerto 1 nunca está en escucha → conexión rechazada de forma inmediata
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:1");
        ReflectionTestUtils.setField(client, "internalKey", "test-key");
        ReflectionTestUtils.setField(client, "timeoutSeconds", 1);
    }


    @Test
    @DisplayName("isHealthy: servidor inalcanzable → devuelve false")
    void isHealthy_unreachableServer_returnsFalse() {
        boolean result = client.isHealthy();

        assertThat(result).isFalse();
    }


    @Test
    @DisplayName("predict: servidor inalcanzable → devuelve Optional vacío")
    void predict_unreachableServer_returnsEmpty() {
        Map<String, Object> features = Map.of("rating", 7.5, "minutesPlayed", 90);

        Optional<?> result = client.predict("P1", features);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("predict: mapa de features vacío + servidor inalcanzable → devuelve Optional vacío")
    void predict_emptyFeatures_returnsEmpty() {
        Optional<?> result = client.predict("P2", Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("predict: playerId con caracteres especiales → devuelve Optional vacío (no lanza excepción)")
    void predict_playerIdWithSpecialChars_returnsEmptyNotThrow() {
        Optional<?> result = client.predict("player/with/slashes", Map.of("rating", 7.5));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("predict: playerId nulo → devuelve Optional vacío (no lanza excepción)")
    void predict_nullPlayerId_returnsEmpty() {
        Optional<?> result = client.predict(null, Map.of("rating", 7.0));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("predict: features con múltiples entradas → devuelve Optional vacío (servidor inaccesible)")
    void predict_largeFeatureMap_returnsEmpty() {
        Map<String, Object> features = Map.of(
                "rating", 7.5,
                "minutes_played", 90,
                "goals", 1,
                "assists", 0,
                "shots_on_target", 2,
                "tackles", 3,
                "blocks", 1,
                "saves", 0,
                "goals_conceded", 0,
                "clean_sheet", 1
        );

        Optional<?> result = client.predict("P3", features);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("triggerRetraining: servidor inalcanzable → no lanza excepción (fire-and-forget)")
    void triggerRetraining_unreachableServer_doesNotThrow() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> client.triggerRetraining(5));
    }

    @Test
    @DisplayName("triggerRetraining: gameweek 0 → no lanza excepción")
    void triggerRetraining_zeroGameweek_doesNotThrow() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> client.triggerRetraining(0));
    }

    @Test
    @DisplayName("triggerRetraining: gameweek negativo → no lanza excepción")
    void triggerRetraining_negativeGameweek_doesNotThrow() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> client.triggerRetraining(-1));
    }

    @Test
    @DisplayName("isHealthy: segunda llamada → sigue devolviendo false (no estado interno roto)")
    void isHealthy_calledTwice_alwaysReturnsFalse() {
        assertThat(client.isHealthy()).isFalse();
        assertThat(client.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("predict: llamadas múltiples → cada una devuelve Optional vacío de forma independiente")
    void predict_multipleCalls_eachReturnsEmpty() {
        for (int i = 0; i < 3; i++) {
            Optional<?> result = client.predict("P" + i, Map.of("rating", (double) i));
            assertThat(result).isEmpty();
        }
    }

    @Test
    @DisplayName("predict: features null → devuelve Optional vacío (no lanza NullPointerException)")
    void predict_nullFeatures_returnsEmpty() {
        Optional<?> result = client.predict("P1", null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("triggerRetraining: gameweek grande → no lanza excepción")
    void triggerRetraining_largeGameweek_doesNotThrow() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> client.triggerRetraining(38));
    }

    @Test
    @DisplayName("predict: playerId vacío → devuelve Optional vacío (servidor inaccesible)")
    void predict_emptyPlayerId_returnsEmpty() {
        Optional<?> result = client.predict("", Map.of("rating", 7.0));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("isHealthy: URL inaccesible con puerto 1 → false (no InterruptedException)")
    void isHealthy_port1_returnsFalseWithoutInterruption() {
        // Verify thread is not interrupted after the call
        boolean result = client.isHealthy();

        assertThat(result).isFalse();
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    @Test
    @DisplayName("predict: features con valores Double y Integer mezclados → devuelve Optional vacío")
    void predict_mixedNumericFeatures_returnsEmpty() {
        Map<String, Object> features = new java.util.LinkedHashMap<>();
        features.put("rating", 7.5);
        features.put("minutes_played", 90);
        features.put("goals", 1);
        features.put("is_home_team", 1);
        features.put("clean_sheet", 0);
        features.put("opponent_strength", 1.05);

        Optional<?> result = client.predict("PLAYER_X", features);

        assertThat(result).isEmpty();
    }
}
