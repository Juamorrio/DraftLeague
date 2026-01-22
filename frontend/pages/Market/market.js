import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Alert, TextInput } from 'react-native';
import { useLeague } from '../../context/LeagueContext';
import { authenticatedFetch } from '../../services/authService';
import withAuth from '../../components/withAuth';
import Player from '../../components/player';

function Market() {
	const { selectedLeague } = useLeague();
	const [marketPlayers, setMarketPlayers] = useState([]);
	const [loading, setLoading] = useState(false);
	const [bidAmounts, setBidAmounts] = useState({});

	const loadMarketPlayers = async () => {
		if (!selectedLeague?.id) return;
		setLoading(true);
		try {
			console.log('Cargando mercado para liga:', selectedLeague.id);
			const res = await authenticatedFetch(`/api/v1/market?leagueId=${selectedLeague.id}`);
			console.log('Respuesta del servidor:', res.status);
			
			if (!res.ok) {
				const errorText = await res.text();
				console.error('Error del servidor:', errorText);
				throw new Error('Error al cargar mercado: ' + errorText);
			}
			const json = await res.json();
			setMarketPlayers(Array.isArray(json) ? json : []);
		} catch (e) {
			console.error('Error cargando mercado:', e);
			Alert.alert('Error', 'No se pudieron cargar jugadores del mercado: ' + e.message);
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		loadMarketPlayers();
	}, [selectedLeague?.id]);

	const placeBid = async (marketPlayerId) => {
		const bidAmount = bidAmounts[marketPlayerId];
		if (!bidAmount || bidAmount <= 0) {
			Alert.alert('Error', 'Ingresa una cantidad válida');
			return;
		}

		try {
			const res = await authenticatedFetch(
				`/api/v1/market/bid?marketPlayerId=${marketPlayerId}&bidAmount=${bidAmount}`,
				{ method: 'POST' }
			);

			if (res.ok) {
				Alert.alert('Éxito', 'Puja realizada correctamente');
				loadMarketPlayers();
				setBidAmounts({ ...bidAmounts, [marketPlayerId]: '' });
			} else {
				const data = await res.json();
				Alert.alert('Error', data?.error || 'No se pudo realizar la puja');
			}
		} catch (e) {
			Alert.alert('Error', 'Error al pujar');
		}
	};

	if (!selectedLeague) {
		return (
			<View style={styles.container}>
				<Text style={styles.emptyText}>Selecciona una liga primero</Text>
			</View>
		);
	}

	return (
		<View style={styles.container}>
			<Text style={styles.header}>Mercado · {selectedLeague.name}</Text>

			{loading && <Text style={styles.loadingText}>Cargando jugadores...</Text>}

			<FlatList
				data={marketPlayers}
				keyExtractor={(item) => String(item.id)}
				renderItem={({ item }) => (
					<View style={styles.playerRow}>
						<Player
							name={item.player.fullName ?? item.player.name}
							avatar={item.player.avatarUrl ? { uri: item.player.avatarUrl } : null}
							teamId ={item.player.teamId}
							size={60}
						/>
						<View style={styles.playerInfo}>
							<Text style={styles.playerName}>{item.player.fullName ?? item.player.name}</Text>
							<Text style={styles.playerMeta}>
								{item.player.position} · Puja actual: €{item.currentBid.toLocaleString()}
							</Text>
							<Text style={styles.timeText}>
								Expira: {new Date(item.auctionEndTime).toLocaleString()}
							</Text>
						</View>
						<View style={styles.bidContainer}>
							<TextInput
								style={styles.bidInput}
								placeholder="€"
								keyboardType="numeric"
								value={bidAmounts[item.id] || ''}
								onChangeText={(text) => setBidAmounts({ ...bidAmounts, [item.id]: text })}
							/>
							<TouchableOpacity
								style={styles.bidBtn}
								onPress={() => placeBid(item.id)}
							>
								<Text style={styles.bidBtnText}>Pujar</Text>
							</TouchableOpacity>
						</View>
					</View>
				)}
				ItemSeparatorComponent={() => <View style={{ height: 12 }} />}
				ListEmptyComponent={
					!loading && <Text style={styles.emptyText}>No hay jugadores en el mercado</Text>
				}
			/>
		</View>
	);
}

export default withAuth(Market);

const styles = StyleSheet.create({
	container: { flex: 1, backgroundColor: '#fff', padding: 16 },
	header: { fontSize: 24, fontWeight: '800', marginBottom: 16, color: '#1a5c3a' },
	loadingText: { fontSize: 16, color: '#888', textAlign: 'center', marginTop: 20 },
	emptyText: { fontSize: 16, color: '#888', textAlign: 'center', marginTop: 40 },
	playerRow: {
		flexDirection: 'row',
		alignItems: 'center',
		padding: 12,
		borderRadius: 8,
		borderWidth: 1,
		borderColor: '#ddd',
		backgroundColor: '#f9f9f9'
	},
	playerInfo: { flex: 1, marginLeft: 12 },
	playerName: { fontSize: 16, fontWeight: '700' },
	playerMeta: { fontSize: 12, color: '#555', marginTop: 4 },
	bidderText: { fontSize: 11, color: '#4CAF50', marginTop: 2 },
	timeText: { fontSize: 10, color: '#999', marginTop: 2 },
	bidContainer: { flexDirection: 'column', alignItems: 'flex-end' },
	bidInput: {
		borderWidth: 1,
		borderColor: '#ccc',
		borderRadius: 6,
		paddingHorizontal: 8,
		paddingVertical: 4,
		width: 80,
		marginBottom: 6,
		textAlign: 'center'
	},
	bidBtn: {
		backgroundColor: '#4CAF50',
		paddingVertical: 6,
		paddingHorizontal: 12,
		borderRadius: 6
	},
	bidBtnText: { color: '#fff', fontWeight: '700', fontSize: 12 }
});
