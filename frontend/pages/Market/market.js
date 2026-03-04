import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Alert, TextInput, Dimensions } from 'react-native';
const { width: SCREEN_WIDTH } = Dimensions.get('window');
import { useLeague } from '../../context/LeagueContext';
import authService, { authenticatedFetch } from '../../services/authService';
import withAuth from '../../components/withAuth';
import Player from '../../components/player';

function Market({ navigation }) {
	const { selectedLeague, setSelectedPlayer } = useLeague();
	const [marketPlayers, setMarketPlayers] = useState([]);
	const [loading, setLoading] = useState(false);
	const [bidAmounts, setBidAmounts] = useState({});
	const [userTeam, setUserTeam] = useState(null);

	const formatNumber = (num) => {
		if (!num && num !== 0) return '0';
		return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
	};

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

	const loadUserTeam = async () => {
		if (!selectedLeague?.id) return null;
		try {
			const user = await authService.getCurrentUser();
			if (!user?.id) return null;
			
			const res = await authenticatedFetch(`/api/v1/teams/my-team/${selectedLeague.id}/${user.id}`);
			if (!res.ok) {
				const errorText = await res.text();
				console.error('Error del servidor al cargar equipo:', errorText);
				return null;
			}
			setUserTeam(await res.json());
			
		} catch (e) {
			console.error('Error cargando equipo:', e);
			return null;
		}
	};

	useEffect(() => {
		loadMarketPlayers();
		loadUserTeam();
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
				setBidAmounts({ ...bidAmounts, [marketPlayerId]: '' });
				await loadUserTeam();
				await loadMarketPlayers();
			} else {
				const data = await res.json();
				Alert.alert('Error', data?.error || 'No se pudo realizar la puja');
			}
		} catch (e) {
			Alert.alert('Error', 'Error al pujar');
		}
	};

	const cancelBid = async (marketPlayerId) => {
		Alert.alert(
			'Cancelar puja',
			'¿Estás seguro de que quieres cancelar tu puja?',
			[
				{ text: 'No', style: 'cancel' },
				{
					text: 'Sí, cancelar',
					style: 'destructive',
					onPress: async () => {
						try {
							const res = await authenticatedFetch(
								`/api/v1/market/cancel-bid?marketPlayerId=${marketPlayerId}`,
								{ method: 'DELETE' }
							);

							if (res.ok) {
								Alert.alert('Puja cancelada', 'Tu puja ha sido cancelada');
								await loadUserTeam();
								await loadMarketPlayers();
							} else {
								const data = await res.json();
								Alert.alert('Error', data?.error || 'No se pudo cancelar la puja');
							}
						} catch (e) {
							Alert.alert('Error', 'Error al cancelar puja');
						}
					}
				}
			]
		);
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
			<View style={styles.headerContainer}>
				<Text style={styles.header}>Mercado · {selectedLeague.name}</Text>
				{userTeam && (
					<View style={styles.budgetContainer}>
						<View style={styles.budgetRow}>
							<Text style={styles.budgetLabel}>Presupuesto actual</Text>
							<Text style={styles.budgetAmount}>€{formatNumber(userTeam.budget)}</Text>
						</View>
						{Object.values(bidAmounts).some(bid => bid && parseInt(bid) > 0) && (
							<View style={[styles.budgetRow, styles.budgetPreview]}>
								<Text style={styles.budgetPreviewLabel}>Si pujas ahora</Text>
								<Text style={styles.budgetPreviewAmount}>
									€{formatNumber(userTeam.budget - Math.max(...Object.values(bidAmounts).map(bid => parseInt(bid) || 0)))}
								</Text>
							</View>
						)}
					</View>
				)}
			</View>

			{loading && <Text style={styles.loadingText}>Cargando jugadores...</Text>}

			<FlatList
				data={marketPlayers}
				keyExtractor={(item) => String(item.id)}
				renderItem={({ item }) => (
					<View style={styles.playerRow}>
					<TouchableOpacity onPress={() => { setSelectedPlayer(item.player); navigation.navigate('PlayerStats'); }} activeOpacity={0.7}>
					<Player
						name={item.player.fullName ?? item.player.name}
						avatar={item.player.avatarUrl ? { uri: item.player.avatarUrl } : null}
						teamId ={item.player.teamId}
						size={60}
					/>
					</TouchableOpacity>
						<View style={styles.playerInfo}>
							<Text style={styles.playerName}>{item.player.fullName ?? item.player.name}</Text>
							<Text style={styles.playerMeta}>
								{item.player.position} · Valor: €{item.player.marketValue.toLocaleString('es-ES')}
							</Text>
							<Text style={styles.timeText}>
								Expira: {new Date(item.auctionEndTime).toLocaleString()}
							</Text>
						</View>
						<View style={styles.bidContainer}>
						{item.hasBid ? (
							<View style={styles.activeBidContainer}>
								<Text style={styles.activeBidLabel}>Tu puja:</Text>
								<Text style={styles.activeBidAmount}>€{formatNumber(item.myBid)}</Text>
								<TouchableOpacity
									style={styles.cancelBtn}
									onPress={() => cancelBid(item.id)}
								>
									<Text style={styles.cancelBtnText}>Cancelar</Text>
								</TouchableOpacity>
							</View>
						) : (
							<>
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
							</>
						)}
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
	container: { flex: 1, backgroundColor: '#fff', padding: 12 },
	headerContainer: { marginBottom: 16 },
	header: { fontSize: SCREEN_WIDTH < 380 ? 18 : 22, fontWeight: '800', color: '#1a5c3a' },
	budgetContainer: { 
		marginTop: 12, 
		padding: 14, 
		backgroundColor: '#e8f5e9', 
		borderRadius: 10,
		borderWidth: 2,
		borderColor: '#4CAF50',
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 2 },
		shadowOpacity: 0.1,
		shadowRadius: 4,
		elevation: 3
	},
	budgetRow: { marginBottom: 6 },
	budgetLabel: { fontSize: 14, color: '#2e7d32', fontWeight: '700', marginBottom: 2 },
	budgetAmount: { fontSize: 26, fontWeight: '900', color: '#1b5e20' },
	budgetPreview: { 
		marginTop: 8, 
		paddingTop: 10, 
		borderTopWidth: 1, 
		borderTopColor: '#81c784',
		marginBottom: 0
	},
	budgetPreviewLabel: { fontSize: 13, color: '#f57c00', fontWeight: '600', marginBottom: 2 },
	budgetPreviewAmount: { fontSize: 22, fontWeight: '800', color: '#ef6c00' },
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
	playerInfo: { flex: 1, marginLeft: 8, flexShrink: 1 },
	playerName: { fontSize: SCREEN_WIDTH < 380 ? 13 : 15, fontWeight: '700' },
	playerMeta: { fontSize: 11, color: '#555', marginTop: 2 },
	bidderText: { fontSize: 11, color: '#4CAF50', marginTop: 2 },
	timeText: { fontSize: 10, color: '#999', marginTop: 2 },
	bidContainer: { flexDirection: 'column', alignItems: 'flex-end', minWidth: SCREEN_WIDTH < 380 ? 80 : 100 },
	activeBidContainer: {
		alignItems: 'center',
		padding: 8,
		backgroundColor: '#fff3e0',
		borderRadius: 8,
		borderWidth: 1,
		borderColor: '#ff9800'
	},
	activeBidLabel: { fontSize: 10, color: '#f57c00', fontWeight: '600' },
	activeBidAmount: { fontSize: 16, fontWeight: '800', color: '#e65100', marginVertical: 4 },
	currentBidText: { fontSize: 10, color: '#666', marginBottom: 4, fontWeight: '600' },
	bidInput: {
		borderWidth: 1,
		borderColor: '#ccc',
		borderRadius: 6,
		paddingHorizontal: 8,
		paddingVertical: 4,
		width: SCREEN_WIDTH < 380 ? 64 : 80,
		marginBottom: 6,
		textAlign: 'center'
	},
	bidBtn: {
		backgroundColor: '#4CAF50',
		paddingVertical: 6,
		paddingHorizontal: 12,
		borderRadius: 6
	},
	bidBtnText: { color: '#fff', fontWeight: '700', fontSize: 12 },
	cancelBtn: {
		backgroundColor: '#f44336',
		paddingVertical: 4,
		paddingHorizontal: 10,
		borderRadius: 6,
		marginTop: 4
	},
	cancelBtnText: { color: '#fff', fontWeight: '700', fontSize: 11 }
});
