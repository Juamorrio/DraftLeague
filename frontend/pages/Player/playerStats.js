import { useEffect, useState, useRef } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, ActivityIndicator } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { useLeague } from '../../context/LeagueContext';
import { authenticatedFetch } from '../../services/authService';
import { Ionicons } from '@expo/vector-icons';
import withAuth from '../../components/withAuth';
import Player from '../../components/player';
import { colors, fontSize, fontWeight, radius, spacing, positionBadgeColors } from '../../utils/theme';

function MarketValueChart({ data }) {
	const CHART_HEIGHT = 150;
	const BAR_MIN_HEIGHT = 6;

	// Prepend an "Inicio" bar with the previousValue of the first entry so the
	// chart shows where the player started (before any recorded change).
	const chartPoints = data.length > 0
		? [
			{ label: 'Inicio', value: data[0].previousValue ?? 0, change: null },
			...data.map(d => ({ label: `J${d.gameweek}`, value: d.newValue ?? 0, change: d.changeAmount ?? 0 })),
		  ]
		: [];

	// Guard: no history yet — avoid Math.min/max on empty arrays
	if (chartPoints.length === 0) {
		return (
			<View style={{ paddingVertical: 32, alignItems: 'center' }}>
				<Text style={{ color: colors.textMuted, fontSize: fontSize.sm, fontWeight: fontWeight.semibold }}>
					Sin historial de valor de mercado aún
				</Text>
			</View>
		);
	}

	const values = chartPoints.map(p => p.value);
	const minVal = Math.min(...values);
	const maxVal = Math.max(...values);
	const range = maxVal - minVal || 1;

	return (
		<ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginVertical: 8 }}>
			<View style={{ flexDirection: 'row', alignItems: 'flex-end', height: CHART_HEIGHT + 56, paddingHorizontal: 8 }}>
				{chartPoints.map((point, index) => {
					const barHeight = BAR_MIN_HEIGHT + ((point.value - minVal) / range) * (CHART_HEIGHT - BAR_MIN_HEIGHT);
					const isInitial = index === 0;
					const barColor = isInitial ? colors.textMuted : point.change >= 0 ? colors.success : colors.danger;
					const valM = (point.value / 1_000_000).toFixed(2);
					return (
						<View key={point.label} style={{ alignItems: 'center', marginHorizontal: 5 }}>
							<Text style={{ fontSize: 9, color: colors.textSecondary, marginBottom: 3, fontWeight: fontWeight.semibold }}>
								{valM}M
							</Text>
							<View style={{ width: 32, height: barHeight, backgroundColor: barColor, borderRadius: 5 }} />
							<Text style={{ fontSize: 10, color: colors.textSecondary, marginTop: 5, fontWeight: fontWeight.bold }}>
								{point.label}
							</Text>
							{!isInitial && (
								<Text style={{ fontSize: 8, color: point.change >= 0 ? colors.success : colors.danger, fontWeight: fontWeight.bold }}>
									{point.change >= 0 ? '▲' : '▼'}
								</Text>
							)}
						</View>
					);
				})}
			</View>
		</ScrollView>
	);
}

