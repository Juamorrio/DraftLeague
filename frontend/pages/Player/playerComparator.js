import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  TextInput,
  useWindowDimensions,
  Platform,
} from 'react-native';
import { LineChart } from 'react-native-chart-kit';
import { useLeague } from '../../context/LeagueContext';
import { authenticatedFetch } from '../../services/authService';
import withAuth from '../../components/withAuth';
import { colors, fontSize, fontWeight, fontFamily, radius, spacing, shadow, positionBadgeColors } from '../../utils/theme';

const webSafeProps = Platform.OS === 'web' ? {
  onStartShouldSetResponder: undefined,
  onResponderGrant: undefined,
  onResponderMove: undefined,
  onResponderRelease: undefined,
  onResponderTerminate: undefined,
  onResponderTerminationRequest: undefined,
} : {};

function StatRow({ label, valueA, valueB, higherIsBetter = true }) {
  const a = parseFloat(valueA) || 0;
  const b = parseFloat(valueB) || 0;
  const aWins = higherIsBetter ? a > b : a < b;
  const bWins = higherIsBetter ? b > a : b < a;

  return (
    <View style={rowStyles.row}>
      <Text style={[rowStyles.value, aWins && rowStyles.valueWinner]}>{valueA ?? '—'}</Text>
      <Text style={rowStyles.label}>{label}</Text>
      <Text style={[rowStyles.value, rowStyles.valueRight, bWins && rowStyles.valueWinner]}>{valueB ?? '—'}</Text>
    </View>
  );
}

const rowStyles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  label: {
    flex: 1,
    textAlign: 'center',
    fontSize: fontSize.xs,
    color: colors.textMuted,
    fontWeight: fontWeight.semibold,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  value: {
    width: 72,
    textAlign: 'left',
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.textSecondary,
  },
  valueRight: {
    textAlign: 'right',
  },
  valueWinner: {
    color: colors.primary,
    fontSize: fontSize.md,
  },
});

function PlayerCard({ player, summary, color }) {
  if (!player) {
    return (
      <View style={[cardStyles.card, { borderColor: colors.border }]}>
        <Text style={cardStyles.placeholder}>Sin jugador</Text>
      </View>
    );
  }
  const posColor = positionBadgeColors[player.position] ?? positionBadgeColors.MID;
  return (
    <View style={[cardStyles.card, { borderColor: color }]}>
      <View style={[cardStyles.colorBar, { backgroundColor: color }]} />
      <View style={[cardStyles.posBadge, { backgroundColor: posColor.bg, borderColor: posColor.border }]}>
        <Text style={[cardStyles.posText, { color: posColor.text }]}>{player.position}</Text>
      </View>
      <Text style={cardStyles.name} numberOfLines={2}>{player.fullName ?? player.name}</Text>
      {player.team?.name && <Text style={cardStyles.team}>{player.team.name}</Text>}
      <Text style={cardStyles.value}>€{((player.marketValue ?? 0) / 1_000_000).toFixed(2)}M</Text>
      {summary && (
        <View style={cardStyles.pts}>
          <Text style={[cardStyles.ptsNum, { color }]}>{summary.totalFantasyPoints ?? player.totalPoints ?? 0}</Text>
          <Text style={cardStyles.ptsLabel}>pts</Text>
        </View>
      )}
    </View>
  );
}

const cardStyles = StyleSheet.create({
  card: {
    flex: 1,
    backgroundColor: colors.bgCard,
    borderRadius: radius.lg,
    borderWidth: 2,
    padding: spacing.md,
    alignItems: 'center',
    gap: 4,
    ...shadow.sm,
    minHeight: 130,
  },
  colorBar: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 4,
    borderTopLeftRadius: radius.lg - 2,
    borderTopRightRadius: radius.lg - 2,
  },
  placeholder: {
    color: colors.textMuted,
    fontSize: fontSize.sm,
    fontStyle: 'italic',
    marginTop: spacing['3xl'],
  },
  posBadge: {
    borderRadius: radius.pill,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
    borderWidth: 1,
    marginTop: spacing.sm,
  },
  posText: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.bold,
  },
  name: {
    fontFamily: fontFamily.displaySemi,
    fontSize: fontSize.sm,
    color: colors.textPrimary,
    textAlign: 'center',
    marginTop: 2,
  },
  team: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
  },
  value: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
    fontWeight: fontWeight.semibold,
  },
  pts: {
    flexDirection: 'row',
    alignItems: 'baseline',
    gap: 2,
    marginTop: 4,
  },
  ptsNum: {
    fontFamily: fontFamily.displayBold,
    fontSize: fontSize['2xl'],
  },
  ptsLabel: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
  },
});

