import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, Modal, FlatList, Alert, ScrollView, ActivityIndicator } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { useLeague } from '../../context/LeagueContext';
import pitch from '../../assets/Team/pitch.png';
import Player from '../../components/player';
import authService, { authenticatedFetch } from '../../services/authService';
import predictionService from '../../services/predictionService';
import { LineChartComponent } from '../../components/StatisticsChart';
import withAuth from '../../components/withAuth';


function Team({ userId: viewUserId = null, readOnly = false }) {
	const { selectedLeague, viewUser, setViewUser, setNavTarget, setSelectedPlayer } = useLeague();
	const [players, setPlayers] = useState([]);
	const [loading, setLoading] = useState(false);
	const [assigned, setAssigned] = useState({});
	const [pickerVisible, setPickerVisible] = useState(false);
	const [optionsVisible, setOptionsVisible] = useState(false);
	const [selectedPT, setSelectedPT] = useState(null);
	const [myBudget, setMyBudget] = useState(null);
	const [currentKey, setCurrentKey] = useState(null);
	const [error, setError] = useState(null);
	const [saving, setSaving] = useState(false);
	const [formation, setFormation] = useState('4-3-3');
	const [selectedPlayerSlot, setSelectedPlayerSlot] = useState(null);
	const [teamPrediction, setTeamPrediction] = useState(null);
	const [loadingPrediction, setLoadingPrediction] = useState(false);
	const [selectedGameweek, setSelectedGameweek] = useState('total');
	const [playerGameweekPoints, setPlayerGameweekPoints] = useState({});
	const [gameweekSnapshots, setGameweekSnapshots] = useState([]);
	const [maxGameweek] = useState(38);
	const [loadingPoints, setLoadingPoints] = useState(false);
	const [captainPlayerId, setCaptainPlayerId] = useState(null);
	const [pointsHistory, setPointsHistory] = useState(null);


	const formations = {
		'4-3-3': {
			name: '4-3-3',
			positions: [
				{ key: 'GK', x: '45%', y: '88%', role: 'POR' },
				{ key: 'LB', x: '8%', y: '68%', role: 'DEF' },
				{ key: 'CB1', x: '32%', y: '68%', role: 'DEF' },
				{ key: 'CB2', x: '58%', y: '68%', role: 'DEF' },
				{ key: 'RB', x: '82%', y: '68%', role: 'DEF' },
				{ key: 'CM1', x: '23%', y: '48%', role: 'MID' },
				{ key: 'CM2', x: '67%', y: '48%', role: 'MID' },
				{ key: 'CAM', x: '45%', y: '35%', role: 'MID' },
				{ key: 'LW', x: '13%', y: '18%', role: 'DEL' },
				{ key: 'RW', x: '77%', y: '18%', role: 'DEL' },
				{ key: 'ST', x: '45%', y: '8%', role: 'DEL' },
				{ key: 'COACH', x: '80%', y: '88%', role: 'COACH' },
			]
		},
	};

	const currentFormation = formations[formation];
	const positions = currentFormation.positions;

	const historicalAssigned = useMemo(() => {
		if (selectedGameweek === 'total' || gameweekSnapshots.length === 0) return null;

		const positionSlots = { POR: [], DEF: [], MID: [], DEL: [] };
		const slotKeys = {
			POR: ['GK'],
			DEF: ['LB', 'CB1', 'CB2', 'RB'],
			MID: ['CM1', 'CM2', 'CAM'],
			DEL: ['LW', 'RW', 'ST'],
		};

		gameweekSnapshots.forEach(s => {
			const pos = s.position;
			if (positionSlots[pos]) {
				positionSlots[pos].push(s);
			}
		});

		const result = {};
		Object.entries(slotKeys).forEach(([pos, keys]) => {
			const players = positionSlots[pos] || [];
			keys.forEach((key, i) => {
				if (i < players.length) {
					const s = players[i];
					result[key] = {
						player: {
							id: s.playerId,
							fullName: s.playerName,
							name: s.playerName,
							position: s.position,
							totalPoints: s.gameweekPoints || 0,
							avatarUrl: s.avatarUrl || null,
						}
					};
				}
			});
		});

		return result;
	}, [selectedGameweek, gameweekSnapshots]);

	if (!viewUserId && viewUser?.id) {
		viewUserId = viewUser.id;
		readOnly = true;
	}

	useEffect(() => {
		let mounted = true;
		(async () => {
			if (!selectedLeague?.id) {
				if (mounted) setPlayers([]);
				return;
			}
			setLoading(true); setError(null);
			try {
				if (readOnly) {
					if (mounted) setPlayers([]);
				} else {
					const res = await authenticatedFetch(`/api/v1/players?leagueId=${selectedLeague.id}&onlyOwned=true`);
				const json = await res.json();
				if (mounted) {
					setPlayers(Array.isArray(json) ? json : (json?.content ?? []));
				}
				}
			} catch (e) {
				if (mounted) setError('No se pudieron cargar jugadores');
			} finally {
				if (mounted) setLoading(false);
			}
		})();
		return () => { mounted = false; };
		
	}, [selectedLeague?.id]);

	const loadTeam = async () => {
		if (!selectedLeague?.id) return;
		let mounted = true;
		try {
			let userIdToUse = viewUserId;
			if (!userIdToUse) {
				const user = await authService.getCurrentUser();
				if (!user?.id) return;
				userIdToUse = user.id;
			}
			const res = await authenticatedFetch(`/api/v1/teams/league/${selectedLeague.id}/${userIdToUse}`);
			if (res.ok) {
				const team = await res.json();
				if (mounted && team?.playerTeams) {
					const newAssigned = {};						
					const playersByRole = {
						POR: [],
						DEF: [],
						MID: [],
						DEL: [],
						COACH: []
					};	
					team.playerTeams.forEach(pt => {
						const role = pt.player.position;
						if (playersByRole[role]) {
							playersByRole[role].push(pt);
						}
					});
					
					const slotsByRole = {
						POR: [],
						DEF: [],
						MID: [],
						DEL: [],
						COACH: []
					};
					
					currentFormation.positions.forEach(pos => {
						if (slotsByRole[pos.role]) {
							slotsByRole[pos.role].push(pos.key);
						}
					});
					
					Object.entries(playersByRole).forEach(([role, players]) => {
						const slots = slotsByRole[role] || [];
						players.forEach((pt, index) => {
							if (index < slots.length) {
								newAssigned[slots[index]] = pt;
							}
						});
					});

					const captain = team.playerTeams.find(pt => pt.isCaptain);
					setCaptainPlayerId(captain ? captain.player.id : null);

					setAssigned(newAssigned);

					if (team.id) {
						predictionService.getTeamPointsHistory(team.id)
							.then(history => {
								if (history && history.history?.length > 0) {
									const chartData = {
										labels: history.history.map(h => `J${h.gameweek}`),
										datasets: [{
											data: history.history.map(h => h.points)
										}]
									};
									setPointsHistory(chartData);
								}
							})
							.catch(err => console.log('Error cargando historial de puntos:', err));
					}
				}
			}
		} catch (e) {
			console.error('Error cargando equipo:', e);
		}
	};

	const loadPlayerGameweekPoints = async (teamId, gameweek) => {
		if (gameweek === 'total') {
			setPlayerGameweekPoints({});
			setGameweekSnapshots([]);
			return;
		}

		try {
			setLoadingPoints(true);
			const res = await authenticatedFetch(
				`/api/v1/fantasy-points/teams/${teamId}/gameweek/${gameweek}/players`
			);
			if (!res.ok) throw new Error('Error loading gameweek points');

			const data = await res.json();
			const pointsMap = {};
			data.forEach(p => {
				pointsMap[p.playerId] = p.gameweekPoints || 0;
			});
			setPlayerGameweekPoints(pointsMap);
			setGameweekSnapshots(data);
		} catch (e) {
			console.error('Error loading gameweek points:', e);
			setPlayerGameweekPoints({});
			setGameweekSnapshots([]);
		} finally {
			setLoadingPoints(false);
		}
	};

	useEffect(() => {
		loadTeam();
	}, [selectedLeague?.id, formation]);

	useEffect(() => {
		if (selectedGameweek === 'total') {
			setPlayerGameweekPoints({});
			setGameweekSnapshots([]);
			return;
		}

		(async () => {
			if (!selectedLeague?.id) return;

			try {
				let userIdToUse = viewUserId;
				if (!userIdToUse) {
					const user = await authService.getCurrentUser();
					if (!user?.id) return;
					userIdToUse = user.id;
				}

				const res = await authenticatedFetch(`/api/v1/teams/league/${selectedLeague.id}/${userIdToUse}`);
				if (res.ok) {
					const team = await res.json();
					if (team?.id) {
						await loadPlayerGameweekPoints(team.id, selectedGameweek);
					}
				}
			} catch (e) {
				console.error('Error loading team for points:', e);
			}
		})();
	}, [selectedGameweek, selectedLeague?.id, viewUserId]);

	useEffect(() => {
		const loadTeamPrediction = async () => {
			if (!selectedLeague?.id || Object.keys(assigned).length === 0) {
				setTeamPrediction(null);
				return;
			}

			setLoadingPrediction(true);
			try {
				const firstAssigned = Object.values(assigned).find(pt => pt?.team?.id);
				if (!firstAssigned) return;

				const teamId = firstAssigned.team.id;

				const response = await authenticatedFetch(`/api/ml/predict/team/${teamId}`);
				if (response.ok) {
					const prediction = await response.json();
					console.log('Prediccion de equipo cargada:', prediction);
					setTeamPrediction(prediction);
				} else {
					console.log('No se pudo cargar la prediccion del equipo');
				}
			} catch (e) {
				console.log('Error al cargar prediccion de equipo:', e);
			} finally {
				setLoadingPrediction(false);
			}
		};

		loadTeamPrediction();
	}, [selectedLeague?.id, assigned]);


	const loadMyBudget = async () => {
		if (!selectedLeague?.id) return;
		try {
			const user = await authService.getCurrentUser();
			if (!user?.id) return;
			const res = await authenticatedFetch(`/api/v1/teams/league/${selectedLeague.id}/${user.id}`);
			if (res.ok) {
				const team = await res.json();
				setMyBudget(team?.budget ?? null);
			}
		} catch {}
	};

	useEffect(() => { loadMyBudget(); }, [selectedLeague?.id]);

	const getPlayerPoints = (player) => {
		if (selectedGameweek === 'total') {
			return player.totalPoints ?? 0;
		} else {
			return playerGameweekPoints[player.id] ?? 0;
		}
	};

	const openPicker = (key) => { if (readOnly) return; setCurrentKey(key); setPickerVisible(true); };

	const getAssignedPlayer = (slotValue) => {
		if (!slotValue) return null;
		return slotValue.player ?? slotValue;
	};

	const openOptions = (pt, slotKey) => {
		if (!pt) return;
		setSelectedPT(pt);
		setSelectedPlayerSlot(slotKey);
		setOptionsVisible(true);
	};

	const goToPlayerStats = (player) => {
		if (!player?.id) return;
		setSelectedPlayer(player);
		setOptionsVisible(false);
		setNavTarget('playerStats');
	};

	const buyout = async (pt) => {
		if (!selectedLeague?.id || !viewUser?.id || !pt?.player?.id) return;
		try {
			const res = await authenticatedFetch(`/api/v1/teams/league/${selectedLeague.id}/buyout`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ sellerUserId: viewUser.id, playerId: pt.player.id })
			});
			if (res.ok) {
				let data = null; try { data = await res.json(); } catch {}
				if (data && typeof data.budget === 'number') setMyBudget(data.budget);
				Alert.alert('Clausulazo realizado', 'El jugador ha sido transferido a tu equipo.');
				await loadTeam();
			} else {
				const data = await res.json().catch(() => ({}));
				Alert.alert('Error', data?.error || 'No se pudo realizar el clausulazo');
			}
		} catch (e) {
			Alert.alert('Error', e?.message || 'Fallo al ejecutar clausulazo');
		}
	};

	const sellPlayer = async (pt) => {
		if (!selectedLeague?.id || !pt?.player?.id) return;
		try {
			const res = await authenticatedFetch(`/api/v1/teams/league/${selectedLeague.id}/sell-player`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ playerId: pt.player.id })
			});
			if (res.ok) {
				const data = await res.json();
				if (typeof data.budget === 'number') setMyBudget(data.budget);
				const price = data.sellPrice || 0;
				Alert.alert('Jugador vendido', `${pt.player.fullName || pt.player.name} vendido por €${price.toLocaleString('es-ES')}`);
				if (captainPlayerId === pt.player.id) setCaptainPlayerId(null);
				await loadTeam();
			} else {
				const data = await res.json().catch(() => ({}));
				Alert.alert('Error', data?.error || 'No se pudo vender el jugador');
			}
		} catch (e) {
			Alert.alert('Error', e?.message || 'Fallo al vender jugador');
		}
	};
	
	const assignPlayer = (player) => {
		if (!currentKey) return;
		setAssigned(prev => ({ ...prev, [currentKey]: { player } }));
		setPickerVisible(false);
		setCurrentKey(null);
	};

	const saveTeam = async () => {
		if (!selectedLeague?.id) {
			Alert.alert('Error', 'Selecciona una liga primero');
			return;
		}

		const playersList = Object.entries(assigned)
			.map(([position, slotValue]) => {
				const p = getAssignedPlayer(slotValue);
				if (!p?.id) return null;
				return {
					playerId: p.id,
			position: position,
			lined: true,
			isCaptain: p.id === captainPlayerId
				};
			})
			.filter(Boolean);

		if (playersList.length === 0) {
			Alert.alert('Error', 'Debes seleccionar al menos un jugador');
			return;
		}

		setSaving(true);
		try {
			const user = await authService.getCurrentUser();
			if (!user?.id) {
				Alert.alert('Error', 'Usuario no autenticado');
				setSaving(false);
				return;
			}
			const res = await authenticatedFetch(`/api/v1/teams/league/${selectedLeague.id}/${user.id}/players`, {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ players: playersList })
			});

			if (res.ok) {
				Alert.alert('Éxito', 'Equipo guardado correctamente');
			} else {
				const data = await res.json();
				Alert.alert('Error', data?.error || 'No se pudo guardar el equipo');
			}
		} catch (e) {
			Alert.alert('Error', 'Error al guardar: ' + (e?.message || 'Desconocido'));
		} finally {
			setSaving(false);
		}
	};

	function matchesSpot(playerPos, key) {
		if (!playerPos || !key) return true;
		
		const positionInfo = currentFormation.positions.find(p => p.key === key);
		if (!positionInfo) return false;
		
		return playerPos === positionInfo.role;
	}

	return (
		<View style={styles.container}>
			<View style={styles.topBar}>
				<Text style={styles.header}>{readOnly && viewUser?.name ? `Equipo de ${viewUser.name}` : (selectedLeague ? `Equipo · ${selectedLeague.name}` : 'Equipo')}</Text>
				{readOnly && (
					<TouchableOpacity 
						style={styles.backBtn}
						onPress={() => { setViewUser(null); setNavTarget('league'); }}
					>
						<Text style={styles.backBtnText}>Volver</Text>
					</TouchableOpacity>
				)}
				{!readOnly && (
					<TouchableOpacity 
						style={[styles.saveBtn, saving && styles.saveBtnDisabled]} 
						onPress={saveTeam}
						disabled={saving}
					>
						<Text style={styles.saveBtnText}>{saving ? 'Guardando...' : 'Guardar'}</Text>
					</TouchableOpacity>
				)}
			</View>

			{!readOnly && (
				<View style={styles.pickerContainer}>
					<Text style={styles.pickerLabel}>Ver puntos:</Text>
					<Picker
						selectedValue={selectedGameweek}
						style={styles.picker}
						onValueChange={(value) => setSelectedGameweek(value)}
					>
						<Picker.Item label="Total Acumulado" value="total" />
						{Array.from({ length: maxGameweek }, (_, i) => i + 1).map(gw => (
							<Picker.Item key={gw} label={`Jornada ${gw}`} value={gw} />
						))}
					</Picker>
				</View>
			)}

			{selectedGameweek !== 'total' && historicalAssigned && (
				<View style={styles.historicalBanner}>
					<Text style={styles.historicalBannerText}>
						Jornada {selectedGameweek} - Alineacion historica
					</Text>
				</View>
			)}

			<ScrollView style={styles.scrollView} contentContainerStyle={styles.scrollContent}>
				<View style={styles.fieldContainer}>
					<Image source={pitch} style={styles.fieldImage} />
					{positions.map(p => {
						const displaySource = historicalAssigned || assigned;
						const slotData = displaySource[p.key];
						const isHistorical = !!historicalAssigned;
						return (
						<View key={p.key} style={[styles.spot, { left: p.x, top: p.y }]}>
						{slotData ? (
							(() => {
								const ap = getAssignedPlayer(slotData);
								if (!ap) return null;
								const displayName = (ap.fullName ?? ap.name ?? '').trim();
								return (
									<Player
										name={displayName}
										avatar={ap.avatarUrl ? { uri: ap.avatarUrl } : null}
										size={50}
										teamId={ap.teamId}
										points={getPlayerPoints(ap)}
										isCaptain={captainPlayerId === ap.id}
										onPress={isHistorical ? undefined : () => openOptions(slotData, p.key)}
									/>
								);
							})()
						) : (
							readOnly || isHistorical ? null : (
								<TouchableOpacity style={styles.spotBtn} activeOpacity={0.7} onPress={() => openPicker(p.key)}>
									<Text style={styles.plus}>+</Text>
								</TouchableOpacity>
							)
						)}
						{!slotData && !readOnly && !isHistorical && <Text style={styles.spotLabel}>{p.key}</Text>}
					</View>
					);
				})}
				</View>

				{/* Historial de Puntos */}
				{pointsHistory && (
					<View style={styles.chartSection}>
						<LineChartComponent 
							data={pointsHistory} 
							title="Evolución de Puntos" 
						/>
					</View>
				)}

				{/* Predicción ML del Equipo */}
				{(teamPrediction || loadingPrediction) && (
					<View style={styles.predictionSection}>
						{loadingPrediction ? (
							<View style={styles.loadingContainer}>
								<ActivityIndicator size="small" color="#1a5c3a" />
								<Text style={styles.loadingText}>Cargando predicción del equipo...</Text>
							</View>
						) : (
							<ScrollView horizontal showsHorizontalScrollIndicator={false}>
								<View style={styles.predictionContent}>
									{/* Total Predicho */}
									<View style={styles.totalPredictionCard}>
										<Text style={styles.predictionCardTitle}>🔮 Predicción Próxima J.</Text>
										<Text style={styles.totalPredictionValue}>
											{teamPrediction.totalPredictedPoints?.toFixed(1) || '0.0'}
										</Text>
										<Text style={styles.predictionLabel}>PUNTOS ESPERADOS</Text>
									</View>

									{/* Top 3 Jugadores */}
									{teamPrediction.players && teamPrediction.players.length > 0 && (
										<View style={styles.topPlayersCard}>
											<Text style={styles.predictionCardTitle}>⭐ Top Rendimiento</Text>
											{teamPrediction.players
												.sort((a, b) => (b.predictedPoints || 0) - (a.predictedPoints || 0))
												.slice(0, 3)
												.map((player, index) => (
													<View key={player.playerId} style={styles.playerPredictionRow}>
														<View style={styles.playerRank}>
															<Text style={styles.rankNumber}>{index + 1}</Text>
														</View>
														<View style={styles.playerInfo}>
															<Text style={styles.playerPredictionName} numberOfLines={1}>
																{player.playerName}
															</Text>
															<Text style={styles.playerPosition}>{player.position}</Text>
														</View>
														<View style={styles.playerPredictionPoints}>
															<Text style={styles.pointsValue}>
																{player.predictedPoints?.toFixed(1) || '0.0'}
															</Text>
															<Text style={styles.pointsLabel}>pts</Text>
														</View>
													</View>
												))}
										</View>
									)}
								</View>
							</ScrollView>
						)}
					</View>
				)}
			</ScrollView>

			{!readOnly && (
				<Modal visible={pickerVisible} transparent animationType="fade" onRequestClose={() => setPickerVisible(false)}>
				<View style={styles.modalBackdrop}>
					<View style={styles.modalCard}>
						<Text style={styles.modalTitle}>Selecciona jugador para {currentKey}</Text>
						{loading && <Text>Cargando jugadores…</Text>}
						{error && <Text style={{ color: 'red' }}>{error}</Text>}
						{!loading && !error && (
							<FlatList
								data={players.filter(sp => matchesSpot(sp.position, currentKey) && !Object.values(assigned).some(a => (getAssignedPlayer(a)?.id) === sp.id) )}
								keyExtractor={(item) => String(item.id ?? `${item.name}-${item.position}`)}
								ItemSeparatorComponent={() => <View style={{ height: 6 }} />}
								renderItem={({ item }) => (
									<TouchableOpacity style={styles.pickRow} onPress={() => assignPlayer(item)}>
										<Text style={styles.pickName}>{item.fullName ?? item.name}</Text>
									<Text style={styles.pickMeta}>{item.position} · {(item.totalPoints ?? 0)} pts · €{((item.marketValue ?? 0)).toLocaleString('es-ES')}</Text>
									</TouchableOpacity>
								)}
							/>
						)}
						<TouchableOpacity style={styles.closeBtn} onPress={() => setPickerVisible(false)}>
							<Text style={styles.closeText}>Cerrar</Text>
						</TouchableOpacity>
					</View>
				</View>
				</Modal>
			)}

			<Modal visible={optionsVisible} transparent animationType="fade" onRequestClose={() => setOptionsVisible(false)}>
				<View style={styles.modalBackdrop}>
					<View style={styles.modalCard}>
						<Text style={styles.modalTitle}>Acciones: {selectedPT?.player ? (selectedPT.player.fullName ?? selectedPT.player.name ?? '') : ''}</Text>
						{selectedPT && (
							<View style={{ gap: 8 }}>
								<TouchableOpacity 
									style={[styles.saveBtn, { backgroundColor: '#2563eb' }]} 
									onPress={() => goToPlayerStats(selectedPT.player)}
								>
									<Text style={styles.saveBtnText}>Ver Estadísticas</Text>
								</TouchableOpacity>
								{!readOnly && selectedPlayerSlot && (
									<TouchableOpacity
										style={[styles.saveBtn, { backgroundColor: '#16a34a' }]}
										onPress={() => {
											setOptionsVisible(false);
											openPicker(selectedPlayerSlot);
										}}
									>
										<Text style={styles.saveBtnText}>Cambiar Jugador</Text>
									</TouchableOpacity>
								)}
								{!readOnly && selectedPT?.player && (
									(() => {
										const player = selectedPT.player;
										const marketValue = player.marketValue || 0;
										const minPrice = Math.round(marketValue * 0.9);
										const maxPrice = Math.round(marketValue * 1.1);
										const isCaptain = captainPlayerId === player.id;
										return (
											<View style={{ gap: 8 }}>
												<View style={{ backgroundColor: '#f3f4f6', padding: 8, borderRadius: 8 }}>
													<Text style={{ fontSize: 12, color: '#374151', fontWeight: '600' }}>
														Valor de mercado: {marketValue.toLocaleString('es-ES')}
													</Text>
													<Text style={{ fontSize: 11, color: '#6b7280' }}>
														Precio de venta: entre {minPrice.toLocaleString('es-ES')} y {maxPrice.toLocaleString('es-ES')}
													</Text>
												</View>
												<TouchableOpacity
													style={[styles.saveBtn, { backgroundColor: '#dc2626' }]}
													onPress={() => {
														Alert.alert(
															'Vender jugador',
															`Vas a vender a ${player.fullName || player.name} por un precio entre ${minPrice.toLocaleString('es-ES')} y ${maxPrice.toLocaleString('es-ES')}. El precio exacto se determina al confirmar.`,
															[
																{ text: 'Cancelar', style: 'cancel' },
																{ text: 'Vender', style: 'destructive', onPress: () => { setOptionsVisible(false); sellPlayer(selectedPT); } },
															]
														);
													}}
												>
													<Text style={styles.saveBtnText}>Vender al Mercado</Text>
												</TouchableOpacity>
												<TouchableOpacity
													style={[styles.saveBtn, { backgroundColor: isCaptain ? '#9ca3af' : '#f59e0b' }]}
													onPress={() => {
														if (isCaptain) {
															setCaptainPlayerId(null);
														} else {
															setCaptainPlayerId(player.id);
														}
														setOptionsVisible(false);
													}}
												>
													<Text style={styles.saveBtnText}>{isCaptain ? 'Quitar Capitan' : 'Elegir Capitan (x2 pts)'}</Text>
												</TouchableOpacity>
											</View>
										);
									})()
								)}
								{readOnly && (
									(() => {
										const price = (selectedPT.buyPrice ?? selectedPT.sellPrice ?? 0);
										const budgetKnown = typeof myBudget === 'number';
										const remaining = budgetKnown ? (myBudget - price) : null;
										const handleClausulazoPress = () => {
											if (budgetKnown) {
												if (remaining < 0) {
													Alert.alert(
														'Presupuesto insuficiente',
														`Tu presupuesto: €${myBudget.toLocaleString('es-ES')} · tras compra: €${remaining.toLocaleString('es-ES')}`
													);
													return;
												}
												Alert.alert(
													'Confirmar clausulazo',
													`Tu presupuesto: €${myBudget.toLocaleString('es-ES')} · tras compra: €${remaining.toLocaleString('es-ES')}`,
													[
														{ text: 'Cancelar', style: 'cancel' },
														{ text: 'Confirmar', onPress: () => { setOptionsVisible(false); buyout(selectedPT); } },
													]
												);
											} else {
												Alert.alert(
													'Confirmar clausulazo',
													`Precio cláusula: €${price.toLocaleString('es-ES')}`,
													[
														{ text: 'Cancelar', style: 'cancel' },
														{ text: 'Confirmar', onPress: () => { setOptionsVisible(false); buyout(selectedPT); } },
													]
												);
											}
										};
										return (
											<View style={{ gap: 4 }}>
												<Text>Precio cláusula: €{price.toLocaleString('es-ES')}</Text>
												<TouchableOpacity style={[styles.saveBtn]} onPress={handleClausulazoPress}>
													<Text style={styles.saveBtnText}>Clausulazo</Text>
												</TouchableOpacity>
											</View>
										);
									})())
								}
							</View>
						)}
						<TouchableOpacity style={styles.closeBtn} onPress={() => setOptionsVisible(false)}>
							<Text style={styles.closeText}>Cerrar</Text>
						</TouchableOpacity>
					</View>
				</View>
			</Modal>
		</View>
	);
}
export default withAuth(Team);

