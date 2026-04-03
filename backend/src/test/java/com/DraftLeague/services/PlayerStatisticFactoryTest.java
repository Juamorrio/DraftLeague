package com.DraftLeague.services;

import com.DraftLeague.models.Statistics.PlayerStatistic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PlayerStatisticFactory Unit Tests")
class PlayerStatisticFactoryTest {

    private final PlayerStatisticFactory factory = new PlayerStatisticFactory();


    @Test
    @DisplayName("createStatistic: 'GOALKEEPER' → tipo GOALKEEPER")
    void createStatistic_goalkeeper_returnsGoalkeeperType() {
        PlayerStatistic stat = factory.createStatistic("GOALKEEPER");

        assertThat(stat).isNotNull();
        assertThat(stat.getPlayerType()).isEqualTo(PlayerStatistic.PlayerType.GOALKEEPER);
    }

    @Test
    @DisplayName("createStatistic: 'DEFENDER' → tipo DEFENDER")
    void createStatistic_defender_returnsDefenderType() {
        PlayerStatistic stat = factory.createStatistic("DEFENDER");

        assertThat(stat.getPlayerType()).isEqualTo(PlayerStatistic.PlayerType.DEFENDER);
    }

    @Test
    @DisplayName("createStatistic: 'MIDFIELDER' → tipo MIDFIELDER")
    void createStatistic_midfielder_returnsMidfielderType() {
        PlayerStatistic stat = factory.createStatistic("MIDFIELDER");

        assertThat(stat.getPlayerType()).isEqualTo(PlayerStatistic.PlayerType.MIDFIELDER);
    }

    @Test
    @DisplayName("createStatistic: 'FORWARD' → tipo FORWARD")
    void createStatistic_forward_returnsForwardType() {
        PlayerStatistic stat = factory.createStatistic("FORWARD");

        assertThat(stat.getPlayerType()).isEqualTo(PlayerStatistic.PlayerType.FORWARD);
    }

    @Test
    @DisplayName("createStatistic: minúsculas 'goalkeeper' → tipo GOALKEEPER (case-insensitive)")
    void createStatistic_lowercase_isCaseInsensitive() {
        PlayerStatistic stat = factory.createStatistic("goalkeeper");

        assertThat(stat.getPlayerType()).isEqualTo(PlayerStatistic.PlayerType.GOALKEEPER);
    }

    @Test
    @DisplayName("createStatistic: null (String) → lanza IllegalArgumentException")
    void createStatistic_nullString_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> factory.createStatistic((String) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }


    @Test
    @DisplayName("createStatistic(PlayerType.MIDFIELDER) → tipo MIDFIELDER")
    void createStatistic_enumMidfielder_returnsMidfielderType() {
        PlayerStatistic stat = factory.createStatistic(PlayerStatistic.PlayerType.MIDFIELDER);

        assertThat(stat.getPlayerType()).isEqualTo(PlayerStatistic.PlayerType.MIDFIELDER);
    }

    @Test
    @DisplayName("createStatistic: null (PlayerType) → lanza IllegalArgumentException")
    void createStatistic_nullEnum_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> factory.createStatistic((PlayerStatistic.PlayerType) null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