function PlayerStats({ navigation }) {
	const { selectedPlayer, setComparePlayer } = useLeague();
	const [playerPrediction, setPlayerPrediction] = useState(null);
	const [loadingPrediction, setLoadingPrediction] = useState(false);
	const [statistics, setStatistics] = useState([]);
	const [loadingStats, setLoadingStats] = useState(false);
	const [activeTab, setActiveTab] = useState('stats');
	const [selectedRound, setSelectedRound] = useState(null);
	const [marketValueHistory, setMarketValueHistory] = useState([]);
	const [loadingMarketValue, setLoadingMarketValue] = useState(false);

	// Track which data sets have been fetched for the current player to avoid re-fetching on tab switch
	const fetchedRef = useRef({ playerId: null, stats: false, market: false, prediction: false });

	// Reset cache when player changes
	useEffect(() => {
		const pid = selectedPlayer?.id ?? null;
		if (fetchedRef.current.playerId !== pid) {
			fetchedRef.current = { playerId: pid, stats: false, market: false, prediction: false };
			setPlayerPrediction(null);
			setStatistics([]);
			setMarketValueHistory([]);
			setSelectedRound(null);
			// Auto-load stats for initial tab
			if (pid) loadStatistics(pid);
		}
	}, [selectedPlayer?.id]);

	// Load data lazily when tab is selected
	useEffect(() => {
		const pid = selectedPlayer?.id;
		if (!pid) return;
		if (activeTab === 'stats' && !fetchedRef.current.stats) loadStatistics(pid);
		if (activeTab === 'market' && !fetchedRef.current.market) loadMarketHistory(pid);
		if (activeTab === 'prediction' && !fetchedRef.current.prediction) loadPlayerPrediction(pid);
	}, [activeTab, selectedPlayer?.id]);

	const loadPlayerPrediction = async (pid) => {
		if (fetchedRef.current.prediction) return;
		fetchedRef.current.prediction = true;
		setLoadingPrediction(true);
		try {
			const response = await authenticatedFetch(`/api/ml/predict/player/${pid}`);
			if (response.ok) {
				const prediction = await response.json();
				setPlayerPrediction(prediction);
			}
		} catch (e) {
			console.log('Error al cargar prediccion de jugador:', e);
		} finally {
			setLoadingPrediction(false);
		}
	};

	const loadStatistics = async (pid) => {
		if (fetchedRef.current.stats) return;
		fetchedRef.current.stats = true;
		setLoadingStats(true);
		try {
			const fantasyTeamId = selectedPlayer?.fantasyTeamId;
			const matchesUrl = fantasyTeamId
				? `/api/statistics/player/${pid}/matches?teamId=${fantasyTeamId}`
				: `/api/statistics/player/${pid}/matches`;
			const response = await authenticatedFetch(matchesUrl);
			if (response.ok) {
				const payload = await response.json();
				let flatStats = [];
				if (Array.isArray(payload)) {
					payload.forEach(item => {
						if (item && Array.isArray(item.matches)) {
							item.matches.forEach(match => {
								flatStats.push({
									...match,
									round: match?.round ?? item?.jornada ?? null,
									totalFantasyPoints: match?.fantasyPoints || 0,
									match: { round: match?.round ?? item?.jornada ?? null }
								});
							});
						} else if (item && (item.round != null || item.matchId != null)) {
							flatStats.push({
								...item,
								totalFantasyPoints: item?.fantasyPoints || 0,
								match: { round: item?.round ?? null }
							});
						}
					});
				}
				flatStats = flatStats.filter(s => s?.round != null || s?.matchId != null);
				setStatistics(flatStats);
				if (flatStats.length > 0) setSelectedRound(flatStats[0].round);
			}
		} catch (e) {
			console.error('Error cargando estadísticas:', e);
		} finally {
			setLoadingStats(false);
		}
	};

	const loadMarketHistory = async (pid) => {
		if (fetchedRef.current.market) return;
		fetchedRef.current.market = true;
		setLoadingMarketValue(true);
		try {
			const response = await authenticatedFetch(`/api/v1/players/${pid}/market-value-history`);
			if (response.ok) {
				const data = await response.json();
				setMarketValueHistory(Array.isArray(data) ? data : []);
			} else {
				setMarketValueHistory([]);
			}
		} catch (e) {
			console.error('Error cargando historial de valor de mercado:', e);
			setMarketValueHistory([]);
		} finally {
			setLoadingMarketValue(false);
		}
	};

	const translateFeature = (featureName) => {
		const translations = {
			avgRatingLast3: 'Rating ultimos 3',
			avgMinutesLast3: 'Minutos ultimos 3',
			avgGoalsLast3: 'Goles ultimos 3',
			avgAssistsLast3: 'Asistencias ultimos 3',
			avgShotsOnTargetLast3: 'Tiros a puerta ultimos 3',
			avgKeyPassesLast3: 'Pases clave ultimos 3',
			avgPassAccuracyLast3: 'Precision pases ultimos 3',
			avgDuelsWonLast3: 'Duelos ganados ultimos 3',
			avgRatingLast5: 'Rating ultimos 5',
			avgMinutesLast5: 'Minutos ultimos 5',
			avgGoalsLast5: 'Goles ultimos 5',
			avgAssistsLast5: 'Asistencias ultimos 5',
			avgShotsOnTargetLast5: 'Tiros a puerta ultimos 5',
			avgKeyPassesLast5: 'Pases clave ultimos 5',
			avgRatingLast10: 'Rating ultimos 10',
			avgMinutesLast10: 'Minutos ultimos 10',
			ratingTrend: 'Tendencia rating',
			minutesTrend: 'Tendencia minutos',
			ratingStdDev: 'Consistencia rating',
			pointsStdDev: 'Consistencia puntos',
			recentFormPoints: 'Forma reciente',
			avgRatingHome: 'Rating local',
			avgRatingAway: 'Rating visitante',
			homeAdvantage: 'Ventaja local',
			matchesPlayed: 'Partidos jugados',
			seasonAvgRating: 'Rating temporada',
			totalSeasonPoints: 'Puntos temporada'
		};
		return translations[featureName] || featureName;
	};

	if (!selectedPlayer) {
		return (
			<View style={styles.container}>
				<View style={styles.topBar}>
					<Text style={styles.topBarTitle}>Estadísticas de Jugador</Text>
				</View>
				<View style={styles.emptyState}>
					<Text style={styles.emptyIcon}>👤</Text>
					<Text style={styles.emptyText}>Selecciona un jugador para ver sus estadísticas</Text>
				</View>
			</View>
		);
	}

	const posColor = positionBadgeColors[selectedPlayer.position] ?? positionBadgeColors.MID;

	return (
		<View style={styles.container}>
			{/* ── Hero card ── */}
			<View style={styles.heroCard}>
				<View style={[styles.heroPosBar, { backgroundColor: posColor.bar }]} />
				<View style={styles.heroInner}>
					<View style={styles.heroAvatarCol}>
						<Player
							name={selectedPlayer.fullName ?? selectedPlayer.name}
							avatar={selectedPlayer.avatarUrl ? { uri: selectedPlayer.avatarUrl } : null}
							teamId={selectedPlayer.teamId}
							size={60}
						/>
					</View>
					<View style={styles.heroMidCol}>
						<Text style={styles.heroName} numberOfLines={2}>
							{selectedPlayer.fullName ?? selectedPlayer.name}
						</Text>
						<View style={styles.heroMetaRow}>
							<View style={[styles.heroPosBadge, { backgroundColor: posColor.bg, borderColor: posColor.border }]}>
								<Text style={[styles.heroPosBadgeText, { color: posColor.text }]}>
									{selectedPlayer.position}
								</Text>
							</View>
							{selectedPlayer.team && (
								<Text style={styles.heroTeam} numberOfLines={1}>{selectedPlayer.team.name}</Text>
							)}
						</View>
						{selectedPlayer.marketValue != null && (
							<Text style={styles.heroValue}>
								€{(selectedPlayer.marketValue / 1_000_000).toFixed(2)}M
							</Text>
						)}
					</View>
					<View style={styles.heroRightCol}>
						{selectedPlayer.totalPoints !== undefined && (
							<View style={[styles.heroPtsBadge, { borderColor: posColor.bar }]}>
								<Text style={[styles.heroPtsNum, { color: posColor.bar }]}>
									{selectedPlayer.totalPoints}
								</Text>
								<Text style={styles.heroPtsLabel}>pts</Text>
							</View>
						)}
						<TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
							<Text style={styles.backBtnText}>← Volver</Text>
						</TouchableOpacity>
						<TouchableOpacity
							style={styles.compareBtn}
							onPress={() => { setComparePlayer(selectedPlayer); navigation.navigate('PlayerComparator'); }}
							activeOpacity={0.8}
						>
							<Ionicons name="git-compare-outline" size={13} color={colors.primary} />
							<Text style={styles.compareBtnText}>Comparar</Text>
						</TouchableOpacity>
					</View>
				</View>
			</View>

			{/* ── Tabs ── */}
			<View style={styles.tabsBar}>
				{[
					{ key: 'stats', label: '📊 Stats' },
					{ key: 'market', label: '💰 Valor' },
					{ key: 'prediction', label: '🔮 IA' },
				].map(tab => (
					<TouchableOpacity
						key={tab.key}
						style={[styles.tab, activeTab === tab.key && styles.tabActive]}
						onPress={() => setActiveTab(tab.key)}
					>
						<Text style={[styles.tabText, activeTab === tab.key && styles.tabTextActive]}>
							{tab.label}
						</Text>
					</TouchableOpacity>
				))}
			</View>

			<ScrollView style={styles.content} contentContainerStyle={{ paddingBottom: 24 }}>

				{/* TAB: ESTADÍSTICAS */}
				{activeTab === 'stats' && (
					<>
						{loadingStats && (
							<View style={styles.loadingContainer}>
								<ActivityIndicator size="small" color={colors.primaryDark} />
								<Text style={styles.loadingText}>Cargando estadísticas...</Text>
							</View>
						)}

						{/* Selector de Jornada */}
						{statistics.length > 0 && (
							<View style={styles.section}>
								<Text style={styles.sectionTitle}>📋 Selecciona una Jornada</Text>
								<View style={styles.pickerContainer}>
									<Picker
										selectedValue={selectedRound}
										onValueChange={(itemValue) => setSelectedRound(itemValue)}
										style={styles.picker}
									>
										<Picker.Item label="-- Selecciona una jornada --" value={null} />
										{statistics.map((stat) => (
											<Picker.Item
												key={stat.matchId}
												label={`Jornada ${stat.round} - vs ${stat.opponent}`}
												value={String(stat.round)}
											/>
										))}
									</Picker>
								</View>
							</View>
						)}

						{/* Estadísticas detalladas de jornada seleccionada */}
						{selectedRound !== null && (
							<>
								{(() => {
									const selectedStat = statistics.find(s => String(s.round) === String(selectedRound));
									if (!selectedStat) return null;

									return (
										<View style={styles.section}>
											<Text style={styles.sectionTitle}>
												Jornada {selectedStat.round} — vs {selectedStat.opponent}
											</Text>

											{/* Resumen general */}
											<View style={styles.matchSummaryCard}>
												<View style={styles.matchSummaryTop}>
													<View style={styles.matchStat}>
														<Text style={styles.matchStatNum}>{selectedStat.minutesPlayed}'</Text>
														<Text style={styles.matchStatLabel}>Minutos</Text>
													</View>
													<View style={styles.matchStat}>
														<Text style={styles.matchStatNum}>{selectedStat.rating?.toFixed(1) || '-'}</Text>
														<Text style={styles.matchStatLabel}>Rating</Text>
													</View>
													<View style={[styles.matchStat, styles.matchStatPts]}>
														<Text style={[styles.matchStatNum, { color: colors.primaryDark, fontSize: 22 }]}>
															{selectedStat.fantasyPoints}
														</Text>
														<Text style={styles.matchStatLabel}>Pts Fantasy</Text>
													</View>
													<View style={styles.matchStat}>
														<Text style={styles.matchStatNum}>
															{selectedStat.isHomeTeam ? '🏠' : '✈️'}
														</Text>
														<Text style={styles.matchStatLabel}>
															{selectedStat.goalsScored}-{selectedStat.goalsConceded}
														</Text>
													</View>
												</View>
											</View>

											{/* Desglose de Puntos Fantasy */}
											{selectedStat.pointsBreakdown && Object.keys(selectedStat.pointsBreakdown).length > 1 && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>Desglose de Puntos Fantasy</Text>
													{selectedStat.appliedChip && (() => {
														const CHIP_DISPLAY = {
															TRIPLE_CAP:     { icon: '👑', name: 'Triple Capitán' },
															DOUBLE_GOALS:   { icon: '⚽', name: 'Golazo' },
															SUPER_SAVES:    { icon: '🧤', name: 'Mano Segura' },
															NO_PENALTY:     { icon: '🟡', name: 'Juego Limpio' },
															DOUBLE_ASSISTS: { icon: '🎯', name: 'Rey de Asistencias' },
															DEFENSIVE_WEEK: { icon: '🛡️', name: 'Muralla' },
															LETHAL_STRIKER: { icon: '🏹', name: 'Delantero Letal' },
															CREATIVE_MIDS:  { icon: '🎨', name: 'Creador Total' },
															GOLDEN_MINUTES: { icon: '⏱️', name: 'Minutos de Oro' },
															BENCH_BOOST:    { icon: '🪑', name: 'Banco Boost' },
														};
														const chip = CHIP_DISPLAY[selectedStat.appliedChip];
														return (
															<View style={styles.chipBanner}>
																<Text style={styles.chipBannerText}>
																	{chip ? `${chip.icon} ${chip.name}` : selectedStat.appliedChip} — chip aplicado en esta jornada
																</Text>
															</View>
														);
													})()}
													<View style={styles.breakdownContainer}>
														{Object.entries(selectedStat.pointsBreakdown)
															.filter(([key]) => key !== 'total' && key !== 'chipActive')
															.map(([key, value]) => {
																const labels = {
																	minutesPlayed: 'Minutos jugados',
																	goals: 'Goles',
																	assists: 'Asistencias',
																	chancesCreated: 'Ocasiones creadas',
																	ratingBonus: 'Bonus por rating',
																	yellowCards: 'Tarjetas amarillas',
																	redCards: 'Tarjetas rojas',
																	foulsCommitted: 'Faltas cometidas',
																	defensiveActions: 'Acciones defensivas',
																	duelsBonus: 'Bonus duelos ganados',
																	cleanSheet: 'Portería a cero',
																	cleanSheetMid: 'Portería a cero',
																	saves: 'Paradas',
																	hatTrick: 'Bonus hat-trick',
																	penaltySaved: 'Penalti parado',
																	doubleAssist: 'Bonus doble asistencia',
																	tripleAssist: 'Bonus triple asistencia',
																	multipleGoalsConceded: 'Goles encajados (3+)',
																	multipleGoalsConcededDef: 'Goles encajados (def)',
																	penaltyCommitted: 'Penalti cometido',
																	penaltyMissed: 'Penalti fallado',
																};
																return (
																	<View key={key} style={styles.breakdownRow}>
																		<Text style={styles.breakdownLabel}>{labels[key] || key}</Text>
																		<Text style={[
																			styles.breakdownValue,
																			value > 0 ? styles.breakdownPositive : styles.breakdownNegative
																		]}>
																			{value > 0 ? `+${value}` : value}
																		</Text>
																	</View>
																);
															})}
														<View style={styles.breakdownTotalRow}>
															<Text style={styles.breakdownTotalLabel}>TOTAL</Text>
															<Text style={styles.breakdownTotalValue}>
																{selectedStat.pointsBreakdown.total}
															</Text>
														</View>
													</View>
												</View>
											)}

											{/* Estadísticas ofensivas */}
											{(selectedStat.goals > 0 || selectedStat.assists > 0 || selectedStat.totalShots > 0) && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>⚽ Ofensivo</Text>
													<View style={styles.statGrid2Col}>
														{selectedStat.goals > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>{selectedStat.goals}</Text>
																<Text style={styles.statLabel}>Goles</Text>
															</View>
														)}
														{selectedStat.assists > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>{selectedStat.assists}</Text>
																<Text style={styles.statLabel}>Asistencias</Text>
															</View>
														)}
														{selectedStat.shotsOnTarget > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>{selectedStat.shotsOnTarget}</Text>
																<Text style={styles.statLabel}>Tiros a puerta</Text>
															</View>
														)}
														{selectedStat.chancesCreated > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>{selectedStat.chancesCreated}</Text>
																<Text style={styles.statLabel}>Ocasiones</Text>
															</View>
														)}
														{selectedStat.totalShots > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>{selectedStat.totalShots}</Text>
																<Text style={styles.statLabel}>Tiros</Text>
															</View>
														)}
													</View>
												</View>
											)}

											{/* Pase y posesión */}
											{(selectedStat.totalPasses != null || selectedStat.accuratePasses != null) && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>🎯 Pases</Text>
													<View style={styles.statGrid2Col}>
														{selectedStat.totalPasses != null && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>{selectedStat.totalPasses}</Text>
																<Text style={styles.statLabel}>Pases</Text>
															</View>
														)}
														{selectedStat.totalPasses > 0 && selectedStat.accuratePasses != null && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>
																	{((selectedStat.accuratePasses / selectedStat.totalPasses) * 100).toFixed(0)}%
																</Text>
																<Text style={styles.statLabel}>Precisión</Text>
															</View>
														)}
													</View>
												</View>
											)}

											{/* Defensa */}
											{(selectedStat.tackles > 0 || selectedStat.interceptions > 0 || selectedStat.blocks > 0) && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>🛡️ Defensa</Text>
													<View style={styles.statGrid2Col}>
														{selectedStat.tackles > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>{selectedStat.tackles}</Text>
																<Text style={styles.statLabel}>Entradas</Text>
															</View>
														)}
														{selectedStat.interceptions > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>{selectedStat.interceptions}</Text>
																<Text style={styles.statLabel}>Intercepciones</Text>
															</View>
														)}
														{selectedStat.blocks > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>{selectedStat.blocks}</Text>
																<Text style={styles.statLabel}>Bloqueos</Text>
															</View>
														)}
													</View>
												</View>
											)}

											{/* Duelos */}
											{(selectedStat.duelsWon != null || selectedStat.duelsLost != null) && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>⚔️ Duelos</Text>
													<View style={styles.statGrid2Col}>
														{selectedStat.duelsWon != null && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>{selectedStat.duelsWon}</Text>
																<Text style={styles.statLabel}>Ganados</Text>
															</View>
														)}
														{selectedStat.duelsLost != null && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>{selectedStat.duelsLost}</Text>
																<Text style={styles.statLabel}>Perdidos</Text>
															</View>
														)}
														{selectedStat.duelsWon != null && selectedStat.duelsLost != null && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>
																	{selectedStat.duelsWon + selectedStat.duelsLost > 0
																		? ((selectedStat.duelsWon / (selectedStat.duelsWon + selectedStat.duelsLost)) * 100).toFixed(0) + '%'
																		: '-'
																	}
																</Text>
																<Text style={styles.statLabel}>% Ganador</Text>
															</View>
														)}
													</View>
												</View>
											)}

											{/* Portero */}
											{selectedStat.saves != null && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>🧤 Portero</Text>
													<View style={styles.statGrid2Col}>
														<View style={styles.statItem2Col}>
															<Text style={styles.statValue}>{selectedStat.saves || 0}</Text>
															<Text style={styles.statLabel}>Paradas</Text>
														</View>
														{selectedStat.goalsAllowed != null && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statValue}>{selectedStat.goalsAllowed}</Text>
																<Text style={styles.statLabel}>Goles recibidos</Text>
															</View>
														)}
													</View>
												</View>
											)}

											{/* Disciplina */}
											{(selectedStat.yellowCards > 0 || selectedStat.redCards > 0 || selectedStat.penaltyCommitted > 0) && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>🟥 Disciplina</Text>
													<View style={styles.statGrid2Col}>
														{selectedStat.yellowCards > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={[styles.statValue, { color: '#D97706' }]}>{selectedStat.yellowCards}</Text>
																<Text style={styles.statLabel}>Amarillas</Text>
															</View>
														)}
														{selectedStat.redCards > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={[styles.statValue, { color: colors.danger }]}>{selectedStat.redCards}</Text>
																<Text style={styles.statLabel}>Rojas</Text>
															</View>
														)}
														{selectedStat.penaltyCommitted > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={[styles.statValue, { color: colors.danger }]}>{selectedStat.penaltyCommitted}</Text>
																<Text style={styles.statLabel}>Penaltis</Text>
															</View>
														)}
													</View>
												</View>
											)}
										</View>
									);
								})()}
							</>
						)}

						{/* Resumen de temporada */}
						{statistics.length > 0 && (
							<View style={styles.section}>
								<Text style={styles.sectionTitle}>📈 Resumen de Temporada</Text>
								<View style={styles.seasonGrid}>
									<View style={styles.seasonStatBox}>
										<Text style={styles.seasonStatNum}>{statistics.length}</Text>
										<Text style={styles.seasonStatLabel}>Partidos</Text>
									</View>
									<View style={styles.seasonStatBox}>
										<Text style={styles.seasonStatNum}>
											{(statistics.reduce((s, x) => s + (x.rating || 0), 0) / statistics.length).toFixed(1)}
										</Text>
										<Text style={styles.seasonStatLabel}>Rating medio</Text>
									</View>
									<View style={styles.seasonStatBox}>
										<Text style={styles.seasonStatNum}>
											{statistics.reduce((s, x) => s + (x.goals || 0), 0)}
										</Text>
										<Text style={styles.seasonStatLabel}>Goles</Text>
									</View>
									<View style={styles.seasonStatBox}>
										<Text style={styles.seasonStatNum}>
											{statistics.reduce((s, x) => s + (x.assists || 0), 0)}
										</Text>
										<Text style={styles.seasonStatLabel}>Asistencias</Text>
									</View>
									<View style={styles.seasonStatBox}>
										<Text style={styles.seasonStatNum}>
											{statistics.reduce((s, x) => s + (x.minutesPlayed || 0), 0)}
										</Text>
										<Text style={styles.seasonStatLabel}>Minutos</Text>
									</View>
									<View style={[styles.seasonStatBox, { borderColor: colors.primaryDark }]}>
										<Text style={[styles.seasonStatNum, { color: colors.primaryDark }]}>
											{statistics.reduce((s, x) => s + (x.fantasyPoints || 0), 0)}
										</Text>
										<Text style={[styles.seasonStatLabel, { color: colors.primaryDark, fontWeight: '700' }]}>Pts totales</Text>
									</View>
								</View>
							</View>
						)}

						{!loadingStats && statistics.length === 0 && (
							<View style={styles.section}>
								<View style={styles.warningCard}>
									<Text style={styles.warningTitle}>ℹ️ Sin datos disponibles</Text>
									<Text style={styles.warningText}>
										No hay estadísticas disponibles para este jugador en las jornadas registradas.
									</Text>
								</View>
							</View>
						)}
					</>
				)}

				{/* TAB: VALOR DE MERCADO */}
				{activeTab === 'market' && (
					<>
						{loadingMarketValue ? (
							<View style={styles.loadingContainer}>
								<ActivityIndicator size="small" color={colors.primaryDark} />
								<Text style={styles.loadingText}>Cargando historial de valor...</Text>
							</View>
						) : marketValueHistory.length > 0 ? (
							<>
								{/* Resumen valor */}
								<View style={styles.section}>
									<View style={styles.mvSummaryRow}>
										<View style={styles.mvSummaryBox}>
											<Text style={styles.mvSummaryNum}>
												€{((selectedPlayer.marketValue ?? 0) / 1_000_000).toFixed(2)}M
											</Text>
											<Text style={styles.mvSummaryLabel}>Valor actual</Text>
										</View>
										{(() => {
											const first = marketValueHistory[0].previousValue ?? marketValueHistory[0].newValue ?? 0;
											const last = selectedPlayer.marketValue ?? 0;
											const totalChange = last - first;
											const isPos = totalChange >= 0;
											return (
												<View style={[styles.mvSummaryBox, { borderColor: isPos ? '#16A34A' : '#EF4444' }]}>
													<Text style={[styles.mvSummaryNum, { color: isPos ? '#16A34A' : '#EF4444' }]}>
														{isPos ? '+' : ''}{(totalChange / 1_000_000).toFixed(2)}M
													</Text>
													<Text style={styles.mvSummaryLabel}>Variación total</Text>
												</View>
											);
										})()}
										<View style={styles.mvSummaryBox}>
											<Text style={styles.mvSummaryNum}>{marketValueHistory.length}</Text>
											<Text style={styles.mvSummaryLabel}>Jornadas</Text>
										</View>
									</View>
								</View>

								{/* Gráfico */}
								<View style={styles.section}>
									<Text style={styles.sectionTitle}>📈 Evolución del Valor</Text>
									<View style={styles.chartContainer}>
										<MarketValueChart data={marketValueHistory} />
										<Text style={styles.chartLegend}>
											🟢 Subida · 🔴 Bajada · ⬜ Valor inicial
										</Text>
									</View>
								</View>

								{/* Lista detallada */}
								<View style={styles.section}>
									<Text style={styles.sectionTitle}>🗒️ Detalle por Jornada</Text>
									{[...marketValueHistory].reverse().map(h => {
										const changeAmount = h.changeAmount ?? 0;
										const isPositive = changeAmount >= 0;
										return (
											<View key={h.gameweek} style={styles.mvHistoryRow}>
												<Text style={styles.mvHistoryGameweek}>J{h.gameweek}</Text>
												<Text style={styles.mvHistoryValue}>
													€{((h.newValue ?? 0) / 1_000_000).toFixed(2)}M
												</Text>
												<Text style={[styles.mvHistoryDelta, isPositive ? styles.mvPositive : styles.mvNegative]}>
													{isPositive ? '▲ +' : '▼ '}{(changeAmount / 1_000_000).toFixed(2)}M
												</Text>
											</View>
										);
									})}
								</View>
							</>
						) : (
							<View style={styles.section}>
								<View style={styles.warningCard}>
									<Text style={styles.warningTitle}>ℹ️ Sin historial disponible</Text>
									<Text style={styles.warningText}>
										El historial de valor de mercado se genera automáticamente al calcular
										los puntos de cada jornada desde el panel de administración.
									</Text>
								</View>
							</View>
						)}
					</>
				)}

				{/* TAB: PREDICCIÓN */}
				{activeTab === 'prediction' && (
					<>
						{(playerPrediction || loadingPrediction) ? (
							<View style={styles.section}>
								<Text style={styles.sectionTitle}>🔮 Predicción</Text>

								{loadingPrediction ? (
									<View style={styles.loadingContainer}>
										<ActivityIndicator size="small" color={colors.primaryDark} />
										<Text style={styles.loadingText}>Generando predicción...</Text>
									</View>
								) : playerPrediction ? (
									<>
										<View style={styles.predictionCard}>
											<Text style={styles.predictionLabel}>PUNTOS PREDICHOS — PRÓXIMO PARTIDO</Text>
											<Text style={styles.predictionValue}>
												{playerPrediction.predictedPoints?.toFixed(1) || '0.0'}
											</Text>
											{playerPrediction.confidenceInterval?.length === 2 && (
												<Text style={styles.predictionRange}>
													Rango estimado: {playerPrediction.confidenceInterval[0]}–{playerPrediction.confidenceInterval[1]} pts
												</Text>
											)}
										</View>

										{playerPrediction.featuresImportance && Object.keys(playerPrediction.featuresImportance).length > 0 && (
											<View style={styles.statsSection}>
												<Text style={styles.statsSubtitle}>⭐ Factores clave</Text>
												{Object.entries(playerPrediction.featuresImportance)
													.sort((a, b) => b[1] - a[1])
													.map(([feature, importance]) => (
														<View key={feature} style={styles.featureRow}>
															<Text style={styles.featureName}>
																{translateFeature(feature)}
															</Text>
															<View style={styles.importanceBarContainer}>
																<View
																	style={[
																		styles.importanceFill,
																		{ width: `${Math.min(importance * 100, 100)}%` }
																	]}
																/>
															</View>
															<Text style={styles.importanceValue}>
																{(importance * 100).toFixed(1)}%
															</Text>
														</View>
													))}
											</View>
										)}

										<View style={styles.infoBox}>
											<Text style={styles.infoBoxTitle}>ℹ️ Acerca de esta predicción</Text>
											<Text style={styles.infoBoxText}>
												Predicción basada en la media ponderada de las últimas jornadas del jugador, dando más peso a las más recientes. Considera el factor local/visitante y la dificultad del rival.
											</Text>
										</View>
									</>
								) : null}
							</View>
						) : (
							<View style={styles.section}>
								<View style={styles.warningCard}>
									<Text style={styles.warningTitle}>⚠️ Predicción no disponible</Text>
									<Text style={styles.warningText}>
										Este jugador necesita al menos 2 partidos con estadísticas para generar una predicción.
									</Text>
								</View>
							</View>
						)}
					</>
				)}
			</ScrollView>
		</View>
	);
}

