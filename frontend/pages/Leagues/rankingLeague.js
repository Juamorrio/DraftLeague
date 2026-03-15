import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, StyleSheet, ActivityIndicator, ScrollView, TouchableOpacity, useWindowDimensions, Platform } from 'react-native';
import { LineChart } from 'react-native-chart-kit';
import authService from '../../services/authService';
import { useLeague } from '../../context/LeagueContext';
import { colors, fontSize, fontWeight, radius, spacing, shadow } from '../../utils/theme';

const webSafeProps = Platform.OS === 'web' ? {
  onStartShouldSetResponder: undefined,
  onResponderGrant: undefined,
  onResponderMove: undefined,
  onResponderRelease: undefined,
  onResponderTerminate: undefined,
  onResponderTerminationRequest: undefined,
} : {};

export default function RankingLeague({ league, onBack }) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [rows, setRows] = useState([]);
  const { setViewUser } = useLeague();
  const [currentUserId, setCurrentUserId] = useState(null);
  const [selectedView, setSelectedView] = useState('total'); // 'total' o número de jornada
  const [maxGameweek] = useState(38); // Número máximo de jornadas
  const [retryCount, setRetryCount] = useState(0);
  const [showChart, setShowChart] = useState(false);
  const [teamHistories, setTeamHistories] = useState([]); // [{ teamId, userDisplayName, cumulative: number[] }]
  const [loadingChart, setLoadingChart] = useState(false);
  const { width: screenWidth } = useWindowDimensions();

  const podiumRows = useMemo(() => rows.slice(0, 3), [rows]);

  useEffect(() => {
    if (!league || !league.id) return;
    const load = async () => {
      setLoading(true); setError('');
      try {
        let endpoint;
        if (selectedView === 'total') {
          endpoint = `/api/v1/leagues/${league.id}/ranking`;
        } else {
          endpoint = `/api/v1/fantasy-points/leagues/${league.id}/gameweek/${selectedView}/ranking`;
        }

        const res = await authService.authenticatedFetch(endpoint);
        if (!res.ok) throw new Error(await res.text());
        const data = await res.json();
        setRows(Array.isArray(data) ? data : []);
      } catch (e) {
        setError(e.message || 'Error');
      } finally { setLoading(false); }
    };
    load();
  }, [league, selectedView, retryCount]); // retryCount forces re-fetch on retry

  useEffect(() => {
    (async () => {
      const user = await authService.getCurrentUser();
      setCurrentUserId(user?.id ?? null);
    })();
  }, []);

  // Load team point histories when chart is toggled on and rows are available
  useEffect(() => {
    if (!showChart || rows.length === 0) return;
    const load = async () => {
      setLoadingChart(true);
      try {
        const results = await Promise.all(
          rows.map(async (r) => {
            try {
              const res = await authService.authenticatedFetch(
                `/api/v1/fantasy-points/teams/${r.teamId}/history`
              );
              if (!res.ok) return null;
              const data = await res.json();
              const history = Array.isArray(data?.history) ? data.history : [];
              history.sort((a, b) => a.gameweek - b.gameweek);
              // Build cumulative points array
              let acc = 0;
              const cumulative = history.map(({ points }) => { acc += points; return acc; });
              const labels = history.map(h => h.gameweek);
              return { teamId: r.teamId, userId: r.userId, userDisplayName: r.userDisplayName, cumulative, labels };
            } catch { return null; }
          })
        );
        setTeamHistories(results.filter(Boolean));
      } catch (e) {
        console.error('Error loading chart histories:', e);
      } finally {
        setLoadingChart(false);
      }
    };
    load();
  }, [showChart, rows]);

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <Text style={styles.title}>Ranking: {league?.name}</Text>
          <TouchableOpacity onPress={onBack} style={styles.backBtn}><Text style={styles.backTxt}>Volver</Text></TouchableOpacity>
        </View>
        <View style={styles.gwSelectorWrap}>
          <Text style={styles.gwSelectorTitle}>Ver por jornada</Text>
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.gwScrollContent}
          >
            <TouchableOpacity
              style={[styles.gwChip, selectedView === 'total' && styles.gwChipActive]}
              onPress={() => setSelectedView('total')}
            >
              <Text style={[styles.gwChipText, selectedView === 'total' && styles.gwChipTextActive]}>
                Total
              </Text>
            </TouchableOpacity>
            {Array.from({ length: maxGameweek }, (_, i) => i + 1).map(gw => (
              <TouchableOpacity
                key={gw}
                style={[styles.gwChip, selectedView === gw && styles.gwChipActive]}
                onPress={() => setSelectedView(gw)}
              >
                <Text style={[styles.gwChipText, selectedView === gw && styles.gwChipTextActive]}>
                  J{gw}
                </Text>
              </TouchableOpacity>
            ))}
          </ScrollView>
        </View>
        {loading && <ActivityIndicator size="large" color="#1d4ed8" />}
        {!loading && error && (
          <View style={styles.errorWrap}>
            <Text style={styles.error}>{error}</Text>
            <TouchableOpacity
              style={styles.retryBtn}
              onPress={() => {
                setError('');
                setLoading(true);
                setRetryCount(c => c + 1);
              }}
            >
              <Text style={styles.retryTxt}>Reintentar</Text>
            </TouchableOpacity>
          </View>
        )}
        {!loading && !error && podiumRows.length >= 2 && (
          <View style={styles.podium}>
            {[
              { data: podiumRows[1], medal: '🥈', color: '#9CA3AF', baseH: 56 },
              { data: podiumRows[0], medal: '🥇', color: '#F59E0B', baseH: 80 },
              podiumRows.length >= 3 ? { data: podiumRows[2], medal: '🥉', color: '#B45309', baseH: 44 } : null,
            ].filter(Boolean).map(({ data, medal, color, baseH }) => (
              <View key={data.teamId} style={styles.podiumCol}>
                <Text style={styles.podiumMedal}>{medal}</Text>
                <Text style={styles.podiumName} numberOfLines={2}>{data.userDisplayName || 'Usuario'}</Text>
                <Text style={[styles.podiumPts, { color }]}>
                  {selectedView === 'total' ? data.totalPoints : (data.gameweekPoints ?? 0)} pts
                </Text>
                <View style={[styles.podiumBase, { height: baseH, backgroundColor: color }]}>
                  <Text style={styles.podiumPosNum}>{data.position}</Text>
                </View>
              </View>
            ))}
          </View>
        )}
        {!loading && !error && rows.length === 0 && <Text style={styles.empty}>Sin equipos todavía.</Text>}

        {/* ── Gráfico de evolución ── */}
        {!loading && !error && rows.length > 0 && (
          <TouchableOpacity
            style={styles.chartToggleBtn}
            onPress={() => setShowChart(v => !v)}
            activeOpacity={0.8}
          >
            <Text style={styles.chartToggleTxt}>
              {showChart ? '▲ Ocultar evolución' : '📈 Ver evolución de puntos'}
            </Text>
          </TouchableOpacity>
        )}
        {showChart && (
          <View style={styles.chartCard}>
            <Text style={styles.chartTitle}>Puntos acumulados por jornada</Text>
            {loadingChart && <ActivityIndicator size="small" color={colors.primary} style={{ marginVertical: 16 }} />}
            {!loadingChart && teamHistories.length > 0 && (() => {
              const maxLen = Math.max(...teamHistories.map(t => t.cumulative.length));
              if (maxLen === 0) return <Text style={styles.chartEmpty}>Sin datos de puntos todavía</Text>;
              const labels = teamHistories[0]?.labels?.map(g => `J${g}`) ?? [];
              const displayLabels = labels.map((l, i) => (labels.length > 8 ? (i % 3 === 0 ? l : '') : l));
              const palette = [
                'rgba(22,163,74,1)',
                'rgba(59,130,246,1)',
                'rgba(239,68,68,1)',
                'rgba(245,158,11,1)',
                'rgba(139,92,246,1)',
                'rgba(20,184,166,1)',
                'rgba(249,115,22,1)',
                'rgba(236,72,153,1)',
              ];
              const datasets = teamHistories.map((t, i) => ({
                data: t.cumulative.length > 0 ? t.cumulative : [0],
                color: () => palette[i % palette.length],
                strokeWidth: t.userId === currentUserId ? 3 : 1.5,
              }));
              const chartData = { labels: displayLabels.slice(0, maxLen), datasets };
              return (
                <>
                  <LineChart
                    data={chartData}
                    width={screenWidth - 64}
                    height={220}
                    chartConfig={{
                      backgroundColor: colors.bgCard,
                      backgroundGradientFrom: colors.bgCard,
                      backgroundGradientTo: colors.bgCard,
                      decimalPlaces: 0,
                      color: (opacity = 1) => `rgba(22,163,74,${opacity})`,
                      labelColor: (opacity = 1) => `rgba(71,85,105,${opacity})`,
                      style: { borderRadius: radius.lg },
                      propsForDots: { r: '2', strokeWidth: '1' },
                    }}
                    style={{ marginVertical: 8, borderRadius: radius.lg }}
                    withDots={false}
                    {...webSafeProps}
                  />
                  <View style={styles.chartLegend}>
                    {teamHistories.map((t, i) => (
                      <View key={t.teamId} style={styles.legendItem}>
                        <View style={[styles.legendDot, { backgroundColor: palette[i % palette.length] }]} />
                        <Text style={[styles.legendName, t.userId === currentUserId && styles.legendNameMe]} numberOfLines={1}>
                          {t.userDisplayName ?? 'Equipo'}
                          {t.userId === currentUserId ? ' (tú)' : ''}
                        </Text>
                      </View>
                    ))}
                  </View>
                </>
              );
            })()}
            {!loadingChart && teamHistories.length === 0 && (
              <Text style={styles.chartEmpty}>Sin datos de evolución todavía</Text>
            )}
          </View>
        )}

        <View style={styles.listContent}>
          {rows.map(r => {
            const isMe = currentUserId != null && r.userId === currentUserId;
            return (
              <TouchableOpacity
                key={r.teamId}
                style={styles.row}
                activeOpacity={isMe ? 1 : 0.8}
                disabled={isMe}
                onPress={!isMe ? () => setViewUser({ id: r.userId, name: r.userDisplayName || 'Usuario' }) : undefined}
              >
                <Text style={styles.pos}>{r.position}</Text>
                <View style={styles.info}>
                  <View style={styles.nameWrap}>
                    <Text style={styles.name}>{r.userDisplayName || 'Usuario'}</Text>
                    {isMe && (
                      <View style={styles.meTag}><Text style={styles.meTagText}>Tú</Text></View>
                    )}
                  </View>
                  {selectedView === 'total' ? (
                    <Text style={styles.points}>{r.totalPoints} pts</Text>
                  ) : (
                    <View>
                      <Text style={styles.points}>{r.gameweekPoints} pts (jornada)</Text>
                      <Text style={styles.totalPointsSecondary}>{r.totalPoints} pts total</Text>
                    </View>
                  )}
                </View>
              </TouchableOpacity>
            );
          })}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  scrollContent: { paddingTop: spacing.xl, paddingHorizontal: spacing.lg, paddingBottom: 40 },
  header: { flexDirection: 'row', alignItems: 'center', marginBottom: spacing.lg },
  title: { flex: 1, fontSize: fontSize.lg, fontWeight: fontWeight.bold, color: colors.textPrimary },
  backBtn: { backgroundColor: colors.primaryDeep, paddingHorizontal: spacing.md, paddingVertical: spacing.sm, borderRadius: radius.pill },
  backTxt: { color: colors.textInverse, fontWeight: fontWeight.semibold },
  errorWrap: { alignItems: 'center', marginTop: spacing.xl, gap: spacing.md },
  error: { color: colors.danger, textAlign: 'center', fontSize: fontSize.sm },
  retryBtn: { backgroundColor: colors.primary, paddingHorizontal: spacing.xl, paddingVertical: spacing.sm, borderRadius: radius.pill },
  retryTxt: { color: colors.textInverse, fontWeight: fontWeight.bold, fontSize: fontSize.sm },
  empty: { marginTop: spacing.xl, color: colors.textMuted },
  listContent: { gap: spacing.sm },
  row: {
    flexDirection: 'row', backgroundColor: colors.bgCard, borderRadius: radius.lg,
    paddingVertical: spacing.md, paddingHorizontal: spacing.md,
    alignItems: 'center', borderWidth: 1, borderColor: colors.border, ...shadow.sm,
  },
  pos: { width: 32, textAlign: 'center', fontWeight: fontWeight.bold, color: colors.primary, fontSize: fontSize.md },
  info: { flex: 1 },
  nameWrap: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  name: { fontSize: fontSize.md, fontWeight: fontWeight.semibold, color: colors.textPrimary },
  meTag: { backgroundColor: colors.warning, paddingHorizontal: spacing.sm, paddingVertical: 2, borderRadius: radius.pill },
  meTagText: { color: colors.textInverse, fontWeight: fontWeight.bold, fontSize: fontSize.xs },
  points: { marginTop: 2, fontSize: fontSize.sm, color: colors.textSecondary },
  gwSelectorWrap: {
    backgroundColor: colors.bgCard,
    paddingTop: spacing.sm,
    paddingBottom: spacing.xs,
    borderRadius: radius.lg,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.md,
  },
  gwSelectorTitle: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.semibold,
    color: colors.textMuted,
    marginLeft: spacing.lg,
    marginBottom: 6,
    letterSpacing: 0.5,
    textTransform: 'uppercase',
  },
  gwScrollContent: {
    paddingHorizontal: spacing.md,
    paddingVertical: 6,
    gap: 8,
    alignItems: 'center',
  },
  gwChip: {
    paddingVertical: 7,
    paddingHorizontal: 14,
    borderRadius: radius.pill,
    backgroundColor: colors.bgSubtle,
    borderWidth: 1.5,
    borderColor: colors.border,
  },
  gwChipActive: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  gwChipText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.semibold,
    color: colors.textSecondary,
  },
  gwChipTextActive: {
    color: colors.textInverse,
    fontWeight: fontWeight.bold,
  },
  totalPointsSecondary: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2 },
  chartToggleBtn: {
    alignSelf: 'stretch',
    backgroundColor: colors.bgCard,
    borderRadius: radius.lg,
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    marginBottom: spacing.sm,
    ...shadow.sm,
  },
  chartToggleTxt: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.semibold,
    color: colors.primary,
  },
  chartCard: {
    backgroundColor: colors.bgCard,
    borderRadius: radius.lg,
    padding: spacing.md,
    marginBottom: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
    ...shadow.sm,
  },
  chartTitle: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.primaryDark,
    marginBottom: spacing.xs,
  },
  chartEmpty: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
    textAlign: 'center',
    paddingVertical: spacing.lg,
  },
  chartLegend: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
    marginTop: spacing.sm,
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  legendDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  legendName: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
  },
  legendNameMe: {
    fontWeight: fontWeight.bold,
    color: colors.primary,
  },
  podium: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    justifyContent: 'center',
    marginBottom: spacing.lg,
    gap: spacing.md,
    paddingHorizontal: spacing.md,
    paddingTop: spacing.md,
  },
  podiumCol: { flex: 1, alignItems: 'center', gap: 4 },
  podiumMedal: { fontSize: 24 },
  podiumName: { fontSize: fontSize.xs, fontWeight: fontWeight.bold, color: colors.textPrimary, textAlign: 'center' },
  podiumPts: { fontSize: fontSize.sm, fontWeight: fontWeight.black, textAlign: 'center' },
  podiumBase: { width: '100%', borderRadius: radius.sm, alignItems: 'center', justifyContent: 'center', minHeight: 44 },
  podiumPosNum: { fontSize: fontSize.xl, fontWeight: fontWeight.black, color: 'rgba(255,255,255,0.9)' },
});
