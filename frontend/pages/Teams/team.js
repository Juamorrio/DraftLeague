import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, Modal, FlatList, Alert } from 'react-native';
import { useLeague } from '../../context/LeagueContext';
import pitch from '../../assets/Team/pitch.png';
import Player from '../../components/player';
import { authenticatedFetch } from '../../services/authService';
import withAuth from '../../components/withAuth';


function Team() {
	const { selectedLeague } = useLeague();
	const [players, setPlayers] = useState([]);
	const [loading, setLoading] = useState(false);
	const [assigned, setAssigned] = useState({});
	const [pickerVisible, setPickerVisible] = useState(false);
	const [currentKey, setCurrentKey] = useState(null);
	const [error, setError] = useState(null);
	const [saving, setSaving] = useState(false);
	const [formation, setFormation] = useState('4-3-3'); 

	
	const FIELD_WIDTH = '100%';
	const FIELD_HEIGHT = '100%';

	const formations = {
		'4-3-3': {
			name: '4-3-3',
			positions: [
				{ key: 'GK', x: '50%', y: '90%', role: 'POR' },
				{ key: 'LB', x: '15%', y: '70%', role: 'DEF' },
				{ key: 'CB1', x: '35%', y: '70%', role: 'DEF' },
				{ key: 'CB2', x: '65%', y: '70%', role: 'DEF' },
				{ key: 'RB', x: '85%', y: '70%', role: 'DEF' },
				{ key: 'CM1', x: '30%', y: '45%', role: 'MID' },
				{ key: 'CM2', x: '70%', y: '45%', role: 'MID' },
				{ key: 'CAM', x: '50%', y: '35%', role: 'MID' },
				{ key: 'LW', x: '20%', y: '15%', role: 'DEL' },
				{ key: 'RW', x: '80%', y: '15%', role: 'DEL' },
				{ key: 'ST', x: '50%', y: '5%', role: 'DEL' },
			]
		},
	};

	const currentFormation = formations[formation];
	const positions = currentFormation.positions;

	useEffect(() => {
		let mounted = true;
		(async () => {
			if (!selectedLeague?.id) {
				if (mounted) setPlayers([]);
				return;
			}
			setLoading(true); setError(null);
			try {
				const res = await authenticatedFetch(`/api/v1/players?leagueId=${selectedLeague.id}&onlyOwned=true`);
				const json = await res.json();
				if (mounted) {
					setPlayers(Array.isArray(json) ? json : (json?.content ?? []));
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
			const res = await authenticatedFetch(`/api/v1/teams/league/${selectedLeague.id}`);
			if (res.ok) {
				const team = await res.json();
				if (mounted && team?.playerTeams) {
					const newAssigned = {};						
					const playersByRole = {
						POR: [],
						DEF: [],
						MID: [],
						DEL: []
					};	
					team.playerTeams.forEach(pt => {
						const role = pt.player.position;
						if (playersByRole[role]) {
							playersByRole[role].push(pt.player);
						}
					});
					
					const slotsByRole = {
						POR: [],
						DEF: [],
						MID: [],
						DEL: []
					};
					
					currentFormation.positions.forEach(pos => {
						if (slotsByRole[pos.role]) {
							slotsByRole[pos.role].push(pos.key);
						}
					});
					
					Object.entries(playersByRole).forEach(([role, players]) => {
						const slots = slotsByRole[role] || [];
						players.forEach((player, index) => {
							if (index < slots.length) {
								newAssigned[slots[index]] = player;
							}
						});
					});
					
					setAssigned(newAssigned);
				}
			}
		} catch (e) {
			console.error('Error cargando equipo:', e);
		}
	};

	useEffect(() => {
		loadTeam();
	}, [selectedLeague?.id, formation]);

	const openPicker = (key) => { setCurrentKey(key); setPickerVisible(true); };
	
	const assignPlayer = (player) => {
		if (!currentKey) return;
		setAssigned(prev => ({ ...prev, [currentKey]: player }));
		setPickerVisible(false);
		setCurrentKey(null);
	};

	const saveTeam = async () => {
		if (!selectedLeague?.id) {
			Alert.alert('Error', 'Selecciona una liga primero');
			return;
		}

		const playersList = Object.entries(assigned).map(([position, player]) => ({
			playerId: player.id,
			position: position,
			lined: true,
			isCaptain: false
		}));

		if (playersList.length === 0) {
			Alert.alert('Error', 'Debes seleccionar al menos un jugador');
			return;
		}

		setSaving(true);
		try {
			const res = await authenticatedFetch(`/api/v1/teams/league/${selectedLeague.id}/players`, {
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
				<Text style={styles.header}>{selectedLeague ? `Equipo · ${selectedLeague.name}` : 'Equipo'}</Text>
				
				<TouchableOpacity 
					style={[styles.saveBtn, saving && styles.saveBtnDisabled]} 
					onPress={saveTeam}
					disabled={saving}
				>
					<Text style={styles.saveBtnText}>{saving ? 'Guardando...' : 'Guardar'}</Text>
				</TouchableOpacity>
			</View>

			<View style={styles.fieldContainer}>
				<Image source={pitch} style={styles.fieldImage} />
				{positions.map(p => (
					<View key={p.key} style={[styles.spot, { left: p.x, top: p.y }]}> 
					{assigned[p.key] ? (
						(() => {
							return (
								<Player
									name={assigned[p.key].fullName ?? assigned[p.key].name}
									avatar={assigned[p.key].avatarUrl ? { uri: assigned[p.key].avatarUrl } : null}
									size={50}
									teamId={assigned[p.key].teamId}
									onPress={() => openPicker(p.key)}
								/>
							);
						})()
					) : (
						<TouchableOpacity style={styles.spotBtn} activeOpacity={0.7} onPress={() => openPicker(p.key)}>
							<Text style={styles.plus}>+</Text>
						</TouchableOpacity>
					)}
					{!assigned[p.key] && <Text style={styles.spotLabel}>{p.key}</Text>}
				</View>
			))}
			</View>

			<Modal visible={pickerVisible} transparent animationType="fade" onRequestClose={() => setPickerVisible(false)}>
				<View style={styles.modalBackdrop}>
					<View style={styles.modalCard}>
						<Text style={styles.modalTitle}>Selecciona jugador para {currentKey}</Text>
						{loading && <Text>Cargando jugadores…</Text>}
						{error && <Text style={{ color: 'red' }}>{error}</Text>}
						{!loading && !error && (
							<FlatList
								data={players.filter(sp => matchesSpot(sp.position, currentKey) && !Object.values(assigned).some(a => a.id === sp.id) )}
								keyExtractor={(item) => String(item.id ?? `${item.name}-${item.position}`)}
								ItemSeparatorComponent={() => <View style={{ height: 6 }} />}
								renderItem={({ item }) => (
									<TouchableOpacity style={styles.pickRow} onPress={() => assignPlayer(item)}>
										<Text style={styles.pickName}>{item.fullName ?? item.name}</Text>
										<Text style={styles.pickMeta}>{item.position} · {(item.totalPoints ?? 0)} pts · €{((item.marketValue ?? 0)).toLocaleString()}</Text>
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
	fieldContainer: { flex: 1, position: 'relative' },
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
	closeText: { fontSize: 12, fontWeight: '700' }
});