const styles = StyleSheet.create({
	container: { flex: 1, backgroundColor: '#ffffffff' },
	topBar: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 12, backgroundColor: '#1a5c3a' },
	header: { color: '#fff', fontWeight: '800', fontSize: 18 },
	saveBtn: { backgroundColor: '#4CAF50', paddingVertical: 8, paddingHorizontal: 16, borderRadius: 8 },
	saveBtnDisabled: { backgroundColor: '#ccc' },
	saveBtnText: { color: '#fff', fontWeight: '700', fontSize: 14 },
	backBtn: { backgroundColor: '#111827', paddingVertical: 8, paddingHorizontal: 16, borderRadius: 8 },
	backBtnText: { color: '#fff', fontWeight: '700', fontSize: 14 },
	scrollView: { flex: 1 },
	scrollContent: { paddingBottom: 20 },
	fieldContainer: { height: 500, position: 'relative' },
	fieldImage: { width: '100%', height: '100%' },
	spot: { position: 'absolute', transform: [{ translateX: -25 }, { translateY: -25 }], alignItems: 'center' },
	spotBtn: { width: 50, height: 50, borderRadius: 18, backgroundColor: 'rgba(255,255,255,0.2)', borderWidth: 2, borderColor: '#000000ff', alignItems: 'center', justifyContent: 'center' },
	plus: { color: '#000000ff', fontWeight: '800', fontSize: 20, lineHeight: 22 },
	spotLabel: { marginTop: 4, color: '#000000ff', fontSize: 10, fontWeight: '700' },
	playerCard: { minWidth: 140 },
	modalBackdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', alignItems: 'center', justifyContent: 'center' },
	modalCard: { width: '86%', maxHeight: '70%', backgroundColor: '#fff', borderRadius: 12, padding: 12, borderWidth: 1, borderColor: '#222' },
	modalTitle: { fontSize: 14, fontWeight: '800', marginBottom: 8 },
	pickRow: { paddingVertical: 8, paddingHorizontal: 6, borderRadius: 8, borderWidth: 1, borderColor: '#ddd' },
	pickName: { fontSize: 12, fontWeight: '800' },
	pickMeta: { fontSize: 11, color: '#555' },
	closeBtn: { marginTop: 10, alignSelf: 'flex-end', paddingVertical: 6, paddingHorizontal: 12, borderRadius: 8, borderWidth: 1, borderColor: '#222' },
	closeText: { fontSize: 12, fontWeight: '700' },
	buyBtn: { marginTop: 4, backgroundColor: '#1d4ed8', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 8 },
	buyTxt: { color: '#fff', fontSize: 10, fontWeight: '700' },
	chartSection: { padding: 16, backgroundColor: '#f9fafb' },
	// Estilos para predicción ML del equipo
	predictionSection: {
		backgroundColor: '#f9fafb',
		paddingVertical: 12,
		paddingHorizontal: 8,
		borderTopWidth: 2,
		borderTopColor: '#1a5c3a',
		maxHeight: 180
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
	predictionContent: {
		flexDirection: 'row',
		gap: 12
	},
	totalPredictionCard: {
		backgroundColor: '#f0fdf4',
		padding: 16,
		borderRadius: 12,
		borderWidth: 2,
		borderColor: '#1a5c3a',
		alignItems: 'center',
		minWidth: 140
	},
	predictionCardTitle: {
		fontSize: 12,
		fontWeight: '700',
		color: '#374151',
		marginBottom: 8,
		textAlign: 'center'
	},
	totalPredictionValue: {
		fontSize: 36,
		fontWeight: '900',
		color: '#1a5c3a',
		marginVertical: 4
	},
	predictionLabel: {
		fontSize: 10,
		color: '#6b7280',
		fontWeight: '600',
		letterSpacing: 1
	},
	rangeText: {
		fontSize: 10,
		color: '#6b7280',
		marginTop: 4,
		fontWeight: '500'
	},
	topPlayersCard: {
		backgroundColor: '#ffffff',
		padding: 12,
		borderRadius: 12,
		borderWidth: 1,
		borderColor: '#e5e7eb',
		minWidth: 180
	},
	playerPredictionRow: {
		flexDirection: 'row',
		alignItems: 'center',
		paddingVertical: 8,
		borderBottomWidth: 1,
		borderBottomColor: '#f3f4f6'
	},
	playerRank: {
		width: 24,
		height: 24,
		borderRadius: 12,
		backgroundColor: '#1a5c3a',
		alignItems: 'center',
		justifyContent: 'center',
		marginRight: 8
	},
	rankNumber: {
		color: '#fff',
		fontSize: 11,
		fontWeight: '800'
	},
	playerInfo: {
		flex: 1,
		marginRight: 8
	},
	playerPredictionName: {
		fontSize: 12,
		fontWeight: '700',
		color: '#111827'
	},
	playerPosition: {
		fontSize: 10,
		color: '#6b7280',
		marginTop: 2
	},
	playerPredictionPoints: {
		alignItems: 'flex-end'
	},
	pointsValue: {
		fontSize: 16,
		fontWeight: '800',
		color: '#1a5c3a'
	},
	pointsLabel: {
		fontSize: 9,
		color: '#6b7280',
		fontWeight: '600'
	},
	breakdownCard: {
		backgroundColor: '#ffffff',
		padding: 12,
		borderRadius: 12,
		borderWidth: 1,
		borderColor: '#e5e7eb',
		minWidth: 160
	},
	positionRow: {
		flexDirection: 'row',
		justifyContent: 'space-between',
		alignItems: 'center',
		paddingVertical: 6,
		borderBottomWidth: 1,
		borderBottomColor: '#f3f4f6'
	},
	positionLabel: {
		fontSize: 12,
		fontWeight: '700',
		color: '#374151'
	},
	positionValue: {
		fontSize: 13,
		fontWeight: '800',
		color: '#1a5c3a'
	},
	pickerContainer: {
		backgroundColor: '#f3f4f6',
		padding: 12,
		borderRadius: 8,
		marginBottom: 12,
		marginHorizontal: 16,
	},
	historicalBanner: {
		backgroundColor: '#1e40af',
		paddingVertical: 8,
		paddingHorizontal: 16,
		marginHorizontal: 16,
		borderRadius: 8,
		marginBottom: 8,
		alignItems: 'center',
	},
	historicalBannerText: {
		color: '#fff',
		fontSize: 13,
		fontWeight: '700',
	},
	pickerLabel: {
		fontSize: 14,
		fontWeight: '600',
		color: '#374151',
		marginBottom: 6,
	},
	picker: {
		backgroundColor: '#fff',
		borderRadius: 6,
		height: 50,
	},
});





