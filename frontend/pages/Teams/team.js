import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, Modal, FlatList } from 'react-native';
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

	const positions = [
		{ key: 'GK', x: 48, y: 90 },
		{ key: 'LB', x: 15, y: 75 },
		{ key: 'CB1', x: 38, y: 75 },
		{ key: 'CB2', x: 60, y: 75 },
		{ key: 'RB', x: 85, y: 75 },
		{ key: 'CM1', x: 32, y: 55 },
		{ key: 'CM2', x: 65, y: 55 },
		{ key: 'LW', x: 18, y: 35 },
		{ key: 'CAM', x: 48, y: 35 },
		{ key: 'RW', x: 78, y: 35 },
		{ key: 'ST', x: 48, y: 18 },
	];

	useEffect(() => {
		let mounted = true;
		(async () => {
			setLoading(true); setError(null);
			try {
				const res = await authenticatedFetch('/api/v1/players');
				const json = await res.json();
				if (mounted) setPlayers(Array.isArray(json) ? json : (json?.content ?? [])); setLoading(false);
			} catch (e) {
				if (mounted) setError('No se pudieron cargar jugadores');
			} 
		})();
		return () => { mounted = false; };
		
	}, []);	

	const openPicker = (key) => { setCurrentKey(key); setPickerVisible(true); };
	const assignPlayer = (player) => {
		if (!currentKey) return;
		setAssigned(prev => ({ ...prev, [currentKey]: player }));
		setPickerVisible(false);
		setCurrentKey(null);
	};

	function matchesSpot(playerPos, key) {
	if (!playerPos || !key) return true;
	const groupBySpot = {
		GK: 'POR',
		LB: 'DEF',
		RB: 'DEF',
		CB1: 'DEF',
		CB2: 'DEF',
		CM1: 'MID',
		CM2: 'MID',
		CAM: 'MID',
		LW: 'DEL',
		RW: 'DEL',
		ST: 'DEL',
	};
	const expected = groupBySpot[key] || key;
	return String(playerPos) === expected;
}

	return (
		<View style={styles.container}>
			<Text style={styles.header}>{selectedLeague ? `Equipo · ${selectedLeague.name}` : 'Equipo'}</Text>
			<Image source={pitch} style={{ width: 400, height: 600 }} />
			{positions.map(p => (
				<View key={p.key} style={[styles.spot, { left: `${p.x}%`, top: `${p.y}%` }]}> 
					{assigned[p.key] ? (
						(() => {
							return (
								<Player
									name={assigned[p.key].fullName ?? assigned[p.key].name}
									avatar={null}
									size={50}
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
	container: { flex: 1, backgroundColor: '#ffffffff', alignItems: 'center', paddingTop: 12 },
	header: { color: '#fff', fontWeight: '800', fontSize: 18, marginBottom: 8 },
	spot: { position: 'absolute', transform: [{ translateX: -18 }, { translateY: -18 }], alignItems: 'center' },
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





