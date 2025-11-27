import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image } from 'react-native';
import { useLeague } from '../../context/LeagueContext';
import pitch from '../../assets/Team/pitch.png';


export default function Team() {
	const { selectedLeague } = useLeague();

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

	return (
		<View style={styles.container}>
			<Text style={styles.header}>{selectedLeague ? `Equipo · ${selectedLeague.name}` : 'Equipo'}</Text>
			<Image source={pitch} style={{ width: 400, height: 600 }} />
				{positions.map(p => (
					<View key={p.key} style={[styles.spot, { left: `${p.x}%`, top: `${p.y}%` }]}> 
						<TouchableOpacity style={styles.spotBtn} activeOpacity={0.7} disabled>
							<Text style={styles.plus}>+</Text>
						</TouchableOpacity>
						<Text style={styles.spotLabel}>{p.key}</Text>
					</View>
				))}
		</View>
	);
}

const styles = StyleSheet.create({
	container: { flex: 1, backgroundColor: '#ffffffff', alignItems: 'center', paddingTop: 12 },
	header: { color: '#fff', fontWeight: '800', fontSize: 18, marginBottom: 8 },
	spot: { position: 'absolute', transform: [{ translateX: -18 }, { translateY: -18 }], alignItems: 'center' },
	spotBtn: { width: 50, height: 50, borderRadius: 18, backgroundColor: 'rgba(255,255,255,0.2)', borderWidth: 2, borderColor: '#000000ff', alignItems: 'center', justifyContent: 'center' },
	plus: { color: '#000000ff', fontWeight: '800', fontSize: 20, lineHeight: 22 },
	spotLabel: { marginTop: 4, color: '#000000ff', fontSize: 10, fontWeight: '700' },
});

