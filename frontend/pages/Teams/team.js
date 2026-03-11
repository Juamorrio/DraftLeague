import React, { useEffect, useMemo, useRef, useState } from 'react';
import { AppState, View, Text, StyleSheet, TouchableOpacity, Image, Modal, FlatList, Alert, ScrollView, ActivityIndicator, TextInput } from 'react-native';
import { useLeague } from '../../context/LeagueContext';
import pitch from '../../assets/Team/pitch.png';
import Player from '../../components/player';
import authService, { authenticatedFetch } from '../../services/authService';
import withAuth from '../../components/withAuth';
import { createOffer, getIncomingOffers, getOutgoingOffers, acceptOffer, rejectOffer, cancelOffer } from '../../services/tradeOfferService';
import { colors, fontSize, fontWeight, radius, spacing, shadow } from '../../utils/theme';


function Team({ navigation, userId: viewUserId = null, readOnly = false }) {
	const { selectedLeague, viewUser, setViewUser, setSelectedPlayer } = useLeague();

	const CHIPS = [
		{ id: 'TRIPLE_CAP',      name: 'Triple Capitán',       desc: 'Tu capitán puntúa ×3 esta jornada',              icon: '👑' },
		{ id: 'DOUBLE_GOALS',    name: 'Golazo',               desc: 'Todos los goles valen el doble de puntos',        icon: '⚽' },
		{ id: 'SUPER_SAVES',     name: 'Mano Segura',          desc: 'Tu portero suma 1pt por cada 2 paradas',          icon: '🧤' },
		{ id: 'NO_PENALTY',      name: 'Juego Limpio',         desc: 'Las tarjetas no restan puntos esta jornada',      icon: '🟡' },
		{ id: 'DOUBLE_ASSISTS',  name: 'Rey de Asistencias',   desc: 'Cada asistencia vale 6pts en lugar de 3',         icon: '🎯' },
		{ id: 'DEFENSIVE_WEEK',  name: 'Muralla',              desc: 'Porterías a cero valen el doble para todos',      icon: '🛡️' },
		{ id: 'LETHAL_STRIKER',  name: 'Delantero Letal',      desc: 'Los goles de delanteros valen 12pts',             icon: '🏹' },
		{ id: 'CREATIVE_MIDS',   name: 'Creador Total',        desc: 'Cada oportunidad creada da 1pt (sin mínimo)',     icon: '🎨' },
		{ id: 'GOLDEN_MINUTES',  name: 'Minutos de Oro',       desc: 'Jugar ≥60 min da +5pts en lugar de +3',           icon: '⏱️' },
		{ id: 'BENCH_BOOST',     name: 'Banco Boost',          desc: 'Tus suplentes también puntúan esta jornada',     icon: '🪑' },
	];
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
	const [teamsLocked, setTeamsLocked] = useState(false);
	const [activeGameweek, setActiveGameweek] = useState(null);
	const [wildcardUsed, setWildcardUsed] = useState(false);

	// Compare state
	const [compareVisible, setCompareVisible] = useState(false);
	const [compareA, setCompareA] = useState(null);
	const [compareB, setCompareB] = useState(null);
	const [comparePicking, setComparePicking] = useState(false);

	// Chip state
	const [activeChip, setActiveChip] = useState(null);
	const [usedChips, setUsedChips] = useState([]);
	const [chipModalVisible, setChipModalVisible] = useState(false);

	// Trade offer state
	const [offerModalVisible, setOfferModalVisible] = useState(false);
	const [offerPriceText, setOfferPriceText] = useState('');
	const [offerPlayer, setOfferPlayer] = useState(null);
	const [offerSubmitting, setOfferSubmitting] = useState(false);

	// Trades management state
	const [incomingOffers, setIncomingOffers] = useState([]);
	const [outgoingOffers, setOutgoingOffers] = useState([]);

	// Refs for auto-refresh (avoid stale closures in intervals)
	const teamIdRef = useRef(null);
	const selectedGameweekRef = useRef(selectedGameweek);
	selectedGameweekRef.current = selectedGameweek;
	const viewUserIdRef = useRef(null);


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
			]
		},
		'4-4-2': {
			name: '4-4-2',
			positions: [
				{ key: 'GK', x: '45%', y: '88%', role: 'POR' },
				{ key: 'LB', x: '8%', y: '68%', role: 'DEF' },
				{ key: 'CB1', x: '32%', y: '68%', role: 'DEF' },
				{ key: 'CB2', x: '58%', y: '68%', role: 'DEF' },
				{ key: 'RB', x: '82%', y: '68%', role: 'DEF' },
				{ key: 'LM', x: '10%', y: '48%', role: 'MID' },
				{ key: 'CM1', x: '35%', y: '48%', role: 'MID' },
				{ key: 'CM2', x: '55%', y: '48%', role: 'MID' },
				{ key: 'RM', x: '80%', y: '48%', role: 'MID' },
				{ key: 'ST1', x: '35%', y: '12%', role: 'DEL' },
				{ key: 'ST2', x: '55%', y: '12%', role: 'DEL' },
			]
		},
		'3-5-2': {
			name: '3-5-2',
			positions: [
				{ key: 'GK', x: '45%', y: '88%', role: 'POR' },
				{ key: 'CB1', x: '20%', y: '68%', role: 'DEF' },
				{ key: 'CB2', x: '45%', y: '68%', role: 'DEF' },
				{ key: 'CB3', x: '70%', y: '68%', role: 'DEF' },
				{ key: 'LM', x: '6%', y: '50%', role: 'MID' },
				{ key: 'CM1', x: '28%', y: '48%', role: 'MID' },
				{ key: 'CM2', x: '52%', y: '48%', role: 'MID' },
				{ key: 'CAM', x: '45%', y: '32%', role: 'MID' },
				{ key: 'RM', x: '84%', y: '50%', role: 'MID' },
				{ key: 'ST1', x: '35%', y: '12%', role: 'DEL' },
				{ key: 'ST2', x: '55%', y: '12%', role: 'DEL' },
			]
		},
		'4-2-3-1': {
			name: '4-2-3-1',
			positions: [
				{ key: 'GK', x: '45%', y: '88%', role: 'POR' },
				{ key: 'LB', x: '8%', y: '68%', role: 'DEF' },
				{ key: 'CB1', x: '32%', y: '68%', role: 'DEF' },
				{ key: 'CB2', x: '58%', y: '68%', role: 'DEF' },
				{ key: 'RB', x: '82%', y: '68%', role: 'DEF' },
				{ key: 'CDM1', x: '35%', y: '52%', role: 'MID' },
				{ key: 'CDM2', x: '55%', y: '52%', role: 'MID' },
				{ key: 'LAM', x: '20%', y: '34%', role: 'MID' },
				{ key: 'CAM', x: '45%', y: '30%', role: 'MID' },
				{ key: 'RAM', x: '70%', y: '34%', role: 'MID' },
				{ key: 'ST', x: '45%', y: '10%', role: 'DEL' },
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

	const teamSummary = useMemo(() => {
		const entries = Object.entries(assigned);
		if (entries.length === 0) return null;
		const VALID_ROLES = ['POR', 'DEF', 'MID', 'DEL'];
		const roleMap = new Map(
			(formations[formation]?.positions ?? []).map(pos => [String(pos.key), pos.role])
		);
		const byPosition = { POR: 0, DEF: 0, MID: 0, DEL: 0 };
		let best = null;
		let total = 0;
		entries.forEach(([key, slotValue]) => {
			const p = slotValue?.player ?? slotValue;
			if (!p) return;
			const pts = selectedGameweek === 'total' ? (p.totalPoints ?? 0) : (playerGameweekPoints[p.id] ?? 0);
			const role = roleMap.get(String(key));
			if (role && VALID_ROLES.includes(role)) byPosition[role] += pts;
			total += pts;
			if (!best || pts > best.points) best = { name: p.fullName ?? p.name, points: pts };
		});
		const count = entries.length;
		return { byPosition, bestPlayer: best, averagePoints: count > 0 ? +(total / count).toFixed(1) : 0, total };
	}, [assigned, playerGameweekPoints, selectedGameweek, formation]);

	if (!viewUserId && viewUser?.id) {
		viewUserId = viewUser.id;
		readOnly = true;
	}
	viewUserIdRef.current = viewUserId;

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
			let userIdToUse = viewUserIdRef.current;
			if (!userIdToUse) {
				const user = await authService.getCurrentUser();
				if (!user?.id) return;
				userIdToUse = user.id;
			}
			const res = await authenticatedFetch(`/api/v1/teams/league/${selectedLeague.id}/${userIdToUse}`);
			if (res.ok) {
				const team = await res.json();
				if (team?.id) {
					teamIdRef.current = team.id;
					setWildcardUsed(!!team.wildcardUsed);
					setActiveChip(team.activeChip ?? null);
					setUsedChips(
						team.usedChips && team.usedChips.length > 0
							? team.usedChips.split(',').filter(Boolean)
							: []
					);
				}
				if (mounted && team?.playerTeams) {
					const newAssigned = {};						
					const playersByRole = {
						POR: [],
						DEF: [],
						MID: [],
						DEL: [],
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

				}
			}
		} catch (e) {
			console.error('Error cargando equipo:', e);
		}
	};

	const loadGameweekStatus = async () => {
		try {
			const res = await authenticatedFetch('/api/v1/fantasy-points/gameweek/status');
			if (res.ok) {
				const data = await res.json();
				setTeamsLocked(!!data.teamsLocked);
				setActiveGameweek(data.activeGameweek ?? null);
			}
		} catch (e) {
			console.log('Error cargando estado de jornada:', e);
		}
	};

	// Auto-refresh: polling every 15s + refresh when app comes to foreground
	useEffect(() => {
		loadGameweekStatus();
		if (!selectedLeague?.id) return;

		const doRefresh = async () => {
			await Promise.all([
				loadTeam(),
				loadGameweekStatus(),
			]);
			if (selectedGameweekRef.current !== 'total' && teamIdRef.current) {
				await loadPlayerGameweekPoints(teamIdRef.current, selectedGameweekRef.current);
			}
		};

		const interval = setInterval(doRefresh, 15000);

		const subscription = AppState.addEventListener('change', state => {
			if (state === 'active') doRefresh();
		});

		return () => {
			clearInterval(interval);
			subscription.remove();
		};
	}, [selectedLeague?.id, viewUser?.id]);

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
	}, [selectedLeague?.id, formation, viewUser?.id]);

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

	const loadTrades = async () => {
		if (!selectedLeague?.id || readOnly) return;
		try {
			const [incoming, outgoing] = await Promise.all([
				getIncomingOffers(selectedLeague.id),
				getOutgoingOffers(selectedLeague.id),
			]);
			setIncomingOffers(Array.isArray(incoming) ? incoming : []);
			setOutgoingOffers(Array.isArray(outgoing) ? outgoing : []);
		} catch (e) {
			console.error('Error cargando traspasos:', e);
		}
	};

	useEffect(() => {
		if (!viewUser?.id && selectedLeague?.id) loadTrades();
	}, [selectedLeague?.id, viewUser?.id]);

	const getPlayerPoints = (player) => {
		if (selectedGameweek === 'total') {
			return player.totalPoints ?? 0;
		} else {
			return playerGameweekPoints[player.id] ?? 0;
		}
	};

	const openPicker = (key) => { if (readOnly || teamsLocked) return; setCurrentKey(key); setPickerVisible(true); };

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
		navigation.navigate('PlayerStats');
	};

	const sendTradeOffer = async () => {
		if (!offerPlayer || !selectedLeague?.id || !teamIdRef.current) return;
		const price = parseInt(offerPriceText, 10);
		if (isNaN(price) || price <= 0) {
			Alert.alert('Error', 'Introduce un precio válido');
			return;
		}
		setOfferSubmitting(true);
		try {
			await createOffer(teamIdRef.current, offerPlayer.id, price, selectedLeague.id);
			setOfferModalVisible(false);
			setOfferPriceText('');
			Alert.alert('Oferta enviada', `Tu oferta de €${price.toLocaleString('es-ES')} por ${offerPlayer.fullName || offerPlayer.name} ha sido enviada.`);
		} catch (e) {
			Alert.alert('Error', e?.message || 'No se pudo enviar la oferta');
		} finally {
			setOfferSubmitting(false);
		}
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

	const saveWildcard = async () => {
		if (!selectedLeague?.id) return;
		const playersList = Object.entries(assigned)
			.map(([position, slotValue]) => {
				const p = getAssignedPlayer(slotValue);
				if (!p?.id) return null;
				return { playerId: p.id, position, lined: true, isCaptain: p.id === captainPlayerId };
			})
			.filter(Boolean);
		if (playersList.length === 0) {
			Alert.alert('Error', 'Debes tener jugadores en el equipo');
			return;
		}
		Alert.alert(
			'🃏 Usar Comodín',
			'Podrás cambiar tu alineación durante la jornada activa. Solo puedes usarlo UNA VEZ por temporada.',
			[
				{ text: 'Cancelar', style: 'cancel' },
				{ text: 'Usar Comodín', style: 'destructive', onPress: async () => {
					setSaving(true);
					try {
						const user = await authService.getCurrentUser();
						if (!user?.id) return;
						const res = await authenticatedFetch(
							`/api/v1/teams/league/${selectedLeague.id}/${user.id}/wildcard`,
							{ method: 'POST', headers: { 'Content-Type': 'application/json' },
							  body: JSON.stringify({ players: playersList }) }
						);
						if (res.ok) {
							setWildcardUsed(true);
							Alert.alert('✅ Comodín activado', 'Tu alineación ha sido guardada para esta jornada.');
						} else {
							const data = await res.json();
							Alert.alert('Error', data?.error || 'No se pudo usar el comodín');
						}
					} catch (e) {
						Alert.alert('Error', e?.message || 'Error desconocido');
					} finally {
						setSaving(false);
					}
				}},
			]
		);
	};

	const activateChip = async (chipId) => {
		if (!selectedLeague?.id) return;
		const chipDef = CHIPS.find(c => c.id === chipId);
		Alert.alert(
			`${chipDef?.icon ?? '🎰'} ${chipDef?.name ?? chipId}`,
			`${chipDef?.desc ?? ''}\n\nEl chip se activará para la próxima jornada y no podrás cancelarlo. ¿Continuar?`,
			[
				{ text: 'Cancelar', style: 'cancel' },
				{ text: 'Activar', style: 'default', onPress: async () => {
					try {
						const user = await authService.getCurrentUser();
						if (!user?.id) return;
						const res = await authenticatedFetch(
							`/api/v1/teams/league/${selectedLeague.id}/${user.id}/chip`,
							{
								method: 'POST',
								headers: { 'Content-Type': 'application/json' },
								body: JSON.stringify({ chip: chipId }),
							}
						);
						if (res.ok) {
							const data = await res.json();
							setActiveChip(data.activeChip || null);
							setUsedChips(data.usedChips ? data.usedChips.split(',').filter(Boolean) : []);
							setChipModalVisible(false);
							Alert.alert('✅ Chip activado', `${chipDef?.name ?? chipId} se aplicará en la próxima jornada.`);
						} else {
							const data = await res.json().catch(() => ({}));
							Alert.alert('Error', data?.error || 'No se pudo activar el chip');
						}
					} catch (e) {
						Alert.alert('Error', e?.message || 'Error desconocido');
					}
				}},
			]
		);
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
						onPress={() => { setViewUser(null); navigation.navigate('Leagues'); }}
					>
						<Text style={styles.backBtnText}>Volver</Text>
					</TouchableOpacity>
				)}
				{!readOnly && (
					<View style={{ flexDirection: 'row', gap: 6, alignItems: 'center' }}>
						<TouchableOpacity 
							style={[styles.saveBtn, (saving || teamsLocked) && styles.saveBtnDisabled]} 
							onPress={saveTeam}
							disabled={saving || teamsLocked}
						>
							<Text style={styles.saveBtnText}>{saving ? 'Guardando...' : teamsLocked ? '🔒 Bloqueado' : 'Guardar'}</Text>
						</TouchableOpacity>
						{teamsLocked && !wildcardUsed && (
							<TouchableOpacity
								style={[styles.saveBtn, { backgroundColor: '#7c3aed' }]}
								onPress={saveWildcard}
								disabled={saving}
							>
								<Text style={styles.saveBtnText}>🃏 Comodín</Text>
							</TouchableOpacity>
						)}
						{teamsLocked && wildcardUsed && (
							<View style={[styles.saveBtn, styles.saveBtnDisabled]}>
								<Text style={styles.saveBtnText}>🃏 Usado</Text>
							</View>
						)}
						{!teamsLocked && (
							activeChip ? (
								<View style={[styles.saveBtn, { backgroundColor: '#d97706' }]}>
									<Text style={styles.saveBtnText}>
										{CHIPS.find(c => c.id === activeChip)?.icon ?? '🎰'} {CHIPS.find(c => c.id === activeChip)?.name ?? activeChip}
									</Text>
								</View>
							) : (
								<TouchableOpacity
									style={[styles.saveBtn, { backgroundColor: '#0369a1' }]}
									onPress={() => setChipModalVisible(true)}
								>
									<Text style={styles.saveBtnText}>🎰 Chips ({usedChips.length}/10)</Text>
								</TouchableOpacity>
							)
						)}
					</View>
				)}
			</View>
			{teamsLocked && !readOnly && (
				<View style={styles.lockBanner}>
					<Text style={styles.lockBannerText}>
						🔒 Jornada {activeGameweek} activa — los equipos están bloqueados
					</Text>
				</View>
			)}

			{!readOnly && !teamsLocked && (
				<View style={styles.formationWrap}>
					<Text style={styles.formationTitle}>Formacion</Text>
					<ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.formationScroll}>
						{Object.keys(formations).map(key => (
							<TouchableOpacity
								key={key}
								style={[styles.formationChip, formation === key && styles.formationChipActive]}
								onPress={() => setFormation(key)}
							>
								<Text style={[styles.formationChipText, formation === key && styles.formationChipTextActive]}>
									{key}
								</Text>
							</TouchableOpacity>
						))}
					</ScrollView>
				</View>
			)}

			{!readOnly && (
				<View style={styles.gwSelectorWrap}>
					<Text style={styles.gwSelectorTitle}>Ver puntos por jornada</Text>
					<ScrollView
						horizontal
						showsHorizontalScrollIndicator={false}
						contentContainerStyle={styles.gwScrollContent}
					>
						<TouchableOpacity
							style={[styles.gwChip, selectedGameweek === 'total' && styles.gwChipActive]}
							onPress={() => setSelectedGameweek('total')}
						>
							<Text style={[styles.gwChipText, selectedGameweek === 'total' && styles.gwChipTextActive]}>
								Total
							</Text>
						</TouchableOpacity>
						{Array.from({ length: maxGameweek }, (_, i) => i + 1).map(gw => (
							<TouchableOpacity
								key={gw}
								style={[styles.gwChip, selectedGameweek === gw && styles.gwChipActive]}
								onPress={() => setSelectedGameweek(gw)}
							>
								<Text style={[styles.gwChipText, selectedGameweek === gw && styles.gwChipTextActive]}>
									J{gw}
								</Text>
							</TouchableOpacity>
						))}
					</ScrollView>
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
							readOnly || isHistorical || teamsLocked ? null : (
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

				{teamSummary && (
					<View style={styles.summaryCard}>
						<View style={styles.summaryRow}>
							{[{ label: 'POR', value: teamSummary.byPosition.POR },
							  { label: 'DEF', value: teamSummary.byPosition.DEF },
							  { label: 'MID', value: teamSummary.byPosition.MID },
							  { label: 'DEL', value: teamSummary.byPosition.DEL },
							].map(({ label, value }) => (
								<View key={label} style={styles.summaryPosBlock}>
									<Text style={styles.summaryPosLabel}>{label}</Text>
									<Text style={styles.summaryPosValue}>{value}</Text>
								</View>
							))}
						</View>
						<View style={styles.summarySeparator} />
						<View style={styles.summaryBottomRow}>
							{teamSummary.bestPlayer && (
								<View style={styles.summaryBest}>
									<Text style={styles.summaryBestLabel}>⭐ Mejor jugador</Text>
									<Text style={styles.summaryBestName} numberOfLines={1}>{teamSummary.bestPlayer.name}</Text>
									<Text style={styles.summaryBestPts}>{teamSummary.bestPlayer.points} pts</Text>
								</View>
							)}
							<View style={styles.summaryStats}>
								<Text style={styles.summaryStatLabel}>Total</Text>
								<Text style={styles.summaryStatValue}>{teamSummary.total}</Text>
								<Text style={styles.summaryStatLabel}>Media</Text>
								<Text style={styles.summaryStatValue}>{teamSummary.averagePoints}</Text>
							</View>
						</View>
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

				{/* Traspasos Pendientes */}
				{!readOnly && (incomingOffers.filter(o => o.status === 'PENDING').length > 0 || outgoingOffers.filter(o => o.status === 'PENDING').length > 0) && (
					<View style={styles.tradesSection}>
						<Text style={styles.tradesSectionTitle}>🤝 Traspasos pendientes</Text>
						{incomingOffers.filter(o => o.status === 'PENDING').map(offer => (
							<View key={offer.id} style={styles.tradeOfferCard}>
								<View style={styles.tradeOfferInfo}>
									<Text style={styles.tradeOfferPlayer}>{offer.player?.fullName}</Text>
									<Text style={styles.tradeOfferMeta}>{offer.fromTeam?.user?.username} ofrece €{offer.offerPrice?.toLocaleString('es-ES')}</Text>
								</View>
								<View style={{ flexDirection: 'row', gap: 6 }}>
									<TouchableOpacity
										style={[styles.tradeActionBtn, { backgroundColor: colors.primary }]}
										onPress={async () => {
											try { await acceptOffer(offer.id); await Promise.all([loadTrades(), loadTeam()]); Alert.alert('Aceptada', 'Traspaso completado.'); }
											catch(e) { Alert.alert('Error', e?.message || 'No se pudo aceptar'); }
										}}
									><Text style={styles.tradeActionBtnText}>Aceptar</Text></TouchableOpacity>
									<TouchableOpacity
										style={[styles.tradeActionBtn, { backgroundColor: colors.danger }]}
										onPress={async () => {
											try { await rejectOffer(offer.id); await loadTrades(); Alert.alert('Rechazada', 'Oferta rechazada.'); }
											catch(e) { Alert.alert('Error', e?.message || 'No se pudo rechazar'); }
										}}
									><Text style={styles.tradeActionBtnText}>Rechazar</Text></TouchableOpacity>
								</View>
							</View>
						))}
						{outgoingOffers.filter(o => o.status === 'PENDING').map(offer => (
							<View key={offer.id} style={[styles.tradeOfferCard, { borderLeftColor: colors.textMuted }]}>
								<View style={styles.tradeOfferInfo}>
									<Text style={styles.tradeOfferPlayer}>{offer.player?.fullName}</Text>
									<Text style={styles.tradeOfferMeta}>enviada a {offer.toTeam?.user?.username} · €{offer.offerPrice?.toLocaleString('es-ES')}</Text>
								</View>
								<TouchableOpacity
									style={[styles.tradeActionBtn, { backgroundColor: colors.textMuted }]}
									onPress={async () => {
										try { await cancelOffer(offer.id); await loadTrades(); Alert.alert('Cancelada', 'Oferta cancelada.'); }
										catch(e) { Alert.alert('Error', e?.message || 'No se pudo cancelar'); }
									}}
								><Text style={styles.tradeActionBtnText}>Cancelar</Text></TouchableOpacity>
							</View>
						))}
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
								{!readOnly && selectedPlayerSlot && !teamsLocked && (
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
												<TouchableOpacity
													style={[styles.saveBtn, { backgroundColor: '#7c3aed' }]}
													onPress={() => {
														setOfferPlayer(selectedPT.player);
														setOfferPriceText(String(selectedPT.player?.marketValue || ''));
														setOptionsVisible(false);
														setOfferModalVisible(true);
													}}
												>
													<Text style={styles.saveBtnText}>Hacer oferta</Text>
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

		{/* Compare Modal */}
		<Modal visible={compareVisible} transparent animationType="fade" onRequestClose={() => { setCompareVisible(false); setComparePicking(false); }}>
			<View style={styles.modalBackdrop}>
				<View style={[styles.modalCard, { maxHeight: '82%' }]}>
					<Text style={styles.modalTitle}>Comparar jugadores</Text>
					{!comparePicking ? (
						<>
							<View style={styles.cmpHeader}>
								<View style={styles.cmpCol}>
									<Text style={styles.cmpPlayerName} numberOfLines={1}>{compareA?.fullName ?? compareA?.name ?? '—'}</Text>
									<View style={[styles.cmpPosBadge, { backgroundColor: colors.primaryDeep }]}>
										<Text style={styles.cmpPosBadgeTxt}>{compareA?.position}</Text>
									</View>
								</View>
								<Text style={styles.cmpVs}>VS</Text>
								<TouchableOpacity style={styles.cmpCol} onPress={() => setComparePicking(true)}>
									{compareB ? (
										<>
											<Text style={styles.cmpPlayerName} numberOfLines={1}>{compareB.fullName ?? compareB.name}</Text>
											<View style={[styles.cmpPosBadge, { backgroundColor: colors.danger }]}>
												<Text style={styles.cmpPosBadgeTxt}>{compareB.position}</Text>
											</View>
										</>
									) : (
										<View style={styles.cmpPickBtn}>
											<Text style={styles.cmpPickBtnText}>+ Elegir</Text>
										</View>
									)}
								</TouchableOpacity>
							</View>
							{compareB && (() => {
								const ptsA = selectedGameweek === 'total' ? (compareA?.totalPoints ?? 0) : (playerGameweekPoints[compareA?.id] ?? 0);
								const ptsB = selectedGameweek === 'total' ? (compareB?.totalPoints ?? 0) : (playerGameweekPoints[compareB?.id] ?? 0);
								const compareRows = [
									{ label: 'Puntos', valA: ptsA, valB: ptsB, numeric: true },
									{ label: 'Valor', valA: `€${((compareA?.marketValue ?? 0) / 1e6).toFixed(2)}M`, valB: `€${((compareB?.marketValue ?? 0) / 1e6).toFixed(2)}M`, numA: compareA?.marketValue ?? 0, numB: compareB?.marketValue ?? 0, numeric: false, numericCmp: true },
									{ label: 'Posición', valA: compareA?.position ?? '—', valB: compareB?.position ?? '—', numeric: false },
									{ label: 'Capitán', valA: captainPlayerId === compareA?.id ? '★' : '—', valB: captainPlayerId === compareB?.id ? '★' : '—', numeric: false },
								];
								return (
									<View style={styles.cmpTable}>
										{compareRows.map(row => {
											const winsA = row.numeric ? row.valA > row.valB : row.numericCmp ? row.numA > row.numB : false;
											const winsB = row.numeric ? row.valB > row.valA : row.numericCmp ? row.numB > row.numA : false;
											return (
												<View key={row.label} style={styles.cmpRow}>
													<Text style={[styles.cmpValue, winsA && styles.cmpWinner]}>{String(row.valA)}</Text>
													<Text style={styles.cmpStat}>{row.label}</Text>
													<Text style={[styles.cmpValue, winsB && styles.cmpWinner]}>{String(row.valB)}</Text>
												</View>
											);
										})}
									</View>
								);
							})()}
						</>
					) : (
						<>
							<Text style={{ fontSize: 13, color: colors.textSecondary, marginBottom: 8 }}>Selecciona el segundo jugador:</Text>
							<FlatList
								style={{ maxHeight: 260 }}
								data={Object.values(assigned).map(s => s?.player ?? s).filter(p => p?.id && p.id !== compareA?.id)}
								keyExtractor={item => String(item.id)}
								ItemSeparatorComponent={() => <View style={{ height: 6 }} />}
								renderItem={({ item }) => (
									<TouchableOpacity style={styles.pickRow} onPress={() => { setCompareB(item); setComparePicking(false); }}>
										<Text style={styles.pickName}>{item.fullName ?? item.name}</Text>
										<Text style={styles.pickMeta}>{item.position} · {selectedGameweek === 'total' ? (item.totalPoints ?? 0) : (playerGameweekPoints[item.id] ?? 0)} pts</Text>
									</TouchableOpacity>
								)}
							/>
						</>
					)}
					<TouchableOpacity style={styles.closeBtn} onPress={() => { setCompareVisible(false); setComparePicking(false); }}>
						<Text style={styles.closeText}>Cerrar</Text>
					</TouchableOpacity>
				</View>
			</View>
		</Modal>

		{/* Compare Modal */}
		<Modal visible={compareVisible} transparent animationType="fade" onRequestClose={() => { setCompareVisible(false); setComparePicking(false); }}>
			<View style={styles.modalBackdrop}>
				<View style={[styles.modalCard, { maxHeight: '82%' }]}>
					<Text style={styles.modalTitle}>Comparar jugadores</Text>
					{!comparePicking ? (
						<>
							<View style={styles.cmpHeader}>
								<View style={styles.cmpCol}>
									<Text style={styles.cmpPlayerName} numberOfLines={1}>{compareA?.fullName ?? compareA?.name ?? '—'}</Text>
									<View style={[styles.cmpPosBadge, { backgroundColor: colors.primaryDeep }]}>
										<Text style={styles.cmpPosBadgeTxt}>{compareA?.position}</Text>
									</View>
								</View>
								<Text style={styles.cmpVs}>VS</Text>
								<TouchableOpacity style={styles.cmpCol} onPress={() => setComparePicking(true)}>
									{compareB ? (
										<>
											<Text style={styles.cmpPlayerName} numberOfLines={1}>{compareB.fullName ?? compareB.name}</Text>
											<View style={[styles.cmpPosBadge, { backgroundColor: colors.danger }]}>
												<Text style={styles.cmpPosBadgeTxt}>{compareB.position}</Text>
											</View>
										</>
									) : (
										<View style={styles.cmpPickBtn}>
											<Text style={styles.cmpPickBtnText}>+ Elegir</Text>
										</View>
									)}
								</TouchableOpacity>
							</View>
							{compareB && (() => {
								const ptsA = selectedGameweek === 'total' ? (compareA?.totalPoints ?? 0) : (playerGameweekPoints[compareA?.id] ?? 0);
								const ptsB = selectedGameweek === 'total' ? (compareB?.totalPoints ?? 0) : (playerGameweekPoints[compareB?.id] ?? 0);
								const compareRows = [
									{ label: 'Puntos', valA: ptsA, valB: ptsB, numeric: true },
									{ label: 'Valor', valA: `€${((compareA?.marketValue ?? 0) / 1e6).toFixed(2)}M`, valB: `€${((compareB?.marketValue ?? 0) / 1e6).toFixed(2)}M`, numA: compareA?.marketValue ?? 0, numB: compareB?.marketValue ?? 0, numeric: false, numericCmp: true },
									{ label: 'Posición', valA: compareA?.position ?? '—', valB: compareB?.position ?? '—', numeric: false },
									{ label: 'Capitán', valA: captainPlayerId === compareA?.id ? '★' : '—', valB: captainPlayerId === compareB?.id ? '★' : '—', numeric: false },
								];
								return (
									<View style={styles.cmpTable}>
										{compareRows.map(row => {
											const winsA = row.numeric ? row.valA > row.valB : row.numericCmp ? row.numA > row.numB : false;
											const winsB = row.numeric ? row.valB > row.valA : row.numericCmp ? row.numB > row.numA : false;
											return (
												<View key={row.label} style={styles.cmpRow}>
													<Text style={[styles.cmpValue, winsA && styles.cmpWinner]}>{String(row.valA)}</Text>
													<Text style={styles.cmpStat}>{row.label}</Text>
													<Text style={[styles.cmpValue, winsB && styles.cmpWinner]}>{String(row.valB)}</Text>
												</View>
											);
										})}
									</View>
								);
							})()}
						</>
					) : (
						<>
							<Text style={{ fontSize: 13, color: colors.textSecondary, marginBottom: 8 }}>Selecciona el segundo jugador:</Text>
							<FlatList
								style={{ maxHeight: 260 }}
								data={Object.values(assigned).map(s => s?.player ?? s).filter(p => p?.id && p.id !== compareA?.id)}
								keyExtractor={item => String(item.id)}
								ItemSeparatorComponent={() => <View style={{ height: 6 }} />}
								renderItem={({ item }) => (
									<TouchableOpacity style={styles.pickRow} onPress={() => { setCompareB(item); setComparePicking(false); }}>
										<Text style={styles.pickName}>{item.fullName ?? item.name}</Text>
										<Text style={styles.pickMeta}>{item.position} · {selectedGameweek === 'total' ? (item.totalPoints ?? 0) : (playerGameweekPoints[item.id] ?? 0)} pts</Text>
									</TouchableOpacity>
								)}
							/>
						</>
					)}
					<TouchableOpacity style={styles.closeBtn} onPress={() => { setCompareVisible(false); setComparePicking(false); }}>
						<Text style={styles.closeText}>Cerrar</Text>
					</TouchableOpacity>
				</View>
			</View>
		</Modal>

		{/* Trade Offer Price Modal */}
		<Modal visible={offerModalVisible} transparent animationType="fade" onRequestClose={() => setOfferModalVisible(false)}>
			<View style={styles.modalBackdrop}>
				<View style={styles.modalCard}>
					<Text style={styles.modalTitle}>Hacer oferta por {offerPlayer?.fullName || offerPlayer?.name}</Text>
					<Text style={{ fontSize: 12, color: '#555', marginBottom: 8 }}>
						Valor de mercado: €{(offerPlayer?.marketValue || 0).toLocaleString('es-ES')}
					</Text>
					<Text style={{ fontSize: 12, fontWeight: '700', marginBottom: 4 }}>Tu oferta (€)</Text>
					<TextInput
						style={{ borderWidth: 1, borderColor: '#ccc', borderRadius: 8, padding: 10, fontSize: 16, marginBottom: 12 }}
						placeholder="Introduce el precio"
						keyboardType="numeric"
						value={offerPriceText}
						onChangeText={setOfferPriceText}
					/>
					<View style={{ flexDirection: 'row', gap: 8 }}>
						<TouchableOpacity
							style={[styles.saveBtn, { flex: 1, backgroundColor: '#7c3aed', opacity: offerSubmitting ? 0.6 : 1 }]}
							onPress={sendTradeOffer}
							disabled={offerSubmitting}
						>
							<Text style={[styles.saveBtnText, { textAlign: 'center' }]}>
								{offerSubmitting ? 'Enviando...' : 'Enviar oferta'}
							</Text>
						</TouchableOpacity>
						<TouchableOpacity
							style={[styles.closeBtn, { flex: 1, alignSelf: 'stretch', justifyContent: 'center', alignItems: 'center' }]}
							onPress={() => setOfferModalVisible(false)}
						>
							<Text style={styles.closeText}>Cancelar</Text>
						</TouchableOpacity>
					</View>
				</View>
			</View>
		</Modal>

		{/* Chip Selector Modal */}
		{!readOnly && (
			<Modal visible={chipModalVisible} transparent animationType="slide" onRequestClose={() => setChipModalVisible(false)}>
				<View style={styles.modalBackdrop}>
					<View style={[styles.modalCard, { maxHeight: '80%', width: '92%' }]}>
						<Text style={styles.modalTitle}>🎰 Tus Chips ({usedChips.length}/10 usados)</Text>
						<Text style={{ fontSize: 12, color: colors.textSecondary, marginBottom: 12 }}>
							Solo puedes activar un chip por jornada y no podrás cancelarlo.
						</Text>
						<ScrollView showsVerticalScrollIndicator={false}>
							{CHIPS.map(chip => {
								const isUsed = usedChips.includes(chip.id);
								const isActive = activeChip === chip.id;
								return (
									<TouchableOpacity
										key={chip.id}
										disabled={isUsed || isActive}
										onPress={() => activateChip(chip.id)}
										style={[
											styles.chipCard,
											isActive && styles.chipCardActive,
											isUsed && styles.chipCardUsed,
										]}
									>
										<Text style={styles.chipIcon}>{chip.icon}</Text>
										<View style={{ flex: 1 }}>
											<Text style={[styles.chipName, isUsed && styles.chipTextUsed]}>
												{chip.name}{isActive ? '  ✓ Activo' : ''}{isUsed ? '  ✗ Usado' : ''}
											</Text>
											<Text style={[styles.chipDesc, isUsed && styles.chipTextUsed]}>{chip.desc}</Text>
										</View>
									</TouchableOpacity>
								);
							})}
						</ScrollView>
						<TouchableOpacity style={styles.closeBtn} onPress={() => setChipModalVisible(false)}>
							<Text style={styles.closeText}>Cerrar</Text>
						</TouchableOpacity>
					</View>
				</View>
			</Modal>
		)}

		</View>
	);
}
export default withAuth(Team);

const styles = StyleSheet.create({
	container: { flex: 1, backgroundColor: colors.bgApp },
	topBar: {
		flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
		paddingHorizontal: spacing.lg, paddingVertical: spacing.md,
		backgroundColor: colors.primaryDeep,
	},
	header: { color: colors.textInverse, fontWeight: fontWeight.extrabold, fontSize: fontSize.lg },
	saveBtn: { backgroundColor: colors.primary, paddingVertical: 8, paddingHorizontal: spacing.lg, borderRadius: radius.pill },
	saveBtnDisabled: { backgroundColor: colors.textMuted },
	saveBtnText: { color: colors.textInverse, fontWeight: fontWeight.bold, fontSize: fontSize.sm },
	backBtn: {
		backgroundColor: 'rgba(255,255,255,0.15)', paddingVertical: 8, paddingHorizontal: spacing.lg,
		borderRadius: radius.pill, borderWidth: 1, borderColor: 'rgba(255,255,255,0.25)',
	},
	backBtnText: { color: colors.textInverse, fontWeight: fontWeight.bold, fontSize: fontSize.sm },
	scrollView: { flex: 1 },
	scrollContent: { paddingBottom: 24 },
	fieldContainer: { height: 500, position: 'relative' },
	fieldImage: { width: '100%', height: '100%' },
	spot: { position: 'absolute', transform: [{ translateX: -25 }, { translateY: -25 }], alignItems: 'center' },
	spotBtn: {
		width: 50, height: 50, borderRadius: radius.md,
		backgroundColor: 'rgba(255,255,255,0.25)', borderWidth: 2,
		borderColor: 'rgba(255,255,255,0.8)', alignItems: 'center', justifyContent: 'center',
	},
	plus: { color: colors.textInverse, fontWeight: fontWeight.black, fontSize: 22, lineHeight: 24 },
	spotLabel: {
		marginTop: 3, color: colors.textInverse, fontSize: fontSize.xs, fontWeight: fontWeight.bold,
		textShadowColor: 'rgba(0,0,0,0.6)', textShadowOffset: { width: 0, height: 1 }, textShadowRadius: 2,
	},
	playerCard: { minWidth: 140 },
	modalBackdrop: { flex: 1, backgroundColor: colors.overlay, alignItems: 'center', justifyContent: 'center' },
	modalCard: {
		width: '88%', maxHeight: '72%', backgroundColor: colors.bgCard,
		borderRadius: radius.xl, padding: spacing.lg, ...shadow.lg,
	},
	modalTitle: { fontSize: fontSize.md, fontWeight: fontWeight.bold, marginBottom: spacing.md, color: colors.textPrimary },
	pickRow: {
		paddingVertical: spacing.md, paddingHorizontal: spacing.md,
		borderRadius: radius.md, borderWidth: 1, borderColor: colors.border, marginBottom: 6,
	},
	pickName: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textPrimary },
	pickMeta: { fontSize: fontSize.xs, color: colors.textSecondary, marginTop: 2 },
	closeBtn: {
		marginTop: spacing.md, alignSelf: 'flex-end', paddingVertical: 8,
		paddingHorizontal: spacing.lg, borderRadius: radius.pill,
		backgroundColor: colors.bgSubtle, borderWidth: 1, borderColor: colors.border,
	},
	closeText: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textSecondary },
	buyBtn: { marginTop: 4, backgroundColor: colors.primary, paddingHorizontal: spacing.sm, paddingVertical: 4, borderRadius: radius.pill },
	buyTxt: { color: colors.textInverse, fontSize: fontSize.xs, fontWeight: fontWeight.bold },
	lockBanner: { backgroundColor: colors.danger, paddingVertical: 10, paddingHorizontal: spacing.lg, alignItems: 'center' },
	lockBannerText: { color: colors.textInverse, fontWeight: fontWeight.bold, fontSize: fontSize.sm },
	predictionSection: {
		backgroundColor: colors.bgApp, paddingVertical: spacing.md, paddingHorizontal: spacing.sm,
		borderTopWidth: 1, borderTopColor: colors.border, maxHeight: 180,
	},
	loadingContainer: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', padding: spacing.xl },
	loadingText: { marginLeft: spacing.md, fontSize: fontSize.sm, color: colors.textMuted, fontWeight: fontWeight.semibold },
	predictionContent: { flexDirection: 'row', gap: spacing.md },
	totalPredictionCard: {
		backgroundColor: colors.successBg, padding: spacing.lg, borderRadius: radius.lg,
		borderWidth: 1.5, borderColor: colors.primaryMuted, alignItems: 'center', minWidth: 140,
	},
	predictionCardTitle: {
		fontSize: fontSize.xs, fontWeight: fontWeight.bold, color: colors.textSecondary,
		marginBottom: spacing.sm, textAlign: 'center',
	},
	totalPredictionValue: {
		fontSize: fontSize['3xl'], fontWeight: fontWeight.black,
		color: colors.primaryDark, marginVertical: 4,
	},
	predictionLabel: { fontSize: fontSize.xs, color: colors.textMuted, fontWeight: fontWeight.semibold, letterSpacing: 1 },
	rangeText: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 4, fontWeight: fontWeight.medium },
	topPlayersCard: {
		backgroundColor: colors.bgCard, padding: spacing.md, borderRadius: radius.lg,
		borderWidth: 1, borderColor: colors.border, minWidth: 180, ...shadow.sm,
	},
	playerPredictionRow: {
		flexDirection: 'row', alignItems: 'center', paddingVertical: spacing.sm,
		borderBottomWidth: 1, borderBottomColor: colors.bgSubtle,
	},
	playerRank: {
		width: 22, height: 22, borderRadius: 11, backgroundColor: colors.primaryDark,
		alignItems: 'center', justifyContent: 'center', marginRight: spacing.sm,
	},
	rankNumber: { color: colors.textInverse, fontSize: fontSize.xs, fontWeight: fontWeight.black },
	playerInfo: { flex: 1, marginRight: spacing.sm },
	playerPredictionName: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textPrimary },
	playerPosition: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2 },
	playerPredictionPoints: { alignItems: 'flex-end' },
	pointsValue: { fontSize: fontSize.md, fontWeight: fontWeight.extrabold, color: colors.primaryDark },
	pointsLabel: { fontSize: fontSize.xs - 1, color: colors.textMuted, fontWeight: fontWeight.semibold },
	breakdownCard: {
		backgroundColor: colors.bgCard, padding: spacing.md, borderRadius: radius.lg,
		borderWidth: 1, borderColor: colors.border, minWidth: 160, ...shadow.sm,
	},
	positionRow: {
		flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
		paddingVertical: 6, borderBottomWidth: 1, borderBottomColor: colors.bgSubtle,
	},
	positionLabel: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textSecondary },
	positionValue: { fontSize: fontSize.sm, fontWeight: fontWeight.extrabold, color: colors.primaryDark },
	gwSelectorWrap: {
		backgroundColor: colors.bgCard,
		paddingTop: spacing.sm,
		paddingBottom: spacing.xs,
		borderBottomWidth: 1,
		borderBottomColor: colors.border,
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
	historicalBanner: {
		backgroundColor: '#1E3A8A', paddingVertical: 10, paddingHorizontal: spacing.lg,
		marginHorizontal: spacing.lg, borderRadius: radius.md, marginBottom: spacing.sm, alignItems: 'center',
	},
	historicalBannerText: { color: colors.textInverse, fontSize: fontSize.sm, fontWeight: fontWeight.bold },
	formationWrap: {
		backgroundColor: colors.bgCard,
		marginHorizontal: spacing.md,
		marginTop: spacing.sm,
		marginBottom: spacing.sm,
		borderRadius: radius.lg,
		borderWidth: 1,
		borderColor: colors.border,
		paddingTop: spacing.sm,
		paddingBottom: spacing.xs,
		...shadow.sm,
	},
	formationTitle: {
		fontSize: fontSize.xs,
		fontWeight: fontWeight.semibold,
		color: colors.textMuted,
		marginLeft: spacing.md,
		marginBottom: 6,
		letterSpacing: 0.5,
		textTransform: 'uppercase',
	},
	formationScroll: {
		paddingHorizontal: spacing.md,
		paddingVertical: 6,
		gap: 8,
		alignItems: 'center',
	},
	formationChip: {
		paddingVertical: 7,
		paddingHorizontal: 14,
		borderRadius: radius.pill,
		backgroundColor: colors.bgSubtle,
		borderWidth: 1.5,
		borderColor: colors.border,
	},
	formationChipActive: {
		backgroundColor: colors.primary,
		borderColor: colors.primary,
	},
	formationChipText: {
		fontSize: fontSize.sm,
		fontWeight: fontWeight.semibold,
		color: colors.textSecondary,
	},
	formationChipTextActive: {
		color: colors.textInverse,
		fontWeight: fontWeight.bold,
	},
	tradesSection: {
		marginHorizontal: spacing.md,
		marginTop: spacing.md,
		marginBottom: spacing.sm,
		backgroundColor: colors.bgCard,
		borderRadius: radius.lg,
		padding: spacing.md,
		borderWidth: 1,
		borderColor: colors.border,
	},
	tradesSectionTitle: {
		fontSize: fontSize.md,
		fontWeight: fontWeight.bold,
		color: colors.textPrimary,
		marginBottom: spacing.sm,
	},
	tradeOfferCard: {
		flexDirection: 'row',
		alignItems: 'center',
		justifyContent: 'space-between',
		backgroundColor: colors.bgSubtle,
		borderRadius: radius.md,
		padding: spacing.md,
		marginBottom: spacing.sm,
		borderLeftWidth: 4,
		borderLeftColor: colors.purple,
	},
	tradeOfferInfo: { flex: 1, marginRight: spacing.sm },
	tradeOfferPlayer: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textPrimary },
	tradeOfferMeta: { fontSize: fontSize.xs, color: colors.textSecondary, marginTop: 2 },
	tradeActionBtn: { paddingVertical: 6, paddingHorizontal: 12, borderRadius: radius.pill },
	tradeActionBtnText: { color: colors.textInverse, fontSize: fontSize.xs, fontWeight: fontWeight.bold },
	// Summary card
	summaryCard: {
		marginHorizontal: spacing.lg, marginTop: spacing.sm, marginBottom: spacing.sm,
		backgroundColor: colors.bgCard, borderRadius: radius.lg, borderWidth: 1,
		borderColor: colors.border, padding: spacing.md, ...shadow.sm,
	},
	summaryRow: { flexDirection: 'row', justifyContent: 'space-around' },
	summaryPosBlock: { alignItems: 'center', gap: 4 },
	summaryPosLabel: { fontSize: fontSize.xs, fontWeight: fontWeight.bold, color: colors.textMuted, textTransform: 'uppercase', letterSpacing: 0.5 },
	summaryPosValue: { fontSize: fontSize.xl, fontWeight: fontWeight.black, color: colors.textPrimary },
	summarySeparator: { height: 1, backgroundColor: colors.border, marginVertical: spacing.sm },
	summaryBottomRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
	summaryBest: { flex: 1, gap: 2 },
	summaryBestLabel: { fontSize: fontSize.xs, color: colors.textMuted, fontWeight: fontWeight.semibold },
	summaryBestName: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textPrimary },
	summaryBestPts: { fontSize: fontSize.xs, color: colors.primary, fontWeight: fontWeight.bold },
	summaryStats: { alignItems: 'flex-end', gap: 4 },
	summaryStatLabel: { fontSize: fontSize.xs, color: colors.textMuted },
	summaryStatValue: { fontSize: fontSize.md, fontWeight: fontWeight.bold, color: colors.textPrimary },
	// Chip modal
	chipCard: {
		flexDirection: 'row', alignItems: 'center', gap: 12,
		paddingVertical: 12, paddingHorizontal: 14,
		borderRadius: 10, borderWidth: 1.5, borderColor: colors.border,
		backgroundColor: colors.bgSubtle, marginBottom: 8,
	},
	chipCardActive: {
		borderColor: '#d97706', backgroundColor: '#fef3c7',
	},
	chipCardUsed: {
		opacity: 0.4, backgroundColor: colors.bgApp,
	},
	chipIcon: { fontSize: 26 },
	chipName: { fontSize: 13, fontWeight: '700', color: colors.textPrimary, marginBottom: 2 },
	chipDesc: { fontSize: 11, color: colors.textSecondary },
	chipTextUsed: { color: colors.textMuted },
	// Compare modal
	cmpHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: spacing.md, gap: spacing.sm },
	cmpCol: { flex: 1, alignItems: 'center', gap: 6 },
	cmpVs: { fontSize: fontSize.sm, fontWeight: fontWeight.black, color: colors.textMuted, paddingHorizontal: 4 },
	cmpPlayerName: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textPrimary, textAlign: 'center' },
	cmpPosBadge: { paddingHorizontal: spacing.sm, paddingVertical: 3, borderRadius: radius.pill },
	cmpPosBadgeTxt: { fontSize: fontSize.xs, fontWeight: fontWeight.bold, color: colors.textInverse },
	cmpPickBtn: { borderWidth: 1.5, borderColor: colors.border, paddingHorizontal: spacing.md, paddingVertical: spacing.sm, borderRadius: radius.md },
	cmpPickBtnText: { fontSize: fontSize.sm, color: colors.textMuted, fontWeight: fontWeight.semibold },
	cmpTable: { gap: 2, marginBottom: spacing.sm },
	cmpRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: spacing.sm, borderBottomWidth: 1, borderBottomColor: colors.border },
	cmpValue: { flex: 1, fontSize: fontSize.md, fontWeight: fontWeight.bold, color: colors.textSecondary, textAlign: 'center' },
	cmpWinner: { color: colors.primary, fontSize: fontSize.lg },
	cmpStat: { flex: 1, fontSize: fontSize.xs, color: colors.textMuted, textAlign: 'center', textTransform: 'uppercase', letterSpacing: 0.5 },
});





