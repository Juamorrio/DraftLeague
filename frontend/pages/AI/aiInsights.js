import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, ActivityIndicator, RefreshControl } from 'react-native';
import { useLeague } from '../../context/LeagueContext';
import authService, { authenticatedFetch } from '../../services/authService';
import predictionService from '../../services/predictionService';
import withAuth from '../../components/withAuth';

const POSITION_GROUPS = [
	{ key: 'POR', label: 'Portero' },
	{ key: 'DEF', label: 'Defensas' },
	{ key: 'MID', label: 'Centrocampistas' },
	{ key: 'DEL', label: 'Delanteros' }
];

const POSITION_COLORS = {
	POR: '#f59e0b',
	DEF: '#3b82f6',
	MID: '#10b981',
	DEL: '#ef4444'
};

const POSITION_LABELS = {
	POR: 'POR',
	DEF: 'DEF',
	MID: 'MED',
	DEL: 'DEL'
};

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

	const loadData = async () => {
		if (!selectedLeague?.id) {
			setTeamError('No hay liga seleccionada');
			setLoadingTeam(false);
			setLoadingMarket(false);
			return;
		}

		setTeamError(null);
		setMarketError(null);
		setLoadingTeam(true);
		setLoadingMarket(true);

		let userId;
		try {
			const user = await authService.getCurrentUser();
			if (!user?.id) {
				setTeamError('Usuario no autenticado');
				setLoadingTeam(false);
				setLoadingMarket(false);
				return;
			}
			userId = user.id;
		} catch {
			setTeamError('Error de autenticacion');
			setLoadingTeam(false);
			setLoadingMarket(false);
			return;
		}

		const [roundResult, teamResult, marketResult] = await Promise.allSettled([
			predictionService.getNextRoundMatches(),
			authenticatedFetch(`/api/v1/teams/league/${selectedLeague.id}/${userId}`).then(r => {
				if (!r.ok) throw new Error('Team fetch failed');
				return r.json();
			}),
			authenticatedFetch(`/api/v1/market?leagueId=${selectedLeague.id}`).then(r => {
				if (!r.ok) throw new Error('Market fetch failed');
				return r.json();
			})
		]);

		if (roundResult.status === 'fulfilled' && roundResult.value) {
			setNextRoundInfo(roundResult.value);
		}

		let teamData = null;
		if (teamResult.status === 'fulfilled') {
			teamData = teamResult.value;
			setTeam(teamData);
		} else {
			setTeamError('No se pudo cargar tu equipo');
		}
		setLoadingTeam(false);

		let marketData = [];
		if (marketResult.status === 'fulfilled') {
			marketData = Array.isArray(marketResult.value) ? marketResult.value : [];
			setMarketPlayers(marketData);
		} else {
			setMarketError('No se pudieron cargar los jugadores del mercado');
		}
		setLoadingMarket(false);

		if (teamData?.playerTeams?.length > 0) {
			setLoadingPlayerPredictions(true);
			const playerIds = teamData.playerTeams.map(pt => pt.player?.id).filter(Boolean);
			const predResults = await Promise.allSettled(
				playerIds.map(pid => predictionService.predictPlayerPoints(pid))
			);
			const pMap = {};
			playerIds.forEach((pid, i) => {
				if (predResults[i].status === 'fulfilled' && predResults[i].value) {
					pMap[pid] = predResults[i].value;
				}
			});
			setPlayerPredictions(pMap);
			setLoadingPlayerPredictions(false);
		}

		if (marketData.length > 0) {
			setLoadingMarketPredictions(true);
			const mPlayerIds = marketData.map(mp => mp.player?.id).filter(Boolean);
			const mPredResults = await Promise.allSettled(
				mPlayerIds.map(pid => predictionService.predictPlayerPoints(pid))
			);
			const mMap = {};
			mPlayerIds.forEach((pid, i) => {
				if (mPredResults[i].status === 'fulfilled' && mPredResults[i].value) {
					mMap[pid] = mPredResults[i].value;
				}
			});
			setMarketPredictions(mMap);
			setLoadingMarketPredictions(false);
		}

		setRefreshing(false);
	};

	useEffect(() => {
		loadData();
	}, [selectedLeague?.id]);

	const onRefresh = () => {
		setRefreshing(true);
		loadData();
	};

	const getPositionColor = (pos) => POSITION_COLORS[pos] || '#6b7280';

	const formatConfidence = (interval) => {
		if (!interval || interval.length < 2) return null;
		return `${interval[0]} - ${interval[1]} pts`;
	};

	if (loadingTeam && !team) {
		return (
			<View style={styles.container}>
				<View style={styles.topBar}>
					<Text style={styles.header}>Análisis Estadístico</Text>
				</View>
				<View style={styles.centerContent}>
					<ActivityIndicator size="large" color="#1a5c3a" />
					<Text style={styles.loadingText}>Calculando probabilidades...</Text>
				</View>
			</View>
		);
	}

	if (!selectedLeague?.id) {
		return (
			<View style={styles.container}>
				<View style={styles.topBar}>
					<Text style={styles.header}>Análisis Estadístico</Text>
				</View>
				<View style={styles.centerContent}>
					<Text style={styles.errorText}>Selecciona una liga primero</Text>
				</View>
			</View>
		);
	}

	const renderPlayerPredRow = (playerTeam) => {
		const player = playerTeam.player;
		if (!player) return null;
		const prediction = playerPredictions[player.id];
		const pos = player.position || 'MID';

		return (
			<View key={player.id} style={styles.playerPredRow}>
				<View style={[styles.positionDot, { backgroundColor: getPositionColor(pos) }]} />
				<View style={styles.playerInfo}>
					<Text style={styles.playerName}>{player.fullName || player.name}</Text>
					<Text style={styles.playerMeta}>
						{POSITION_LABELS[pos] || pos}
						{player.totalPoints != null ? ` | ${player.totalPoints} pts totales` : ''}
					</Text>
				</View>
				<View style={styles.predictionCol}>
					{prediction ? (
						<>
							<Text style={styles.predPoints}>{prediction.predictedPoints?.toFixed(1)}</Text>
							{prediction.confidenceInterval && (
								<Text style={styles.predConfidence}>
									{formatConfidence(prediction.confidenceInterval)}
								</Text>
							)}
						</>
					) : (
						<Text style={styles.noPrediction}>Sin datos</Text>
					)}
				</View>
			</View>
		);
	};

	const renderMarketPlayerRow = (mp) => {
		const player = mp.player;
		if (!player) return null;
		const prediction = marketPredictions[player.id];
		const pos = player.position || 'MID';

		return (
			<View key={mp.id} style={styles.marketPlayerCard}>
				<View style={[styles.positionDot, { backgroundColor: getPositionColor(pos) }]} />
				<View style={styles.playerInfo}>
					<Text style={styles.playerName}>{player.fullName || player.name}</Text>
					<Text style={styles.playerMeta}>
						{POSITION_LABELS[pos] || pos} | {player.marketValue?.toLocaleString()} coins
					</Text>
					{mp.auctionEndTime && (
						<Text style={styles.auctionTime}>
							Cierre: {new Date(mp.auctionEndTime).toLocaleDateString('es-ES', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })}
						</Text>
					)}
				</View>
				<View style={styles.predictionCol}>
					{prediction ? (
						<>
							<Text style={styles.predPoints}>{prediction.predictedPoints?.toFixed(1)}</Text>
							{prediction.confidenceInterval && (
								<Text style={styles.predConfidence}>
									{formatConfidence(prediction.confidenceInterval)}
								</Text>
							)}
						</>
					) : (
						<Text style={styles.noPrediction}>Sin datos</Text>
					)}
				</View>
			</View>
		);
	};

	const playersByPosition = {};
	if (team?.playerTeams) {
		team.playerTeams.forEach(pt => {
			const pos = pt.player?.position || 'MID';
			if (!playersByPosition[pos]) playersByPosition[pos] = [];
			playersByPosition[pos].push(pt);
		});
		Object.keys(playersByPosition).forEach(pos => {
			playersByPosition[pos].sort((a, b) => {
				const predA = playerPredictions[a.player?.id]?.predictedPoints || 0;
				const predB = playerPredictions[b.player?.id]?.predictedPoints || 0;
				return predB - predA;
			});
		});
	}

	const sortedMarketPlayers = [...marketPlayers].sort((a, b) => {
		const predA = marketPredictions[a.player?.id]?.predictedPoints;
		const predB = marketPredictions[b.player?.id]?.predictedPoints;
		if (predA != null && predB != null) return predB - predA;
		if (predA != null) return -1;
		if (predB != null) return 1;
		return 0;
	});

	return (
		<View style={styles.container}>
			<View style={styles.topBar}>
				<Text style={styles.header}>Análisis Inteligente</Text>
				<Text style={styles.subtitle}>
					{nextRoundInfo?.round ? `Jornada ${nextRoundInfo.round} - ` : ''}{selectedLeague?.name}
				</Text>
			</View>

			<ScrollView
				style={styles.scrollView}
				contentContainerStyle={styles.scrollContent}
				refreshControl={
					<RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={['#1a5c3a']} />
				}
			>
				{/* === SECTION 1: Team Player Predictions === */}
				<View style={styles.section}>
					<Text style={styles.sectionTitle}>Tu Equipo</Text>
					<Text style={styles.sectionSubtitle}>Expectativa de puntos según Poisson y Elo</Text>

					{teamError ? (
						<View style={styles.errorCard}>
							<Text style={styles.errorCardText}>{teamError}</Text>
							<TouchableOpacity style={styles.retryButton} onPress={loadData}>
								<Text style={styles.retryButtonText}>Reintentar</Text>
							</TouchableOpacity>
						</View>
					) : (
						<>
							{loadingPlayerPredictions && (
								<ActivityIndicator size="small" color="#1a5c3a" style={{ marginBottom: 12 }} />
							)}
							{POSITION_GROUPS.map(group => {
								const players = playersByPosition[group.key];
								if (!players || players.length === 0) return null;
								return (
									<View key={group.key}>
										<Text style={[styles.positionGroupHeader, { color: getPositionColor(group.key) }]}>
											{group.label}
										</Text>
										{players.map(renderPlayerPredRow)}
									</View>
								);
							})}
							{team && (!team.playerTeams || team.playerTeams.length === 0) && (
								<Text style={styles.emptyText}>No tienes jugadores en tu equipo</Text>
							)}
						</>
					)}
				</View>

				{/* === SECTION 2: Total Team Prediction === */}
				<View style={styles.section}>
					<Text style={styles.sectionTitle}>Proyección Total</Text>
					<Text style={styles.sectionSubtitle}>Suma algorítmica de tu alineación</Text>

					{loadingPlayerPredictions ? (
						<View style={styles.predictionCard}>
							<ActivityIndicator size="large" color="#1a5c3a" />
						</View>
					) : teamError ? (
						<View style={styles.warningCard}>
							<Text style={styles.warningText}>Carga tu equipo para ver la proyección</Text>
						</View>
					) : Object.keys(playerPredictions).length > 0 ? (
						<>
							<View style={styles.predictionCard}>
								<View style={styles.mainPrediction}>
									<Text style={styles.predictionLabel}>PUNTOS ESPERADOS</Text>
									<Text style={styles.predictionValue}>
										{Object.values(playerPredictions)
											.reduce((sum, p) => sum + (p.predictedPoints || 0), 0)
											.toFixed(1)}
									</Text>
								</View>
							</View>

							{/* Breakdown by position */}
							<View style={styles.breakdownGrid}>
								{POSITION_GROUPS.map(group => {
									const posPlayerTeams = (team?.playerTeams || []).filter(
										pt => (pt.player?.position || 'MID') === group.key
									);
									if (posPlayerTeams.length === 0) return null;
									const totalPos = posPlayerTeams.reduce((sum, pt) => {
										const pred = playerPredictions[pt.player?.id];
										return sum + (pred?.predictedPoints || 0);
									}, 0);
									return (
										<View key={group.key} style={styles.breakdownCard}>
											<View style={[styles.breakdownDot, { backgroundColor: getPositionColor(group.key) }]} />
											<Text style={styles.breakdownPosition}>{group.key}</Text>
											<Text style={styles.breakdownTotal}>{totalPos.toFixed(1)}</Text>
											<Text style={styles.breakdownLabel}>pts</Text>
										</View>
									);
								})}
							</View>
						</>
					) : team ? (
						<View style={styles.warningCard}>
							<Text style={styles.warningText}>Sin proyecciones disponibles</Text>
						</View>
					) : null}
				</View>

				{/* === SECTION 3: Market Predictions === */}
				<View style={styles.section}>
					<Text style={styles.sectionTitle}>Análisis de Mercado</Text>
					<Text style={styles.sectionSubtitle}>Jugadores con mayor potencial para esta jornada</Text>

					{loadingMarket ? (
						<ActivityIndicator size="small" color="#1a5c3a" />
					) : marketError ? (
						<View style={styles.errorCard}>
							<Text style={styles.errorCardText}>{marketError}</Text>
						</View>
					) : sortedMarketPlayers.length === 0 ? (
						<Text style={styles.emptyText}>No hay jugadores en el mercado</Text>
					) : (
						<>
							{loadingMarketPredictions && (
								<ActivityIndicator size="small" color="#1a5c3a" style={{ marginBottom: 12 }} />
							)}
							{sortedMarketPlayers.map(renderMarketPlayerRow)}
						</>
					)}
				</View>
			</ScrollView>
		</View>
	);
}

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: '#f9fafb'
	},
	topBar: {
		backgroundColor: '#1a5c3a',
		paddingHorizontal: 16,
		paddingVertical: 16,
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 2 },
		shadowOpacity: 0.1,
		shadowRadius: 4,
		elevation: 3
	},
	header: {
		color: '#fff',
		fontSize: 22,
		fontWeight: '800',
		marginBottom: 4
	},
	subtitle: {
		color: '#d1fae5',
		fontSize: 13,
		fontWeight: '600'
	},
	scrollView: {
		flex: 1
	},
	scrollContent: {
		padding: 16,
		paddingBottom: 32
	},
	centerContent: {
		flex: 1,
		justifyContent: 'center',
		alignItems: 'center',
		padding: 32
	},
	loadingText: {
		marginTop: 16,
		fontSize: 15,
		color: '#6b7280',
		fontWeight: '600'
	},
	errorText: {
		fontSize: 15,
		color: '#dc2626',
		textAlign: 'center',
		marginBottom: 16
	},
	retryButton: {
		backgroundColor: '#1a5c3a',
		paddingHorizontal: 20,
		paddingVertical: 10,
		borderRadius: 8,
		alignSelf: 'center'
	},
	retryButtonText: {
		color: '#fff',
		fontWeight: '700',
		fontSize: 14
	},

	// Sections
	section: {
		backgroundColor: '#fff',
		borderRadius: 12,
		padding: 16,
		marginBottom: 16,
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 1 },
		shadowOpacity: 0.05,
		shadowRadius: 3,
		elevation: 2
	},
	sectionTitle: {
		fontSize: 18,
		fontWeight: '800',
		color: '#111827',
		marginBottom: 2
	},
	sectionSubtitle: {
		fontSize: 13,
		color: '#6b7280',
		marginBottom: 14
	},

	// Position group
	positionGroupHeader: {
		fontSize: 13,
		fontWeight: '700',
		marginTop: 14,
		marginBottom: 6,
		paddingLeft: 4
	},

	// Player prediction row
	playerPredRow: {
		flexDirection: 'row',
		alignItems: 'center',
		paddingVertical: 10,
		paddingHorizontal: 4,
		borderBottomWidth: 1,
		borderBottomColor: '#f3f4f6'
	},
	positionDot: {
		width: 8,
		height: 8,
		borderRadius: 4,
		marginRight: 10
	},
	playerInfo: {
		flex: 1
	},
	playerName: {
		fontSize: 14,
		fontWeight: '700',
		color: '#111827',
		marginBottom: 2
	},
	playerMeta: {
		fontSize: 11,
		color: '#6b7280',
		fontWeight: '500'
	},
	predictionCol: {
		alignItems: 'flex-end',
		minWidth: 70
	},
	predPoints: {
		fontSize: 18,
		fontWeight: '800',
		color: '#1a5c3a'
	},
	predConfidence: {
		fontSize: 10,
		color: '#6b7280',
		fontWeight: '500'
	},
	noPrediction: {
		fontSize: 11,
		fontStyle: 'italic',
		color: '#9ca3af'
	},

	// Team prediction card
	predictionCard: {
		backgroundColor: '#f0fdf4',
		borderRadius: 12,
		padding: 20,
		borderWidth: 2,
		borderColor: '#1a5c3a',
		alignItems: 'center'
	},
	mainPrediction: {
		alignItems: 'center'
	},
	predictionLabel: {
		fontSize: 11,
		color: '#6b7280',
		fontWeight: '700',
		letterSpacing: 1,
		marginBottom: 6
	},
	predictionValue: {
		fontSize: 52,
		fontWeight: '900',
		color: '#1a5c3a',
		marginBottom: 6
	},
	confidenceText: {
		fontSize: 13,
		color: '#059669',
		fontWeight: '600'
	},

	// Position breakdown
	breakdownGrid: {
		flexDirection: 'row',
		flexWrap: 'wrap',
		gap: 10,
		marginTop: 14
	},
	breakdownCard: {
		flex: 1,
		minWidth: '22%',
		backgroundColor: '#f9fafb',
		padding: 12,
		borderRadius: 8,
		alignItems: 'center'
	},
	breakdownDot: {
		width: 6,
		height: 6,
		borderRadius: 3,
		marginBottom: 6
	},
	breakdownPosition: {
		fontSize: 12,
		fontWeight: '800',
		color: '#374151',
		marginBottom: 4
	},
	breakdownTotal: {
		fontSize: 20,
		fontWeight: '900',
		color: '#1a5c3a',
		marginBottom: 2
	},
	breakdownLabel: {
		fontSize: 10,
		color: '#6b7280'
	},

	// Market player card
	marketPlayerCard: {
		flexDirection: 'row',
		alignItems: 'center',
		padding: 12,
		borderRadius: 8,
		borderWidth: 1,
		borderColor: '#e5e7eb',
		backgroundColor: '#f9fafb',
		marginBottom: 8
	},
	auctionTime: {
		fontSize: 10,
		color: '#9ca3af',
		marginTop: 2
	},

	// Warning / Error cards
	warningCard: {
		backgroundColor: '#fef3c7',
		borderWidth: 1,
		borderColor: '#fbbf24',
		borderRadius: 8,
		padding: 14
	},
	warningText: {
		fontSize: 13,
		color: '#92400e',
		textAlign: 'center'
	},
	errorCard: {
		backgroundColor: '#fef2f2',
		borderWidth: 1,
		borderColor: '#fca5a5',
		borderRadius: 8,
		padding: 14
	},
	errorCardText: {
		fontSize: 13,
		color: '#dc2626',
		textAlign: 'center',
		marginBottom: 10
	},
	emptyText: {
		fontSize: 14,
		color: '#9ca3af',
		textAlign: 'center',
		paddingVertical: 20
	}
});

export default withAuth(AIInsights);
