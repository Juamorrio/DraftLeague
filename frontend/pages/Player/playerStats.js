import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, ActivityIndicator } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { useLeague } from '../../context/LeagueContext';
import { authenticatedFetch } from '../../services/authService';
import withAuth from '../../components/withAuth';

function PlayerStats() {
	const { selectedPlayer, setNavTarget, selectedLeague } = useLeague();
	const [playerPrediction, setPlayerPrediction] = useState(null);
	const [loadingPrediction, setLoadingPrediction] = useState(false);
	const [statistics, setStatistics] = useState([]);
	const [loadingStats, setLoadingStats] = useState(false);
	const [activeTab, setActiveTab] = useState('stats');
	const [selectedRound, setSelectedRound] = useState(null);

	useEffect(() => {
		if (!selectedPlayer?.id) {
			setPlayerPrediction(null);
			return;
		}

		const loadPlayerPrediction = async () => {
			setLoadingPrediction(true);
			try {
				const response = await authenticatedFetch(`/api/ml/predict/player/${selectedPlayer.id}`);

				if (response.ok) {
					const prediction = await response.json();
					console.log('Prediccion de jugador cargada:', prediction);
					setPlayerPrediction(prediction);
				} else {
					console.log('No se pudo cargar la prediccion del jugador');
				}
			} catch (e) {
				console.log('Error al cargar prediccion de jugador:', e);
			} finally {
				setLoadingPrediction(false);
			}
		};

		loadPlayerPrediction();
	}, [selectedPlayer?.id]);

	// Cargar estadísticas históricas del jugador
	useEffect(() => {
		if (!selectedPlayer?.id || !selectedLeague?.id) {
			setStatistics([]);
			return;
		}

		const loadStatistics = async () => {
			setLoadingStats(true);
			try {
				// El ID del jugador viene como string desde el backend, usarlo tal cual
				const playerId = selectedPlayer.id;
				const response = await authenticatedFetch(`/api/statistics/player/${playerId}/matches`);

				if (response.ok) {
					const jornadas = await response.json();
					console.log('📊 Jornadas recibidas:', jornadas);

					// Convertir estructura anidada a lista plana
					let flatStats = [];
					if (Array.isArray(jornadas)) {
						jornadas.forEach(jornada => {
							if (jornada.matches && Array.isArray(jornada.matches)) {
								jornada.matches.forEach(match => {
									flatStats.push({
										...match,
										// Asegurar que tenemos los campos correctos del backend
										totalFantasyPoints: match.fantasyPoints || 0,
										match: { round: match.round }
									});
								});
							}
						});
					}
					console.log('📊 Estadísticas procesadas:', flatStats);
					setStatistics(flatStats);
				} else {
					console.warn('⚠️ Error al cargar estadísticas:', response.status);
				}
			} catch (e) {
				console.error('Error cargando estadísticas:', e);
			} finally {
				setLoadingStats(false);
			}
		};

		loadStatistics();
	}, [selectedPlayer?.id, selectedLeague?.id]);

	const translateFeature = (featureName) => {
		const translations = {
			avgRatingLast3: 'Rating últimos 3',
			avgMinutesLast3: 'Minutos últimos 3',
			avgGoalsLast3: 'Goles últimos 3',
			avgAssistsLast3: 'Asistencias últimos 3',
			avgXgLast3: 'xG últimos 3',
			avgXaLast3: 'xA últimos 3',
			avgPassAccuracyLast3: 'Precisión pases últimos 3',
			avgTouchesLast3: 'Toques últimos 3',
			avgDuelsWonLast3: 'Duelos ganados últimos 3',
			avgRatingLast5: 'Rating últimos 5',
			avgMinutesLast5: 'Minutos últimos 5',
			avgGoalsLast5: 'Goles últimos 5',
			avgAssistsLast5: 'Asistencias últimos 5',
			avgXgLast5: 'xG últimos 5',
			avgXaLast5: 'xA últimos 5',
			avgRatingLast10: 'Rating últimos 10',
			avgMinutesLast10: 'Minutos últimos 10',
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
					<Text style={styles.header}>Estadísticas de Jugador</Text>
				</View>
				<View style={styles.emptyState}>
					<Text style={styles.emptyText}>Selecciona un jugador para ver sus estadísticas</Text>
				</View>
			</View>
		);
	}

	return (
		<View style={styles.container}>
			<View style={styles.topBar}>
				<Text style={styles.header}>
					{selectedPlayer.fullName || selectedPlayer.name}
				</Text>
				<TouchableOpacity
					style={styles.backBtn}
					onPress={() => setNavTarget('team')}
				>
					<Text style={styles.backBtnText}>Volver</Text>
				</TouchableOpacity>
			</View>

			{/* Tabs */}
			<View style={styles.tabsContainer}>
				<TouchableOpacity
					style={[styles.tab, activeTab === 'stats' && styles.tabActive]}
					onPress={() => setActiveTab('stats')}
				>
					<Text style={[styles.tabText, activeTab === 'stats' && styles.tabTextActive]}>
						📊 Estadísticas
					</Text>
				</TouchableOpacity>
				<TouchableOpacity
					style={[styles.tab, activeTab === 'prediction' && styles.tabActive]}
					onPress={() => setActiveTab('prediction')}
				>
					<Text style={[styles.tabText, activeTab === 'prediction' && styles.tabTextActive]}>
						🔮 Predicción
					</Text>
				</TouchableOpacity>
			</View>

			<ScrollView style={styles.content}>
				{/* Información básica del jugador (siempre visible) */}
				<View style={styles.section}>
					<View style={styles.playerInfoCard}>
						<View style={styles.infoRow}>
							<Text style={styles.infoLabel}>Posición:</Text>
							<Text style={styles.infoValue}>{selectedPlayer.position}</Text>
						</View>
						{selectedPlayer.team && (
							<View style={styles.infoRow}>
								<Text style={styles.infoLabel}>Equipo:</Text>
								<Text style={styles.infoValue}>{selectedPlayer.team.name}</Text>
							</View>
						)}
						{selectedPlayer.marketValue && (
							<View style={styles.infoRow}>
								<Text style={styles.infoLabel}>Valor:</Text>
								<Text style={styles.infoValue}>
									€{selectedPlayer.marketValue.toLocaleString('es-ES')}
								</Text>
							</View>
						)}
						{selectedPlayer.totalPoints !== undefined && (
							<View style={styles.infoRow}>
								<Text style={styles.infoLabel}>Puntos Totales:</Text>
								<Text style={styles.infoValue}>{selectedPlayer.totalPoints}</Text>
							</View>
						)}
					</View>
				</View>

				{/* TAB: ESTADÍSTICAS */}
				{activeTab === 'stats' && (
					<>
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
												value={stat.round}
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
									const selectedStat = statistics.find(s => s.round === selectedRound);
									if (!selectedStat) return null;

									return (
										<View style={styles.section}>
											<Text style={styles.sectionTitle}>
												Jornada {selectedStat.round} - vs {selectedStat.opponent}
											</Text>

											{/* Card de resumen general */}
											<View style={styles.detailCard}>
												<View style={styles.detailRow}>
													<Text style={styles.detailLabel}>Resultado:</Text>
													<Text style={styles.detailValue}>
														{selectedStat.isHomeTeam ? '🏠' : '🚌'} {selectedStat.goalsScored} - {selectedStat.goalsConceded}
													</Text>
												</View>
												<View style={styles.detailRow}>
													<Text style={styles.detailLabel}>Minutos:</Text>
													<Text style={styles.detailValue}>{selectedStat.minutesPlayed}'</Text>
												</View>
												<View style={styles.detailRow}>
													<Text style={styles.detailLabel}>Rating:</Text>
													<Text style={styles.detailValue}>{selectedStat.fotmobRating?.toFixed(2) || '-'}</Text>
												</View>
												<View style={styles.detailRow}>
													<Text style={styles.detailLabel}>Puntos Fantasy:</Text>
													<Text style={[styles.detailValue, styles.pointsHighlight]}>
														{selectedStat.fantasyPoints}
													</Text>
												</View>
											</View>

											{/* Desglose de Puntos Fantasy */}
											{selectedStat.pointsBreakdown && Object.keys(selectedStat.pointsBreakdown).length > 1 && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>Desglose de Puntos Fantasy</Text>
													<View style={styles.breakdownContainer}>
														{Object.entries(selectedStat.pointsBreakdown)
															.filter(([key]) => key !== 'total')
															.map(([key, value]) => {
																const labels = {
																	minutesPlayed: 'Minutos jugados',
																	goals: 'Goles',
																	assists: 'Asistencias',
																	ratingBonus: 'Bonus por rating',
																	yellowCards: 'Tarjetas amarillas',
																	redCards: 'Tarjetas rojas',
																	defensiveActions: 'Acciones defensivas',
																	duelsBonus: 'Bonus duelos ganados',
																	dispossessedPenalty: 'Perdidas de balon',
																	cleanSheet: 'Porteria a cero',
																	hatTrick: 'Bonus hat-trick',
																	penaltySaved: 'Penalti parado',
																	doubleAssist: 'Bonus doble asistencia',
																	tripleAssist: 'Bonus triple asistencia',
																	multipleGoalsConceded: 'Goles encajados (3+)',
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
											{(selectedStat.goals > 0 || selectedStat.assists > 0 || selectedStat.expectedGoals !== undefined) && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>⚽ Estadísticas Ofensivas</Text>
													<View style={styles.statGrid2Col}>
														{selectedStat.goals > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Goles</Text>
																<Text style={styles.statValue}>{selectedStat.goals}</Text>
															</View>
														)}
														{selectedStat.assists > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Asistencias</Text>
																<Text style={styles.statValue}>{selectedStat.assists}</Text>
															</View>
														)}
														{selectedStat.expectedGoals !== undefined && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>xG</Text>
																<Text style={styles.statValue}>{selectedStat.expectedGoals?.toFixed(2) || '-'}</Text>
															</View>
														)}
														{selectedStat.expectedAssists !== undefined && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>xA</Text>
																<Text style={styles.statValue}>{selectedStat.expectedAssists?.toFixed(2) || '-'}</Text>
															</View>
														)}
														{selectedStat.chancesCreated !== undefined && selectedStat.chancesCreated > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Ocasiones</Text>
																<Text style={styles.statValue}>{selectedStat.chancesCreated}</Text>
															</View>
														)}
														{selectedStat.totalShots !== undefined && selectedStat.totalShots > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Tiros</Text>
																<Text style={styles.statValue}>{selectedStat.totalShots}</Text>
															</View>
														)}
													</View>
												</View>
											)}

											{/* Estadísticas de pase y posesión */}
											{(selectedStat.totalPasses !== undefined || selectedStat.accuratePasses !== undefined || selectedStat.totalCrosses !== undefined) && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>🎯 Pase y Posesión</Text>
													<View style={styles.statGrid2Col}>
														{selectedStat.totalPasses !== undefined && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Pases</Text>
																<Text style={styles.statValue}>{selectedStat.totalPasses}</Text>
															</View>
														)}
														{selectedStat.totalPasses !== undefined && selectedStat.accuratePasses !== undefined && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Precisión</Text>
																<Text style={styles.statValue}>
																	{((selectedStat.accuratePasses / selectedStat.totalPasses) * 100).toFixed(0)}%
																</Text>
															</View>
														)}
														{selectedStat.totalCrosses !== undefined && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Centros</Text>
																<Text style={styles.statValue}>{selectedStat.totalCrosses}</Text>
															</View>
														)}
														{selectedStat.touches !== undefined && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Toques</Text>
																<Text style={styles.statValue}>{selectedStat.touches}</Text>
															</View>
														)}
													</View>
												</View>
											)}

											{/* Estadísticas defensivas */}
											{(selectedStat.tackles !== undefined || selectedStat.interceptions !== undefined || selectedStat.clearances !== undefined || selectedStat.blocks !== undefined) && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>🛡️ Defensa</Text>
													<View style={styles.statGrid2Col}>
														{selectedStat.tackles !== undefined && selectedStat.tackles > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Entradas</Text>
																<Text style={styles.statValue}>{selectedStat.tackles}</Text>
															</View>
														)}
														{selectedStat.interceptions !== undefined && selectedStat.interceptions > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Intercepciones</Text>
																<Text style={styles.statValue}>{selectedStat.interceptions}</Text>
															</View>
														)}
														{selectedStat.clearances !== undefined && selectedStat.clearances > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Despejes</Text>
																<Text style={styles.statValue}>{selectedStat.clearances}</Text>
															</View>
														)}
														{selectedStat.blocks !== undefined && selectedStat.blocks > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Bloqueos</Text>
																<Text style={styles.statValue}>{selectedStat.blocks}</Text>
															</View>
														)}
													</View>
												</View>
											)}

											{/* Duelos */}
											{(selectedStat.duelsWon !== undefined || selectedStat.duelsLost !== undefined) && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>⚔️ Duelos</Text>
													<View style={styles.statGrid2Col}>
														{selectedStat.duelsWon !== undefined && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Ganados</Text>
																<Text style={styles.statValue}>{selectedStat.duelsWon}</Text>
															</View>
														)}
														{selectedStat.duelsLost !== undefined && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Perdidos</Text>
																<Text style={styles.statValue}>{selectedStat.duelsLost}</Text>
															</View>
														)}
														{selectedStat.duelsWon !== undefined && selectedStat.duelsLost !== undefined && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Ganador %</Text>
																<Text style={styles.statValue}>
																	{selectedStat.duelsWon + selectedStat.duelsLost > 0
																		? ((selectedStat.duelsWon / (selectedStat.duelsWon + selectedStat.duelsLost)) * 100).toFixed(0) + '%'
																		: '-'
																	}
																</Text>
															</View>
														)}
													</View>
												</View>
											)}

											{/* Portero - Estadísticas especiales */}
											{(selectedStat.saves !== undefined || selectedStat.goalkeeperSaves !== undefined) && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>🥅 Portero</Text>
													<View style={styles.statGrid2Col}>
														{(selectedStat.saves !== undefined || selectedStat.goalkeeperSaves !== undefined) && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Paradas</Text>
																<Text style={styles.statValue}>{selectedStat.saves || selectedStat.goalkeeperSaves || '-'}</Text>
															</View>
														)}
														{selectedStat.goalsAllowed !== undefined && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Goles Recibidos</Text>
																<Text style={styles.statValue}>{selectedStat.goalsAllowed}</Text>
															</View>
														)}
													</View>
												</View>
											)}

											{/* Tarjetas */}
											{(selectedStat.yellowCards !== undefined || selectedStat.redCards !== undefined) && (
												<View style={styles.statsSection}>
													<Text style={styles.statsSubtitle}>🟥 Disciplina</Text>
													<View style={styles.statGrid2Col}>
														{selectedStat.yellowCards !== undefined && selectedStat.yellowCards > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Amarillas</Text>
																<Text style={styles.statValue}>{selectedStat.yellowCards}</Text>
															</View>
														)}
														{selectedStat.redCards !== undefined && selectedStat.redCards > 0 && (
															<View style={styles.statItem2Col}>
																<Text style={styles.statLabel}>Rojas</Text>
																<Text style={styles.statValue}>{selectedStat.redCards}</Text>
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

						{/* Resumen general de toda la temporada */}
						{statistics.length > 0 && (
							<View style={styles.section}>
								<Text style={styles.sectionTitle}>📈 Resumen de Temporada</Text>
								<View style={styles.summaryCard}>
									<View style={styles.summaryRow}>
										<Text style={styles.summaryLabel}>Partidos Jugados:</Text>
										<Text style={styles.summaryValue}>{statistics.length}</Text>
									</View>
									<View style={styles.summaryRow}>
										<Text style={styles.summaryLabel}>Rating Promedio:</Text>
										<Text style={styles.summaryValue}>
											{(statistics.reduce((sum, s) => sum + (s.fotmobRating || 0), 0) / statistics.length).toFixed(2)}
										</Text>
									</View>
									<View style={styles.summaryRow}>
										<Text style={styles.summaryLabel}>Goles Totales:</Text>
										<Text style={styles.summaryValue}>
											{statistics.reduce((sum, s) => sum + (s.goals || 0), 0)}
										</Text>
									</View>
									<View style={styles.summaryRow}>
										<Text style={styles.summaryLabel}>Asistencias Totales:</Text>
										<Text style={styles.summaryValue}>
											{statistics.reduce((sum, s) => sum + (s.assists || 0), 0)}
										</Text>
									</View>
									<View style={styles.summaryRow}>
										<Text style={styles.summaryLabel}>Minutos Totales:</Text>
										<Text style={styles.summaryValue}>
											{statistics.reduce((sum, s) => sum + (s.minutesPlayed || 0), 0)}
										</Text>
									</View>
									<View style={styles.summaryRow}>
										<Text style={styles.summaryLabel}>Puntos Acumulados:</Text>
										<Text style={[styles.summaryValue, styles.summaryValueBold]}>
											{statistics.reduce((sum, s) => sum + (s.fantasyPoints || 0), 0)}
										</Text>
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

				{/* TAB: PREDICCIÓN */}
				{activeTab === 'prediction' && (
					<>
						{(playerPrediction || loadingPrediction) ? (
							<View style={styles.section}>
								<Text style={styles.sectionTitle}>🔮 Predicción del ML</Text>

								{loadingPrediction ? (
									<View style={styles.loadingContainer}>
										<ActivityIndicator size="small" color="#1a5c3a" />
										<Text style={styles.loadingText}>Generando predicción...</Text>
									</View>
								) : playerPrediction ? (
									<>
										{/* Puntos predichos - Destacado */}
										<View style={styles.predictionCard}>
											<Text style={styles.predictionLabel}>PUNTOS PREDICHOS PARA PRÓXIMO PARTIDO</Text>
											<Text style={styles.predictionValue}>
												{playerPrediction.predictedPoints?.toFixed(1) || '0.0'}
											</Text>
											{playerPrediction.confidenceInterval && playerPrediction.confidenceInterval.length === 2 && (
												<Text style={styles.predictionRange}>
													Intervalo de confianza: {playerPrediction.confidenceInterval[0]}-{playerPrediction.confidenceInterval[1]} puntos
												</Text>
											)}
										</View>

										{/* Factores clave */}
										{playerPrediction.featuresImportance && Object.keys(playerPrediction.featuresImportance).length > 0 && (
											<View style={styles.section}>
												<Text style={styles.sectionTitle}>⭐ Factores Más Importantes</Text>
												<View style={styles.featuresSection}>
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
											</View>
										)}

										{/* Información sobre la predicción */}
										<View style={styles.section}>
											<View style={styles.infoBox}>
												<Text style={styles.infoBoxTitle}>ℹ️ Acerca de esta predicción</Text>
												<Text style={styles.infoBoxText}>
													Esta predicción se basa en un modelo Random Forest entrenado con datos históricos del jugador. Considera métricas como rating, minutos jugados, goles, asistencias y forma reciente.
												</Text>
												<Text style={styles.infoBoxText}>
													El rango de confianza (95%) indica la variabilidad esperada en la predicción.
												</Text>
											</View>
										</View>
									</>
								) : null}
							</View>
						) : (
							<View style={styles.section}>
								<View style={styles.warningCard}>
									<Text style={styles.warningTitle}>⚠️ Predicción no disponible</Text>
									<Text style={styles.warningText}>
										Este jugador necesita al menos 3 partidos con estadísticas para generar una predicción del ML.
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
	container: {
		flex: 1,
		backgroundColor: '#ffffff'
	},
	topBar: {
		flexDirection: 'row',
		justifyContent: 'space-between',
		alignItems: 'center',
		paddingHorizontal: 16,
		paddingVertical: 12,
		backgroundColor: '#1a5c3a'
	},
	header: {
		color: '#fff',
		fontWeight: '800',
		fontSize: 18,
		flex: 1
	},
	backBtn: {
		backgroundColor: '#111827',
		paddingVertical: 8,
		paddingHorizontal: 16,
		borderRadius: 8
	},
	backBtnText: {
		color: '#fff',
		fontWeight: '700',
		fontSize: 14
	},
	// Tabs
	tabsContainer: {
		flexDirection: 'row',
		borderBottomWidth: 2,
		borderBottomColor: '#e5e7eb',
		backgroundColor: '#ffffff'
	},
	tab: {
		flex: 1,
		paddingVertical: 16,
		paddingHorizontal: 12,
		alignItems: 'center',
		borderBottomWidth: 3,
		borderBottomColor: 'transparent'
	},
	tabActive: {
		borderBottomColor: '#1a5c3a'
	},
	tabText: {
		fontSize: 14,
		fontWeight: '600',
		color: '#9ca3af'
	},
	tabTextActive: {
		color: '#1a5c3a',
		fontWeight: '700'
	},
	content: {
		flex: 1
	},
	section: {
		padding: 16,
		borderBottomWidth: 1,
		borderBottomColor: '#e5e7eb'
	},
	sectionTitle: {
		fontSize: 16,
		fontWeight: '800',
		color: '#111827',
		marginBottom: 12
	},
	subsectionTitle: {
		fontSize: 14,
		fontWeight: '700',
		color: '#374151',
		marginBottom: 8,
		marginTop: 12
	},
	playerInfoCard: {
		backgroundColor: '#f9fafb',
		borderRadius: 12,
		padding: 16,
		borderWidth: 1,
		borderColor: '#e5e7eb'
	},
	infoRow: {
		flexDirection: 'row',
		justifyContent: 'space-between',
		paddingVertical: 8,
		borderBottomWidth: 1,
		borderBottomColor: '#e5e7eb'
	},
	infoLabel: {
		fontSize: 14,
		color: '#6b7280',
		fontWeight: '600'
	},
	infoValue: {
		fontSize: 14,
		color: '#111827',
		fontWeight: '700'
	},
	loadingContainer: {
		flexDirection: 'row',
		alignItems: 'center',
		justifyContent: 'center',
		padding: 20
	},
	loadingText: {
		marginLeft: 12,
		fontSize: 13,
		color: '#6b7280',
		fontWeight: '600'
	},
	// ESTADÍSTICAS TAB
	statsContainer: {
		gap: 8
	},
	pickerContainer: {
		borderRadius: 8,
		borderWidth: 1,
		borderColor: '#e5e7eb',
		overflow: 'hidden',
		backgroundColor: '#f9fafb'
	},
	picker: {
		height: 50,
		color: '#111827'
	},
	detailCard: {
		backgroundColor: '#f9fafb',
		borderRadius: 8,
		padding: 12,
		borderWidth: 1,
		borderColor: '#e5e7eb',
		marginBottom: 16
	},
	detailRow: {
		flexDirection: 'row',
		justifyContent: 'space-between',
		paddingVertical: 8,
		borderBottomWidth: 1,
		borderBottomColor: '#e5e7eb'
	},
	detailLabel: {
		fontSize: 13,
		fontWeight: '600',
		color: '#6b7280'
	},
	detailValue: {
		fontSize: 13,
		fontWeight: '700',
		color: '#111827'
	},
	pointsHighlight: {
		color: '#1a5c3a',
		fontSize: 16
	},
	// Breakdown de puntos
	breakdownContainer: {
		backgroundColor: '#f9fafb',
		borderRadius: 8,
		padding: 12,
		borderWidth: 1,
		borderColor: '#e5e7eb'
	},
	breakdownRow: {
		flexDirection: 'row',
		justifyContent: 'space-between',
		alignItems: 'center',
		paddingVertical: 6,
		borderBottomWidth: 1,
		borderBottomColor: '#f3f4f6'
	},
	breakdownLabel: {
		fontSize: 13,
		fontWeight: '600',
		color: '#374151'
	},
	breakdownValue: {
		fontSize: 14,
		fontWeight: '700'
	},
	breakdownPositive: {
		color: '#16a34a'
	},
	breakdownNegative: {
		color: '#dc2626'
	},
	breakdownTotalRow: {
		flexDirection: 'row',
		justifyContent: 'space-between',
		alignItems: 'center',
		paddingTop: 10,
		marginTop: 4
	},
	breakdownTotalLabel: {
		fontSize: 14,
		fontWeight: '800',
		color: '#111827'
	},
	breakdownTotalValue: {
		fontSize: 18,
		fontWeight: '900',
		color: '#1a5c3a'
	},
	statsSection: {
		marginBottom: 16
	},
	statsSubtitle: {
		fontSize: 13,
		fontWeight: '700',
		color: '#374151',
		marginBottom: 10,
		paddingHorizontal: 4
	},
	statGrid2Col: {
		flexDirection: 'row',
		flexWrap: 'wrap',
		gap: 8
	},
	statItem2Col: {
		width: '48%',
		backgroundColor: '#f9fafb',
		borderRadius: 6,
		padding: 10,
		borderWidth: 1,
		borderColor: '#e5e7eb',
		alignItems: 'center'
	},
	statLabel: {
		fontSize: 10,
		color: '#6b7280',
		fontWeight: '600',
		marginBottom: 6
	},
	statValue: {
		fontSize: 16,
		fontWeight: '700',
		color: '#1a5c3a'
	},
	statCard: {
		backgroundColor: '#f9fafb',
		borderRadius: 8,
		padding: 12,
		borderWidth: 1,
		borderColor: '#e5e7eb'
	},
	statHeader: {
		flexDirection: 'row',
		justifyContent: 'space-between',
		alignItems: 'center',
		marginBottom: 12
	},
	statMatchInfo: {
		fontSize: 12,
		fontWeight: '700',
		color: '#374151'
	},
	statPoints: {
		fontSize: 14,
		fontWeight: '800',
		color: '#1a5c3a'
	},
	statGrid: {
		flexDirection: 'row',
		flexWrap: 'wrap',
		gap: 8
	},
	statGridItem: {
		width: '48%',
		backgroundColor: '#ffffff',
		borderRadius: 6,
		padding: 8,
		borderWidth: 1,
		borderColor: '#e5e7eb',
		alignItems: 'center'
	},
	statGridLabel: {
		fontSize: 10,
		color: '#6b7280',
		fontWeight: '600',
		marginBottom: 4
	},
	statGridValue: {
		fontSize: 16,
		fontWeight: '700',
		color: '#1a5c3a'
	},
	// RESUMEN
	summaryCard: {
		backgroundColor: '#f9fafb',
		borderRadius: 12,
		padding: 16,
		borderWidth: 1,
		borderColor: '#e5e7eb'
	},
	summaryRow: {
		flexDirection: 'row',
		justifyContent: 'space-between',
		paddingVertical: 10,
		borderBottomWidth: 1,
		borderBottomColor: '#e5e7eb'
	},
	summaryLabel: {
		fontSize: 13,
		fontWeight: '600',
		color: '#6b7280'
	},
	summaryValue: {
		fontSize: 14,
		fontWeight: '700',
		color: '#111827'
	},
	summaryValueBold: {
		color: '#1a5c3a',
		fontSize: 16
	},
	// PREDICCIÓN TAB
	predictionCard: {
		backgroundColor: '#f0fdf4',
		borderRadius: 12,
		padding: 20,
		alignItems: 'center',
		borderWidth: 2,
		borderColor: '#1a5c3a',
		marginBottom: 16
	},
	predictionLabel: {
		fontSize: 11,
		color: '#6b7280',
		fontWeight: '700',
		letterSpacing: 1,
		marginBottom: 8
	},
	predictionValue: {
		fontSize: 56,
		fontWeight: '900',
		color: '#1a5c3a',
		marginVertical: 8
	},
	predictionRange: {
		fontSize: 12,
		color: '#6b7280',
		marginTop: 8,
		fontWeight: '500'
	},
	featuresSection: {
		marginTop: 12
	},
	featureRow: {
		flexDirection: 'row',
		alignItems: 'center',
		paddingVertical: 10,
		borderBottomWidth: 1,
		borderBottomColor: '#f3f4f6'
	},
	featureName: {
		width: 120,
		fontSize: 12,
		fontWeight: '600',
		color: '#374151'
	},
	importanceBarContainer: {
		flex: 1,
		height: 10,
		backgroundColor: '#e5e7eb',
		borderRadius: 5,
		overflow: 'hidden',
		marginHorizontal: 8
	},
	importanceFill: {
		height: '100%',
		backgroundColor: '#1a5c3a',
		borderRadius: 5
	},
	importanceValue: {
		fontSize: 11,
		fontWeight: '700',
		color: '#1a5c3a',
		width: 45,
		textAlign: 'right'
	},
	infoBox: {
		backgroundColor: '#eff6ff',
		borderRadius: 12,
		padding: 16,
		borderWidth: 1,
		borderColor: '#bfdbfe'
	},
	infoBoxTitle: {
		fontSize: 14,
		fontWeight: '700',
		color: '#1e40af',
		marginBottom: 8
	},
	infoBoxText: {
		fontSize: 12,
		color: '#1e3a8a',
		lineHeight: 18,
		marginBottom: 6
	},
	warningCard: {
		backgroundColor: '#fef3c7',
		borderRadius: 12,
		padding: 16,
		borderWidth: 1,
		borderColor: '#fbbf24'
	},
	warningTitle: {
		fontSize: 14,
		fontWeight: '700',
		color: '#92400e',
		marginBottom: 4
	},
	warningText: {
		fontSize: 12,
		color: '#78350f',
		lineHeight: 18
	},
	emptyState: {
		flex: 1,
		justifyContent: 'center',
		alignItems: 'center',
		padding: 32
	},
	emptyText: {
		fontSize: 14,
		color: '#6b7280',
		textAlign: 'center'
	}
});
