import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, ActivityIndicator, RefreshControl } from 'react-native';
import { useLeague } from '../../context/LeagueContext';
import authService, { authenticatedFetch } from '../../services/authService';
import predictionService from '../../services/predictionService';
import withAuth from '../../components/withAuth';
import { colors, fontSize, fontWeight, radius, spacing, shadow, positionBadgeColors } from '../../utils/theme';

const POSITION_GROUPS = [
	{ key: 'POR', label: 'Porteros' },
	{ key: 'DEF', label: 'Defensas' },
	{ key: 'MID', label: 'Centrocampistas' },
	{ key: 'DEL', label: 'Delanteros' },
];

const getPosColor = (pos) => positionBadgeColors[pos] || { bg: colors.bgSubtle, text: colors.textSecondary, border: colors.border, bar: colors.textMuted };

const MAX_PTS = 15; // normalisation cap for progress bars

function PtsBar({ pts, color }) {
	const pct = Math.min((pts / MAX_PTS) * 100, 100);
	return (
		<View style={barStyles.track}>
			<View style={[barStyles.fill, { width: `${pct}%`, backgroundColor: color }]} />
		</View>
	);
}
const barStyles = StyleSheet.create({
	track: { height: 5, flex: 1, backgroundColor: colors.border, borderRadius: 3, overflow: 'hidden' },
	fill:  { height: 5, borderRadius: 3 },
});