export default withAuth(PlayerStats);

const styles = StyleSheet.create({
	container: { flex: 1, backgroundColor: colors.bgApp },

	// ── Hero card ──────────────────────────────────────────────────────────────
	heroCard: {
		backgroundColor: colors.bgCard,
		borderBottomWidth: 1,
		borderBottomColor: colors.border,
		elevation: 3,
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 2 },
		shadowOpacity: 0.08,
		shadowRadius: 4,
	},
	heroPosBar: {
		height: 4,
		width: '100%',
	},
	heroInner: {
		flexDirection: 'row',
		alignItems: 'center',
		paddingHorizontal: spacing.lg,
		paddingVertical: 12,
		gap: 12,
	},
	heroAvatarCol: {
		alignItems: 'center',
	},
	heroMidCol: {
		flex: 1,
		gap: 4,
	},
	heroName: {
		fontSize: fontSize.md,
		fontWeight: fontWeight.extrabold,
		color: colors.textPrimary,
		lineHeight: 20,
	},
	heroMetaRow: {
		flexDirection: 'row',
		alignItems: 'center',
		gap: 6,
		flexWrap: 'wrap',
	},
	heroPosBadge: {
		paddingHorizontal: 7,
		paddingVertical: 2,
		borderRadius: radius.pill,
		borderWidth: 1,
	},
	heroPosBadgeText: {
		fontSize: 10,
		fontWeight: fontWeight.extrabold,
		letterSpacing: 0.5,
	},
	heroTeam: {
		fontSize: fontSize.xs,
		color: colors.textSecondary,
		fontWeight: fontWeight.semibold,
		flexShrink: 1,
	},
	heroValue: {
		fontSize: fontSize.xs,
		color: colors.textMuted,
		fontWeight: fontWeight.semibold,
		marginTop: 2,
	},
	heroRightCol: {
		alignItems: 'flex-end',
		gap: 8,
	},
	heroPtsBadge: {
		alignItems: 'center',
		borderWidth: 2,
		borderRadius: 10,
		paddingHorizontal: 10,
		paddingVertical: 4,
		minWidth: 56,
	},
	heroPtsNum: {
		fontSize: 20,
		fontWeight: fontWeight.black,
		lineHeight: 24,
	},
	heroPtsLabel: {
		fontSize: 9,
		color: colors.textMuted,
		fontWeight: fontWeight.bold,
		letterSpacing: 0.5,
	},
	backBtn: {
		backgroundColor: colors.bgSubtle,
		paddingVertical: 5,
		paddingHorizontal: 10,
		borderRadius: radius.pill,
		borderWidth: 1,
		borderColor: colors.border,
	},
	backBtnText: { color: colors.textSecondary, fontWeight: fontWeight.bold, fontSize: 11 },
	compareBtn: {
		flexDirection: 'row',
		alignItems: 'center',
		gap: 3,
		backgroundColor: colors.successBg,
		paddingVertical: 5,
		paddingHorizontal: 10,
		borderRadius: radius.pill,
		borderWidth: 1,
		borderColor: colors.primaryMuted,
		marginTop: 4,
	},
	compareBtnText: { color: colors.primary, fontWeight: fontWeight.bold, fontSize: 11 },

	// ── Top bar (no-player state) ──────────────────────────────────────────────
	topBar: {
		paddingHorizontal: spacing.lg,
		paddingVertical: spacing.md,
		backgroundColor: colors.primaryDeep,
	},
	topBarTitle: { color: colors.textInverse, fontWeight: fontWeight.extrabold, fontSize: fontSize.lg },

	// ── Tab bar ───────────────────────────────────────────────────────────────
	tabsBar: {
		flexDirection: 'row',
		backgroundColor: colors.bgCard,
		paddingHorizontal: 12,
		paddingVertical: 8,
		gap: 6,
		borderBottomWidth: 1,
		borderBottomColor: colors.border,
	},
	tab: {
		flex: 1,
		alignItems: 'center',
		paddingVertical: 7,
		borderRadius: radius.pill,
		backgroundColor: colors.bgSubtle,
	},
	tabActive: {
		backgroundColor: colors.primaryDeep,
	},
	tabText: {
		fontSize: 12,
		fontWeight: fontWeight.semibold,
		color: colors.textMuted,
	},
	tabTextActive: {
		color: '#fff',
		fontWeight: fontWeight.bold,
	},

	// ── Content & sections ────────────────────────────────────────────────────
	content: { flex: 1 },
	section: { padding: spacing.lg, borderBottomWidth: 1, borderBottomColor: colors.border },
	sectionTitle: { fontSize: fontSize.sm, fontWeight: fontWeight.extrabold, color: colors.textPrimary, marginBottom: spacing.md },
	loadingContainer: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', padding: spacing.xl },
	loadingText: { marginLeft: spacing.md, fontSize: fontSize.sm, color: colors.textMuted, fontWeight: fontWeight.semibold },
	pickerContainer: { borderRadius: radius.md, borderWidth: 1, borderColor: colors.border, overflow: 'hidden', backgroundColor: colors.bgSubtle },
	picker: { height: 50, color: colors.textPrimary },

	// ── Match summary card ────────────────────────────────────────────────────
	matchSummaryCard: {
		backgroundColor: colors.bgSubtle,
		borderRadius: radius.lg,
		borderWidth: 1,
		borderColor: colors.border,
		overflow: 'hidden',
		marginBottom: spacing.lg,
	},
	matchSummaryTop: {
		flexDirection: 'row',
	},
	matchStat: {
		flex: 1,
		alignItems: 'center',
		paddingVertical: 14,
		borderRightWidth: 1,
		borderRightColor: colors.border,
	},
	matchStatPts: {
		backgroundColor: '#F0FDF4',
	},
	matchStatNum: {
		fontSize: 18,
		fontWeight: fontWeight.extrabold,
		color: colors.textPrimary,
	},
	matchStatLabel: {
		fontSize: 10,
		color: colors.textMuted,
		fontWeight: fontWeight.semibold,
		marginTop: 2,
	},

	// ── Stat grid ─────────────────────────────────────────────────────────────
	statsSection: { marginBottom: spacing.lg },
	statsSubtitle: {
		fontSize: fontSize.xs,
		fontWeight: fontWeight.bold,
		color: colors.textSecondary,
		marginBottom: spacing.sm,
		textTransform: 'uppercase',
		letterSpacing: 0.5,
	},
	statGrid2Col: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
	statItem2Col: {
		width: '48%',
		backgroundColor: colors.bgSubtle,
		borderRadius: radius.md,
		padding: spacing.md,
		borderWidth: 1,
		borderColor: colors.border,
		alignItems: 'center',
	},
	statValue: { fontSize: fontSize.lg, fontWeight: fontWeight.extrabold, color: colors.primaryDark },
	statLabel: { fontSize: fontSize.xs, color: colors.textMuted, fontWeight: fontWeight.semibold, marginTop: 2 },

	// ── Season grid ───────────────────────────────────────────────────────────
	seasonGrid: {
		flexDirection: 'row',
		flexWrap: 'wrap',
		gap: spacing.sm,
	},
	seasonStatBox: {
		width: '48%',
		backgroundColor: colors.bgSubtle,
		borderRadius: radius.md,
		padding: 14,
		borderWidth: 1,
		borderColor: colors.border,
		alignItems: 'center',
	},
	seasonStatNum: {
		fontSize: 22,
		fontWeight: fontWeight.black,
		color: colors.textPrimary,
	},
	seasonStatLabel: {
		fontSize: fontSize.xs,
		color: colors.textMuted,
		fontWeight: fontWeight.semibold,
		marginTop: 3,
	},

	// ── Points breakdown ──────────────────────────────────────────────────────
	chipBanner: { backgroundColor: '#d97706', borderRadius: radius.sm, paddingHorizontal: spacing.sm, paddingVertical: 5, marginBottom: spacing.sm, alignSelf: 'flex-start' },
	chipBannerText: { fontSize: fontSize.xs, fontWeight: fontWeight.bold, color: '#fff' },
	breakdownContainer: { backgroundColor: colors.bgSubtle, borderRadius: radius.md, padding: spacing.md, borderWidth: 1, borderColor: colors.border },
	breakdownRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 6, borderBottomWidth: 1, borderBottomColor: colors.border },
	breakdownLabel: { fontSize: fontSize.sm, fontWeight: fontWeight.semibold, color: colors.textSecondary },
	breakdownValue: { fontSize: fontSize.sm, fontWeight: fontWeight.bold },
	breakdownPositive: { color: colors.primary },
	breakdownNegative: { color: colors.danger },
	breakdownTotalRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingTop: 10, marginTop: 4 },
	breakdownTotalLabel: { fontSize: fontSize.sm, fontWeight: fontWeight.extrabold, color: colors.textPrimary },
	breakdownTotalValue: { fontSize: fontSize.lg, fontWeight: fontWeight.black, color: colors.primaryDark },

	// ── Market value tab ──────────────────────────────────────────────────────
	mvSummaryRow: {
		flexDirection: 'row',
		gap: spacing.sm,
	},
	mvSummaryBox: {
		flex: 1,
		backgroundColor: colors.bgSubtle,
		borderRadius: radius.md,
		padding: 12,
		alignItems: 'center',
		borderWidth: 1,
		borderColor: colors.border,
	},
	mvSummaryNum: {
		fontSize: 15,
		fontWeight: fontWeight.extrabold,
		color: colors.textPrimary,
	},
	mvSummaryLabel: {
		fontSize: 10,
		color: colors.textMuted,
		fontWeight: fontWeight.semibold,
		marginTop: 3,
	},
	chartContainer: {
		backgroundColor: colors.bgSubtle,
		borderRadius: radius.lg,
		padding: 12,
		borderWidth: 1,
		borderColor: colors.border,
	},
	chartLegend: {
		fontSize: 10,
		color: colors.textMuted,
		textAlign: 'center',
		marginTop: 4,
	},
	mvHistoryRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: spacing.sm, borderBottomWidth: 1, borderBottomColor: colors.border },
	mvHistoryGameweek: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textSecondary, flex: 1 },
	mvHistoryValue: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textPrimary, width: 80, textAlign: 'right' },
	mvHistoryDelta: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, width: 100, textAlign: 'right' },
	mvPositive: { color: '#16A34A' },
	mvNegative: { color: '#EF4444' },

	// ── Prediction tab ────────────────────────────────────────────────────────
	predictionCard: {
		backgroundColor: '#F0FDF4',
		borderRadius: radius.lg,
		padding: spacing.xl,
		alignItems: 'center',
		borderWidth: 1.5,
		borderColor: '#BBF7D0',
		marginBottom: spacing.lg,
	},
	predictionLabel: { fontSize: fontSize.xs, color: colors.textMuted, fontWeight: fontWeight.bold, letterSpacing: 1, marginBottom: spacing.sm, textAlign: 'center' },
	predictionValue: { fontSize: 56, fontWeight: fontWeight.black, color: colors.primaryDark, marginVertical: spacing.sm },
	predictionRange: { fontSize: fontSize.sm, color: colors.textMuted, marginTop: spacing.sm, fontWeight: fontWeight.medium },
	featureRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: spacing.sm, borderBottomWidth: 1, borderBottomColor: colors.bgSubtle },
	featureName: { width: 120, fontSize: fontSize.sm, fontWeight: fontWeight.semibold, color: colors.textSecondary },
	importanceBarContainer: { flex: 1, height: 8, backgroundColor: colors.border, borderRadius: 4, overflow: 'hidden', marginHorizontal: spacing.sm },
	importanceFill: { height: '100%', backgroundColor: colors.primaryDark, borderRadius: 4 },
	importanceValue: { fontSize: fontSize.xs, fontWeight: fontWeight.bold, color: colors.primaryDark, width: 45, textAlign: 'right' },

	// ── Info & warning cards ──────────────────────────────────────────────────
	infoBox: { backgroundColor: '#EFF6FF', borderRadius: radius.lg, padding: spacing.lg, borderWidth: 1, borderColor: '#BFDBFE', marginTop: spacing.lg },
	infoBoxTitle: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: '#1E40AF', marginBottom: spacing.sm },
	infoBoxText: { fontSize: fontSize.sm, color: '#1E3A8A', lineHeight: 18 },
	warningCard: { backgroundColor: colors.warningBg ?? '#FEF3C7', borderRadius: radius.lg, padding: spacing.lg, borderWidth: 1, borderColor: colors.warning ?? '#F59E0B' },
	warningTitle: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: '#92400E', marginBottom: 4 },
	warningText: { fontSize: fontSize.sm, color: '#78350F', lineHeight: 18 },

	// ── Empty state ───────────────────────────────────────────────────────────
	emptyState: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 32 },
	emptyIcon: { fontSize: 48, marginBottom: 12 },
	emptyText: { fontSize: fontSize.sm, color: colors.textMuted, textAlign: 'center', lineHeight: 20 },
});