function PlayerComparator({ navigation }) {
  const { comparePlayer } = useLeague();
  const { width: screenWidth } = useWindowDimensions();

  const [playerB, setPlayerB] = useState(null);
  const [summaryA, setSummaryA] = useState(null);
  const [summaryB, setSummaryB] = useState(null);
  const [matchesA, setMatchesA] = useState([]);
  const [matchesB, setMatchesB] = useState([]);
  const [allPlayers, setAllPlayers] = useState([]);
  const [search, setSearch] = useState('');
  const [loadingStats, setLoadingStats] = useState(false);
  const [loadingPlayers, setLoadingPlayers] = useState(true);

  // Fetch all players for the picker
  useEffect(() => {
    (async () => {
      try {
        const res = await authenticatedFetch('/api/v1/players');
        if (res.ok) {
          const data = await res.json();
          setAllPlayers(Array.isArray(data) ? data : []);
        }
      } catch (e) {
        console.error('Error loading players:', e);
      } finally {
        setLoadingPlayers(false);
      }
    })();
  }, []);

  // Fetch summary for player A when comparePlayer changes
  useEffect(() => {
    if (!comparePlayer?.id) return;
    loadSummary(comparePlayer.id, setSummaryA, setMatchesA);
  }, [comparePlayer?.id]);

  // Fetch summary for player B when selected
  useEffect(() => {
    if (!playerB?.id) return;
    loadSummary(playerB.id, setSummaryB, setMatchesB);
  }, [playerB?.id]);

  const loadSummary = async (playerId, setSummary, setMatches) => {
    setLoadingStats(true);
    try {
      const [summaryRes, matchesRes] = await Promise.all([
        authenticatedFetch(`/api/statistics/player/${playerId}/summary`),
        authenticatedFetch(`/api/statistics/player/${playerId}/matches`),
      ]);

      if (summaryRes.ok) {
        const data = await summaryRes.json();
        setSummary(data);
      }

      if (matchesRes.ok) {
        const payload = await matchesRes.json();
        // Flatten matches array
        const flat = [];
        if (Array.isArray(payload)) {
          payload.forEach(item => {
            if (item && Array.isArray(item.matches)) {
              item.matches.forEach(m => flat.push({ round: m.round ?? item.jornada, points: m.fantasyPoints ?? 0 }));
            } else if (item && item.round != null) {
              flat.push({ round: item.round, points: item.fantasyPoints ?? 0 });
            }
          });
        }
        flat.sort((a, b) => (a.round ?? 0) - (b.round ?? 0));
        setMatches(flat);
      }
    } catch (e) {
      console.error('Error loading summary:', e);
    } finally {
      setLoadingStats(false);
    }
  };

  const filteredPlayers = search.trim()
    ? allPlayers.filter(p =>
        (p.fullName ?? p.name ?? '').toLowerCase().includes(search.toLowerCase()) &&
        p.id !== comparePlayer?.id
      )
    : [];

  const buildChartData = () => {
    if (matchesA.length === 0 && matchesB.length === 0) return null;

    const allRounds = [...new Set([
      ...matchesA.map(m => m.round),
      ...matchesB.map(m => m.round),
    ])].filter(r => r != null).sort((a, b) => a - b).slice(0, 20); // max 20 for readability

    if (allRounds.length < 2) return null;

    const pointsMapA = Object.fromEntries(matchesA.map(m => [m.round, m.points]));
    const pointsMapB = Object.fromEntries(matchesB.map(m => [m.round, m.points]));

    const dataA = allRounds.map(r => pointsMapA[r] ?? 0);
    const dataB = allRounds.map(r => pointsMapB[r] ?? 0);

    const labels = allRounds.map(r => `J${r}`);
    // Show every other label if too many
    const displayLabels = labels.map((l, i) => (allRounds.length > 10 ? (i % 2 === 0 ? l : '') : l));

    return {
      labels: displayLabels,
      datasets: [
        { data: dataA, color: () => COLOR_A, strokeWidth: 2 },
        ...(matchesB.length > 0 ? [{ data: dataB, color: () => COLOR_B, strokeWidth: 2 }] : []),
      ],
      legend: [
        comparePlayer?.fullName?.split(' ')[0] ?? 'Jugador A',
        ...(playerB ? [playerB.fullName?.split(' ')[0] ?? 'Jugador B'] : []),
      ],
    };
  };

  const COLOR_A = 'rgba(22, 163, 74, 1)';
  const COLOR_B = 'rgba(59, 130, 246, 1)';

  const chartData = buildChartData();
  const bothSelected = comparePlayer && playerB;

  if (!comparePlayer) {
    return (
      <View style={styles.container}>
        <View style={styles.topBar}>
          <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
            <Text style={styles.backBtnText}>← Volver</Text>
          </TouchableOpacity>
          <Text style={styles.topBarTitle}>Comparar jugadores</Text>
          <View style={{ width: 80 }} />
        </View>
        <View style={styles.centered}>
          <Text style={styles.emptyText}>Selecciona un jugador desde sus estadísticas para comparar</Text>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Top bar */}
      <View style={styles.topBar}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
          <Text style={styles.backBtnText}>← Volver</Text>
        </TouchableOpacity>
        <Text style={styles.topBarTitle}>Comparar</Text>
        <View style={{ width: 80 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {/* Player cards */}
        <View style={styles.cardsRow}>
          <PlayerCard player={comparePlayer} summary={summaryA} color={COLOR_A} />
          <View style={styles.vsCircle}><Text style={styles.vsText}>VS</Text></View>
          <PlayerCard player={playerB} summary={summaryB} color={COLOR_B} />
        </View>

        {/* Player B search */}
        {!playerB && (
          <View style={styles.searchSection}>
            <Text style={styles.searchLabel}>Elige al rival</Text>
            <TextInput
              style={styles.searchInput}
              placeholder="Buscar jugador..."
              placeholderTextColor={colors.textMuted}
              value={search}
              onChangeText={setSearch}
            />
            {loadingPlayers && <ActivityIndicator size="small" color={colors.primary} style={{ marginTop: 8 }} />}
            {filteredPlayers.slice(0, 8).map(p => {
              const posColor = positionBadgeColors[p.position] ?? positionBadgeColors.MID;
              return (
                <TouchableOpacity
                  key={p.id}
                  style={styles.playerRow}
                  onPress={() => { setPlayerB(p); setSearch(''); }}
                  activeOpacity={0.8}
                >
                  <View style={[styles.posTag, { backgroundColor: posColor.bg, borderColor: posColor.border }]}>
                    <Text style={[styles.posTagText, { color: posColor.text }]}>{p.position}</Text>
                  </View>
                  <Text style={styles.playerRowName}>{p.fullName ?? p.name}</Text>
                  <Text style={styles.playerRowValue}>€{((p.marketValue ?? 0) / 1_000_000).toFixed(1)}M</Text>
                </TouchableOpacity>
              );
            })}
            {search.trim().length > 0 && filteredPlayers.length === 0 && !loadingPlayers && (
              <Text style={styles.noResults}>Sin resultados para "{search}"</Text>
            )}
          </View>
        )}

        {playerB && (
          <TouchableOpacity
            style={styles.changeBtn}
            onPress={() => { setPlayerB(null); setSummaryB(null); setMatchesB([]); }}
          >
            <Text style={styles.changeBtnText}>Cambiar jugador B</Text>
          </TouchableOpacity>
        )}

        {/* Loading */}
        {loadingStats && <ActivityIndicator size="large" color={colors.primary} style={{ marginVertical: spacing.lg }} />}

        {/* Stats comparison table */}
        {bothSelected && summaryA && summaryB && !loadingStats && (
          <View style={styles.tableSection}>
            <Text style={styles.tableSectionTitle}>Estadísticas de temporada</Text>
            <StatRow label="Puntos fantasy" valueA={summaryA.totalFantasyPoints} valueB={summaryB.totalFantasyPoints} />
            <StatRow label="Partidos" valueA={summaryA.matchesPlayed} valueB={summaryB.matchesPlayed} />
            <StatRow label="Pts / partido" valueA={(summaryA.avgFantasyPoints ?? 0).toFixed(1)} valueB={(summaryB.avgFantasyPoints ?? 0).toFixed(1)} />
            <StatRow label="Goles" valueA={summaryA.totalGoals} valueB={summaryB.totalGoals} />
            <StatRow label="Asistencias" valueA={summaryA.totalAssists} valueB={summaryB.totalAssists} />
            <StatRow label="Rating medio" valueA={(summaryA.avgRating ?? 0).toFixed(2)} valueB={(summaryB.avgRating ?? 0).toFixed(2)} />
            <StatRow label="Minutos" valueA={summaryA.totalMinutesPlayed} valueB={summaryB.totalMinutesPlayed} />
            <StatRow label="Tarjetas A." valueA={summaryA.totalYellowCards} valueB={summaryB.totalYellowCards} higherIsBetter={false} />
            <StatRow label="Tarjetas R." valueA={summaryA.totalRedCards} valueB={summaryB.totalRedCards} higherIsBetter={false} />
          </View>
        )}

        {/* Points evolution chart */}
        {bothSelected && summaryA && summaryB && chartData && !loadingStats && (
          <View style={styles.chartSection}>
            <Text style={styles.chartTitle}>Puntos por jornada</Text>
            <View style={styles.legendRow}>
              <View style={[styles.legendDot, { backgroundColor: COLOR_A }]} />
              <Text style={styles.legendText}>{comparePlayer.fullName?.split(' ')[0] ?? 'A'}</Text>
              <View style={[styles.legendDot, { backgroundColor: COLOR_B }]} />
              <Text style={styles.legendText}>{playerB.fullName?.split(' ')[0] ?? 'B'}</Text>
            </View>
            <LineChart
              data={chartData}
              width={screenWidth - 64}
              height={220}
              chartConfig={{
                backgroundColor: colors.bgCard,
                backgroundGradientFrom: colors.bgCard,
                backgroundGradientTo: colors.bgCard,
                decimalPlaces: 0,
                color: (opacity = 1) => `rgba(22, 163, 74, ${opacity})`,
                labelColor: (opacity = 1) => `rgba(71, 85, 105, ${opacity})`,
                style: { borderRadius: radius.lg },
                propsForDots: { r: '3', strokeWidth: '1' },
              }}
              style={{ marginVertical: 8, borderRadius: radius.lg }}
              withLegend={false}
              {...webSafeProps}
            />
          </View>
        )}
      </ScrollView>
    </View>
  );
}

export default withAuth(PlayerComparator);

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgApp,
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.lg,
    paddingBottom: spacing.md,
    backgroundColor: colors.bgCard,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  topBarTitle: {
    fontFamily: fontFamily.displayBold,
    fontSize: fontSize.xl,
    color: colors.textPrimary,
  },
  backBtn: {
    backgroundColor: colors.primaryDeep,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: radius.pill,
    width: 80,
  },
  backBtnText: {
    color: colors.textInverse,
    fontWeight: fontWeight.semibold,
    fontSize: fontSize.xs,
  },
  content: {
    padding: spacing.lg,
    paddingBottom: 60,
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: spacing['3xl'],
  },
  emptyText: {
    fontFamily: fontFamily.body,
    fontSize: fontSize.sm,
    color: colors.textMuted,
    textAlign: 'center',
  },
  cardsRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: spacing.sm,
    marginBottom: spacing.lg,
  },
  vsCircle: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.primaryDeep,
    justifyContent: 'center',
    alignItems: 'center',
    alignSelf: 'center',
    flexShrink: 0,
  },
  vsText: {
    color: colors.textInverse,
    fontFamily: fontFamily.displayBold,
    fontSize: fontSize.xs,
  },
  searchSection: {
    backgroundColor: colors.bgCard,
    borderRadius: radius.lg,
    padding: spacing.md,
    marginBottom: spacing.lg,
    borderWidth: 1,
    borderColor: colors.border,
    ...shadow.sm,
  },
  searchLabel: {
    fontFamily: fontFamily.displaySemi,
    fontSize: fontSize.md,
    color: colors.textPrimary,
    marginBottom: spacing.sm,
  },
  searchInput: {
    backgroundColor: colors.bgSubtle,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    height: 44,
    color: colors.textPrimary,
    borderWidth: 1.5,
    borderColor: colors.border,
    fontFamily: fontFamily.body,
    fontSize: fontSize.sm,
    marginBottom: spacing.sm,
  },
  playerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  posTag: {
    borderRadius: radius.pill,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
    borderWidth: 1,
    minWidth: 36,
    alignItems: 'center',
  },
  posTagText: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.bold,
  },
  playerRowName: {
    flex: 1,
    fontSize: fontSize.sm,
    color: colors.textPrimary,
    fontWeight: fontWeight.semibold,
  },
  playerRowValue: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
    fontWeight: fontWeight.semibold,
  },
  noResults: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
    textAlign: 'center',
    paddingVertical: spacing.md,
  },
  changeBtn: {
    alignSelf: 'center',
    backgroundColor: colors.bgSubtle,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.xl,
    paddingVertical: spacing.sm,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.lg,
  },
  changeBtnText: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
    fontWeight: fontWeight.semibold,
  },
  tableSection: {
    backgroundColor: colors.bgCard,
    borderRadius: radius.lg,
    padding: spacing.md,
    marginBottom: spacing.lg,
    borderWidth: 1,
    borderColor: colors.border,
    ...shadow.sm,
  },
  tableSectionTitle: {
    fontFamily: fontFamily.displaySemi,
    fontSize: fontSize.md,
    color: colors.textPrimary,
    marginBottom: spacing.sm,
  },
  chartSection: {
    backgroundColor: colors.bgCard,
    borderRadius: radius.lg,
    padding: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
    ...shadow.sm,
  },
  chartTitle: {
    fontFamily: fontFamily.displaySemi,
    fontSize: fontSize.md,
    color: colors.primaryDark,
    marginBottom: spacing.sm,
  },
  legendRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    marginBottom: spacing.sm,
  },
  legendDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  legendText: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
    fontWeight: fontWeight.semibold,
    marginRight: spacing.sm,
  },
});