function AIInsights() {
	const { selectedLeague } = useLeague();

	const [team, setTeam] = useState(null);
	const [playerPredictions, setPlayerPredictions] = useState({});
	const [marketPlayers, setMarketPlayers] = useState([]);
	const [marketPredictions, setMarketPredictions] = useState({});
	const [nextRoundInfo, setNextRoundInfo] = useState(null);
	const [loadingTeam, setLoadingTeam] = useState(true);
	const [loadingPlayerPredictions, setLoadingPlayerPredictions] = useState(false);
	const [loadingMarket, setLoadingMarket] = useState(true);
	const [loadingMarketPredictions, setLoadingMarketPredictions] = useState(false);
	const [refreshing, setRefreshing] = useState(false);
	const [teamError, setTeamError] = useState(null);
	const [marketError, setMarketError] = useState(null);
	const [lastUpdatedAt, setLastUpdatedAt] = useState(null);
	const [expandedPlayer, setExpandedPlayer] = useState(null);

	const loadData = async () => {
		if (!selectedLeague?.id) {
			setTeamError('No hay liga seleccionada');
			setLoadingTeam(false);
			setLoadingMarket(false);
			return;
		}
		setTeamError(null); setMarketError(null);
		setLoadingTeam(true); setLoadingMarket(true);

		let userId;
		try {
			const user = await authService.getCurrentUser();
			if (!user?.id) { setTeamError('Usuario no autenticado'); setLoadingTeam(false); setLoadingMarket(false); return; }
			userId = user.id;
		} catch { setTeamError('Error de autenticacion'); setLoadingTeam(false); setLoadingMarket(false); return; }

		const [roundResult, teamResult, marketResult] = await Promise.allSettled([
			predictionService.getNextRoundMatches(),
			authenticatedFetch(`/api/v1/teams/league/${selectedLeague.id}/${userId}`).then(r => { if (!r.ok) throw new Error(); return r.json(); }),
			authenticatedFetch(`/api/v1/market?leagueId=${selectedLeague.id}`).then(r => { if (!r.ok) throw new Error(); return r.json(); }),
		]);

		if (roundResult.status === 'fulfilled' && roundResult.value) setNextRoundInfo(roundResult.value);

		let teamData = null;
		if (teamResult.status === 'fulfilled') { teamData = teamResult.value; setTeam(teamData); }
		else setTeamError('No se pudo cargar tu equipo');
		setLoadingTeam(false);

		let marketData = [];
		if (marketResult.status === 'fulfilled') { marketData = Array.isArray(marketResult.value) ? marketResult.value : []; setMarketPlayers(marketData); }
		else setMarketError('No se pudieron cargar los jugadores del mercado');
		setLoadingMarket(false);

		if (teamData?.playerTeams?.length > 0) {
			setLoadingPlayerPredictions(true);
			const playerIds = teamData.playerTeams.map(pt => pt.player?.id).filter(Boolean);
			const results = await Promise.allSettled(playerIds.map(pid => predictionService.predictPlayerPoints(pid)));
			const pMap = {};
			playerIds.forEach((pid, i) => { if (results[i].status === 'fulfilled' && results[i].value) pMap[pid] = results[i].value; });
			setPlayerPredictions(pMap);
			setLoadingPlayerPredictions(false);
		}

		if (marketData.length > 0) {
			setLoadingMarketPredictions(true);
			const mIds = marketData.map(mp => mp.player?.id).filter(Boolean);
			const mResults = await Promise.allSettled(mIds.map(pid => predictionService.predictPlayerPoints(pid)));
			const mMap = {};
			mIds.forEach((pid, i) => { if (mResults[i].status === 'fulfilled' && mResults[i].value) mMap[pid] = mResults[i].value; });
			setMarketPredictions(mMap);
			setLoadingMarketPredictions(false);
		}
		setLastUpdatedAt(new Date());
		setRefreshing(false);
	};

	useEffect(() => { loadData(); }, [selectedLeague?.id]);
	const onRefresh = () => { setRefreshing(true); loadData(); };

	// ── Derived / computed insight values ──────────────────────────────────
	const totalPredicted = useMemo(() =>
		Object.values(playerPredictions).reduce((s, p) => s + (p.predictedPoints || 0), 0),
		[playerPredictions]);

	const playersByPosition = useMemo(() => {
		const map = {};
		if (team?.playerTeams) {
			team.playerTeams.forEach(pt => {
				const pos = pt.player?.position || 'MID';
				if (!map[pos]) map[pos] = [];
				map[pos].push(pt);
			});
			Object.keys(map).forEach(pos => {
				map[pos].sort((a, b) => (playerPredictions[b.player?.id]?.predictedPoints || 0) - (playerPredictions[a.player?.id]?.predictedPoints || 0));
			});
		}
		return map;
	}, [team, playerPredictions]);

	const sortedMarketPlayers = useMemo(() =>
		[...marketPlayers].sort((a, b) => {
			const pA = marketPredictions[a.player?.id]?.predictedPoints;
			const pB = marketPredictions[b.player?.id]?.predictedPoints;
			if (pA != null && pB != null) return pB - pA;
			return pA != null ? -1 : 1;
		}),
		[marketPlayers, marketPredictions]);

	// Captain: highest predicted in my team
	const captainRec = useMemo(() => {
		if (!team?.playerTeams || Object.keys(playerPredictions).length === 0) return null;
		return team.playerTeams
			.filter(pt => pt.player?.id && playerPredictions[pt.player.id])
			.sort((a, b) => (playerPredictions[b.player.id]?.predictedPoints || 0) - (playerPredictions[a.player.id]?.predictedPoints || 0))[0] || null;
	}, [team, playerPredictions]);

	// Market top pick by pts/value efficiency
	const marketTopPick = useMemo(() => {
		const withEff = sortedMarketPlayers
			.filter(mp => marketPredictions[mp.player?.id]?.predictedPoints && mp.player?.marketValue > 0)
			.map(mp => ({
				...mp,
				pts: marketPredictions[mp.player.id].predictedPoints,
				eff: (marketPredictions[mp.player.id].predictedPoints / (mp.player.marketValue / 1_000_000)),
			}))
			.sort((a, b) => b.eff - a.eff);
		return withEff[0] || null;
	}, [sortedMarketPlayers, marketPredictions]);

	// Weakest player in my team
	const weakestPlayer = useMemo(() => {
		if (!team?.playerTeams || Object.keys(playerPredictions).length === 0) return null;
		return team.playerTeams
			.filter(pt => pt.player?.id && playerPredictions[pt.player.id])
			.sort((a, b) => (playerPredictions[a.player.id]?.predictedPoints || 0) - (playerPredictions[b.player.id]?.predictedPoints || 0))[0] || null;
	}, [team, playerPredictions]);

	// ── Loading / no-league screens ──────────────────────────────────────
	if (loadingTeam && !team) {
		return (
			<View style={s.container}>
				<View style={s.topBar}>
					<Text style={s.topBarTitle}>Análisis Predictivo</Text>
					<Text style={s.topBarSub}>{selectedLeague?.name || '—'}</Text>
				</View>
				<View style={s.center}>
					<ActivityIndicator size="large" color={colors.primary} />
					<Text style={s.loadingTxt}>Calculando análisis de rendimiento...</Text>
				</View>
			</View>
		);
	}

	if (!selectedLeague?.id) {
		return (
			<View style={s.container}>
				<View style={s.topBar}><Text style={s.topBarTitle}>Análisis Predictivo</Text></View>
				<View style={s.center}><Text style={s.errorTxt}>Selecciona una liga primero</Text></View>
			</View>
		);
	}

	// ── Helpers ──────────────────────────────────────────────────────────
	const hasPlayerPreds = Object.keys(playerPredictions).length > 0;

	return (
		<View style={s.container}>
			{/* ══ TOP BAR ══════════════════════════════════════════════════ */}
			<View style={s.topBar}>
				<View style={{ flex: 1 }}>
					<Text style={s.topBarTitle}>Análisis Predictivo</Text>
					<Text style={s.topBarSub}>
						{nextRoundInfo?.round ? `Jornada ${nextRoundInfo.round} · ` : ''}{selectedLeague.name}
					</Text>
					{lastUpdatedAt && (
						<Text style={s.topBarFresh}>
							Actualizado {lastUpdatedAt.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })}
						</Text>
					)}
				</View>
				<TouchableOpacity style={s.refreshBtn} onPress={onRefresh} disabled={refreshing}>
					<Text style={s.refreshTxt}>{refreshing ? '...' : '↻'}</Text>
				</TouchableOpacity>
			</View>

			<ScrollView
				style={{ flex: 1 }}
				contentContainerStyle={s.scroll}
				refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={[colors.primary]} />}
			>
				{/* ══ INSIGHT CHIPS ════════════════════════════════════════ */}
				{(captainRec || marketTopPick || weakestPlayer) && (
					<ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.chipsRow}>
						{captainRec && playerPredictions[captainRec.player?.id] && (
							<View style={[s.insightChip, { borderColor: colors.warning, backgroundColor: colors.warningBg }]}>
								<Text style={s.chipIcon}>⭐</Text>
								<View>
									<Text style={s.chipLabel}>Capitán recomendado</Text>
									<Text style={s.chipValue}>{captainRec.player.fullName ?? captainRec.player.name}</Text>
									<Text style={s.chipSub}>{playerPredictions[captainRec.player.id].predictedPoints?.toFixed(1)} pts esperados</Text>
								</View>
							</View>
						)}
						{marketTopPick && (
							<View style={[s.insightChip, { borderColor: positionBadgeColors.DEF.border, backgroundColor: positionBadgeColors.DEF.bg }]}>
								<Text style={s.chipIcon}>🛒</Text>
								<View>
									<Text style={s.chipLabel}>Mejor fichaje eficiente</Text>
									<Text style={s.chipValue}>{marketTopPick.player.fullName ?? marketTopPick.player.name}</Text>
									<Text style={s.chipSub}>{marketTopPick.pts?.toFixed(1)} pts · {marketTopPick.eff?.toFixed(1)} pts/M€</Text>
								</View>
							</View>
						)}
						{weakestPlayer && playerPredictions[weakestPlayer.player?.id] && (
							<View style={[s.insightChip, { borderColor: colors.danger, backgroundColor: colors.dangerBg }]}>
								<Text style={s.chipIcon}>⚠️</Text>
								<View>
									<Text style={s.chipLabel}>Eslabón débil</Text>
									<Text style={s.chipValue}>{weakestPlayer.player.fullName ?? weakestPlayer.player.name}</Text>
									<Text style={s.chipSub}>{playerPredictions[weakestPlayer.player.id].predictedPoints?.toFixed(1)} pts esperados</Text>
								</View>
							</View>
						)}
					</ScrollView>
				)}

				{/* ══ HERO: PROYECCIÓN TOTAL ═══════════════════════════════ */}
				<View style={s.heroCard}>
					<Text style={s.heroLabel}>PUNTOS ESPERADOS PRÓXIMA JORNADA</Text>
					{loadingPlayerPredictions ? (
						<ActivityIndicator size="large" color={colors.primary} style={{ marginVertical: 12 }} />
					) : hasPlayerPreds ? (
						<>
							<Text style={s.heroValue}>{totalPredicted.toFixed(1)}</Text>
							<View style={s.heroBreakdown}>
								{POSITION_GROUPS.map(g => {
									const pts = (team?.playerTeams || [])
										.filter(pt => (pt.player?.position || 'MID') === g.key)
										.reduce((sum, pt) => sum + (playerPredictions[pt.player?.id]?.predictedPoints || 0), 0);
									if (pts === 0) return null;
									const pc = getPosColor(g.key);
									return (
										<View key={g.key} style={[s.heroBreakPill, { backgroundColor: pc.bg, borderColor: pc.border }]}>
											<Text style={[s.heroBreakPos, { color: pc.text }]}>{g.key}</Text>
											<Text style={[s.heroBreakPts, { color: pc.text }]}>{pts.toFixed(1)}</Text>
										</View>
									);
								})}
							</View>
						</>
					) : (
						<Text style={s.heroNoData}>
							{teamError ? teamError : 'Sin predicciones disponibles'}
						</Text>
					)}
				</View>

				{/* ══ TUS JUGADORES ════════════════════════════════════════ */}
				<View style={s.section}>
					<View style={s.sectionHeader}>
						<Text style={s.sectionTitle}>Tu Equipo</Text>
						{loadingPlayerPredictions && <ActivityIndicator size="small" color={colors.primary} />}
					</View>
					<Text style={s.sectionSub}>
					{Object.values(playerPredictions).some(p => p?.modelSource?.includes('XGBOOST'))
						? 'Predicción XGBoost · enriquecida con IA'
						: 'Media ponderada de las últimas 5 jornadas'}
				</Text>

					{teamError ? (
						<View style={s.errorCard}>
							<Text style={s.errorCardTxt}>{teamError}</Text>
							<TouchableOpacity style={s.retryBtn} onPress={loadData}>
								<Text style={s.retryTxt}>Reintentar</Text>
							</TouchableOpacity>
						</View>
					) : team && (!team.playerTeams || team.playerTeams.length === 0) ? (
						<Text style={s.emptyTxt}>No tienes jugadores en tu equipo</Text>
					) : (
						POSITION_GROUPS.map(group => {
							const players = playersByPosition[group.key];
							if (!players || players.length === 0) return null;
							const pc = getPosColor(group.key);
							return (
								<View key={group.key} style={s.posGroup}>
									<View style={[s.posGroupHeader, { backgroundColor: pc.bg, borderColor: pc.border }]}>
										<View style={[s.posGroupDot, { backgroundColor: pc.bar }]} />
										<Text style={[s.posGroupLabel, { color: pc.text }]}>{group.label}</Text>
									</View>

									{players.map((pt, idx) => {
										const player = pt.player;
										if (!player) return null;
										const pred = playerPredictions[player.id];
										const pts = pred?.predictedPoints || 0;
										const ci = pred?.confidenceInterval;
										const isBest = idx === 0 && pts > 0;
										const pc2 = getPosColor(player.position);

										return (
											<View key={player.id} style={[s.playerRow, isBest && s.playerRowBest]}>
												{isBest && (
													<View style={s.bestBadge}>
														<Text style={s.bestBadgeTxt}>TOP</Text>
													</View>
												)}
												<View style={s.playerLeft}>
													<View style={s.playerNameRow}>
														<Text style={s.playerName} numberOfLines={1}>
															{player.fullName || player.name}
														</Text>
													</View>
													<Text style={s.playerMeta}>
														{player.totalPoints != null ? `${player.totalPoints} pts acumulados` : 'Sin datos totales'}
													</Text>
													{ci && ci.length >= 2 && (
														<Text style={s.playerCI}>
															Rango: {Number(ci[0]).toFixed(1)} – {Number(ci[1]).toFixed(1)} pts
														</Text>
													)}
													{pred && <PtsBar pts={pts} color={pc2.bar} />}
												{pred?.aiAnalysis && (
													<TouchableOpacity
														onPress={() => setExpandedPlayer(expandedPlayer === player.id ? null : player.id)}
														style={s.claudeToggleBtn}
													>
														<Text style={s.claudeToggleTxt}>
															{expandedPlayer === player.id ? '▲ Ocultar análisis IA' : '▼ Ver análisis IA'}
														</Text>
													</TouchableOpacity>
												)}
												{expandedPlayer === player.id && pred?.aiAnalysis && (
													<View style={s.claudeCard}>
														<Text style={s.claudeLabel}>ANÁLISIS IA</Text>
														<Text style={s.claudeTxt}>{pred.aiAnalysis}</Text>
														{pred.modelSource && (
															<Text style={s.modelSourceBadge}>{pred.modelSource}</Text>
														)}
													</View>
												)}
												</View>
												<View style={s.playerRight}>
													{pred ? (
														<>
															<Text style={[s.playerPts, { color: pc2.bar }]}>{pts.toFixed(1)}</Text>
															<Text style={s.playerPtsLabel}>pts</Text>
														</>
													) : (
														<Text style={s.noPred}>—</Text>
													)}
												</View>
											</View>
										);
									})}
								</View>
							);
						})
					)}
				</View>

				{/* ══ MERCADO ══════════════════════════════════════════════ */}
				<View style={s.section}>
					<View style={s.sectionHeader}>
						<Text style={s.sectionTitle}>Oportunidades de Mercado</Text>
						{loadingMarketPredictions && <ActivityIndicator size="small" color={colors.primary} />}
					</View>
					<Text style={s.sectionSub}>Ordenados por puntos esperados · <Text style={s.sectionSubHighlight}>pts/M€</Text> = puntos predichos por millón de valor</Text>

					{loadingMarket ? (
						<ActivityIndicator size="small" color={colors.primary} style={{ margin: spacing.lg }} />
					) : marketError ? (
						<View style={s.errorCard}>
							<Text style={s.errorCardTxt}>{marketError}</Text>
						</View>
					) : sortedMarketPlayers.length === 0 ? (
						<Text style={s.emptyTxt}>No hay jugadores en el mercado</Text>
					) : (
						sortedMarketPlayers.map((mp, idx) => {
							const player = mp.player;
							if (!player) return null;
							const pred = marketPredictions[player.id];
							const pts = pred?.predictedPoints || 0;
							const eff = pts > 0 && player.marketValue > 0
								? (pts / (player.marketValue / 1_000_000)).toFixed(1)
								: null;
							const pc = getPosColor(player.position);
							const medal = idx === 0 ? '🥇' : idx === 1 ? '🥈' : idx === 2 ? '🥉' : null;
							const ci = pred?.confidenceInterval;

							return (
								<View key={mp.id} style={[s.marketCard, medal && s.marketCardTop]}>
									<View style={s.marketRank}>
										{medal
											? <Text style={s.medalTxt}>{medal}</Text>
											: <Text style={s.rankTxt}>#{idx + 1}</Text>
										}
									</View>
									<View style={s.marketInfo}>
										<View style={s.marketNameRow}>
											<Text style={s.marketName} numberOfLines={1}>
												{player.fullName || player.name}
											</Text>
											<View style={[s.posBadge, { backgroundColor: pc.bg, borderColor: pc.border }]}>
												<Text style={[s.posBadgeTxt, { color: pc.text }]}>{player.position}</Text>
											</View>
										</View>
										<Text style={s.marketMeta}>
											€{player.marketValue?.toLocaleString('es-ES')}
											{eff && <Text style={s.effTxt}>  ·  {eff} pts/M€</Text>}
										</Text>
										{ci && ci.length >= 2 && (
											<Text style={s.playerCI}>
												Rango: {Number(ci[0]).toFixed(1)} – {Number(ci[1]).toFixed(1)} pts
											</Text>
										)}
										{pred && <PtsBar pts={pts} color={pc.bar} />}
									</View>
									<View style={s.marketPtsCol}>
										{pred ? (
											<>
												<Text style={[s.marketPts, { color: pc.bar }]}>{pts.toFixed(1)}</Text>
												<Text style={s.marketPtsLabel}>pts</Text>
											</>
										) : (
											<Text style={s.noPred}>—</Text>
										)}
									</View>
								</View>
							);
						})
					)}
				</View>
			</ScrollView>
		</View>
	);
}

