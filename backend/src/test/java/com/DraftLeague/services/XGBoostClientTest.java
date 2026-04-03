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
}