export default withAuth(AIInsights);

const s = StyleSheet.create({
	container: { flex: 1, backgroundColor: colors.bgApp },

	// Top bar
	topBar: {
		flexDirection: 'row', alignItems: 'center',
		backgroundColor: colors.primaryDeep,
		paddingHorizontal: spacing.lg, paddingVertical: spacing.lg,
		...shadow.md,
	},
	topBarTitle: { color: colors.textInverse, fontSize: fontSize.xl, fontWeight: fontWeight.extrabold },
	topBarSub: { color: 'rgba(209,250,229,0.85)', fontSize: fontSize.sm, fontWeight: fontWeight.semibold, marginTop: 2 },
	topBarFresh: { color: 'rgba(209,250,229,0.55)', fontSize: fontSize.xs, marginTop: 2 },
	refreshBtn: {
		width: 38, height: 38, borderRadius: 19,
		backgroundColor: 'rgba(255,255,255,0.15)',
		alignItems: 'center', justifyContent: 'center',
		borderWidth: 1, borderColor: 'rgba(255,255,255,0.25)',
	},
	refreshTxt: { color: colors.textInverse, fontSize: 20, fontWeight: fontWeight.bold },

	scroll: { padding: spacing.md, paddingBottom: 40, gap: spacing.md },

	center: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 32 },
	loadingTxt: { marginTop: spacing.lg, fontSize: fontSize.md, color: colors.textMuted, fontWeight: fontWeight.semibold },
	errorTxt: { fontSize: fontSize.md, color: colors.danger, textAlign: 'center' },

	// Insight chips
	chipsRow: { paddingVertical: 4, paddingHorizontal: 2, gap: spacing.sm },
	insightChip: {
		flexDirection: 'row', alignItems: 'center', gap: spacing.sm,
		borderWidth: 1.5, borderRadius: radius.xl,
		paddingHorizontal: spacing.md, paddingVertical: spacing.sm,
		...shadow.sm, minWidth: 200,
	},
	chipIcon: { fontSize: 24 },
	chipLabel: { fontSize: fontSize.xs, color: colors.textMuted, fontWeight: fontWeight.semibold, textTransform: 'uppercase', letterSpacing: 0.4 },
	chipValue: { fontSize: fontSize.sm, fontWeight: fontWeight.extrabold, color: colors.textPrimary },
	chipSub: { fontSize: fontSize.xs, color: colors.textSecondary, marginTop: 1 },

	// Hero projection card
	heroCard: {
		backgroundColor: colors.primaryDeep,
		borderRadius: radius.xl, paddingVertical: spacing.xl,
		paddingHorizontal: spacing.lg, alignItems: 'center',
		...shadow.lg,
		borderWidth: 1, borderColor: 'rgba(255,255,255,0.1)',
	},
	heroLabel: {
		color: 'rgba(209,250,229,0.75)', fontSize: fontSize.xs,
		fontWeight: fontWeight.bold, letterSpacing: 1.2,
		textTransform: 'uppercase', marginBottom: 8,
	},
	heroValue: {
		color: colors.textInverse, fontSize: 64,
		fontWeight: fontWeight.black, lineHeight: 72,
		marginBottom: spacing.md,
	},
	heroBreakdown: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, justifyContent: 'center' },
	heroBreakPill: {
		flexDirection: 'row', alignItems: 'center', gap: 6,
		paddingHorizontal: 12, paddingVertical: 6,
		borderRadius: radius.pill, borderWidth: 1,
	},
	heroBreakPos: { fontSize: fontSize.xs, fontWeight: fontWeight.extrabold, letterSpacing: 0.5 },
	heroBreakPts: { fontSize: fontSize.md, fontWeight: fontWeight.black },
	heroNoData: { color: 'rgba(255,255,255,0.6)', fontSize: fontSize.md, fontStyle: 'italic', marginTop: 8 },

	// Sections
	section: {
		backgroundColor: colors.bgCard, borderRadius: radius.xl,
		padding: spacing.lg, ...shadow.sm,
		borderWidth: 1, borderColor: colors.border,
	},
	sectionHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 2 },
	sectionTitle: { fontSize: fontSize.lg, fontWeight: fontWeight.extrabold, color: colors.textPrimary },
	sectionSub: { fontSize: fontSize.xs, color: colors.textMuted, marginBottom: spacing.md, fontWeight: fontWeight.medium },
	sectionSubHighlight: { fontWeight: fontWeight.bold, color: colors.primary },

	// Position groups
	posGroup: { marginBottom: spacing.sm },
	posGroupHeader: {
		flexDirection: 'row', alignItems: 'center', gap: 8,
		alignSelf: 'flex-start', paddingHorizontal: 12, paddingVertical: 5,
		borderRadius: radius.pill, borderWidth: 1,
		marginBottom: 8, marginTop: 4,
	},
	posGroupDot: { width: 7, height: 7, borderRadius: 4 },
	posGroupLabel: { fontSize: fontSize.xs, fontWeight: fontWeight.extrabold, letterSpacing: 0.5, textTransform: 'uppercase' },

	// Player rows
	playerRow: {
		flexDirection: 'row', alignItems: 'center',
		paddingVertical: 10, paddingHorizontal: 10,
		borderRadius: radius.md, marginBottom: 4,
		backgroundColor: colors.bgSubtle,
		borderWidth: 1, borderColor: colors.border,
	},
	playerRowBest: {
		backgroundColor: colors.successBg,
		borderColor: colors.primaryMuted,
	},
	bestBadge: {
		position: 'absolute', top: -1, right: 8,
		backgroundColor: colors.primary,
		borderRadius: radius.pill,
		paddingHorizontal: 7, paddingVertical: 2,
	},
	bestBadgeTxt: { color: colors.textInverse, fontSize: 9, fontWeight: fontWeight.extrabold, letterSpacing: 0.5 },
	playerLeft: { flex: 1, gap: 4, paddingRight: spacing.sm },
	playerNameRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
	playerName: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textPrimary, flexShrink: 1 },
	playerMeta: { fontSize: fontSize.xs, color: colors.textMuted },
	playerCI: { fontSize: 10, color: colors.textMuted, fontStyle: 'italic' },
	playerRight: { alignItems: 'flex-end', minWidth: 48 },
	playerPts: { fontSize: fontSize.xl, fontWeight: fontWeight.black },
	playerPtsLabel: { fontSize: fontSize.xs, color: colors.textMuted, fontWeight: fontWeight.semibold },
	noPred: { fontSize: fontSize.md, color: colors.textMuted },

	// Market cards
	marketCard: {
		flexDirection: 'row', alignItems: 'center',
		padding: spacing.md, borderRadius: radius.lg,
		borderWidth: 1, borderColor: colors.border,
		backgroundColor: colors.bgSubtle, marginBottom: 8,
		gap: spacing.sm,
	},
	marketCardTop: {
		backgroundColor: colors.successBg,
		borderColor: colors.primaryMuted,
	},
	marketRank: { width: 32, alignItems: 'center' },
	medalTxt: { fontSize: 22 },
	rankTxt: { fontSize: fontSize.sm, fontWeight: fontWeight.extrabold, color: colors.textMuted },
	marketInfo: { flex: 1, gap: 4 },
	marketNameRow: { flexDirection: 'row', alignItems: 'center', gap: 6, flexWrap: 'wrap' },
	marketName: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textPrimary, flexShrink: 1 },
	posBadge: { paddingHorizontal: 8, paddingVertical: 2, borderRadius: radius.pill, borderWidth: 1 },
	posBadgeTxt: { fontSize: 10, fontWeight: fontWeight.extrabold, letterSpacing: 0.5 },
	marketMeta: { fontSize: fontSize.xs, color: colors.textSecondary },
	effTxt: { fontWeight: fontWeight.extrabold, color: colors.primaryDark },
	marketPtsCol: { alignItems: 'flex-end', minWidth: 48 },
	marketPts: { fontSize: fontSize.xl, fontWeight: fontWeight.black },
	marketPtsLabel: { fontSize: fontSize.xs, color: colors.textMuted },

	// Claude AI analysis accordion
	claudeToggleBtn: { marginTop: spacing.xs, alignSelf: 'flex-start' },
	claudeToggleTxt: { fontSize: fontSize.xs, color: colors.primary, fontWeight: fontWeight.semibold },
	claudeCard: {
		marginTop: spacing.sm,
		padding: spacing.sm,
		backgroundColor: colors.bgSubtle,
		borderRadius: radius.md,
		borderWidth: 1,
		borderColor: colors.primaryMuted,
	},
	claudeLabel: {
		fontSize: 9,
		color: colors.primary,
		fontWeight: fontWeight.extrabold,
		letterSpacing: 0.8,
		marginBottom: 4,
	},
	claudeTxt: {
		fontSize: fontSize.xs,
		color: colors.textSecondary,
		lineHeight: 16,
		fontWeight: fontWeight.medium,
	},
	modelSourceBadge: {
		fontSize: 9,
		color: colors.textMuted,
		marginTop: 4,
		fontStyle: 'italic',
	},

	// Misc
	errorCard: {
		backgroundColor: colors.dangerBg, borderWidth: 1, borderColor: colors.danger,
		borderRadius: radius.md, padding: spacing.lg, alignItems: 'center',
	},
	errorCardTxt: { fontSize: fontSize.sm, color: colors.danger, textAlign: 'center', marginBottom: spacing.sm },
	retryBtn: { backgroundColor: colors.primary, paddingHorizontal: spacing.lg, paddingVertical: spacing.sm, borderRadius: radius.md },
	retryTxt: { color: colors.textInverse, fontWeight: fontWeight.bold, fontSize: fontSize.sm },
	emptyTxt: { fontSize: fontSize.sm, color: colors.textMuted, textAlign: 'center', paddingVertical: spacing.xl },
});
