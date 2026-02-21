import React, { useState, useEffect } from 'react';
import {
	View,
	Text,
	StyleSheet,
	ScrollView,
	TouchableOpacity,
	Alert,
	ActivityIndicator,
	FlatList,
	Modal,
	TextInput
} from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { authenticatedFetch } from '../../services/authService';
import withAuth from '../../components/withAuth';
import { useMatches } from '../../context/MatchesContext';

function Admin() {
	const [stats, setStats] = useState(null);
	const [users, setUsers] = useState([]);
	const [leagues, setLeagues] = useState([]);
	const [loading, setLoading] = useState(false);
	const [activeTab, setActiveTab] = useState('stats');
	const [editingUser, setEditingUser] = useState(null);
	const [selectedRole, setSelectedRole] = useState('USER');
	const [importingPlayers, setImportingPlayers] = useState(false);
	const [syncingMatches, setSyncingMatches] = useState(false);
	const [syncingPlayers, setSyncingPlayers] = useState(false);
	const [selectedGameweek, setSelectedGameweek] = useState(1);
	const [recalculating, setRecalculating] = useState(false);
	const [updatingPlayerPoints, setUpdatingPlayerPoints] = useState(false);
	const { loadMatches } = useMatches();

	useEffect(() => {
		loadStats();
		loadUsers();
		loadLeagues();
	}, []);

	const loadStats = async () => {
		setLoading(true);
		try {
			const res = await authenticatedFetch('/api/v1/admin/stats');
			if (res.ok) {
				const data = await res.json();
				setStats(data);
			} else {
				Alert.alert('Error', 'No tienes permisos de administrador');
			}
		} catch (e) {
			Alert.alert('Error', 'No se pudieron cargar las estadísticas');
		} finally {
			setLoading(false);
		}
	};

	const loadUsers = async () => {
		try {
			const res = await authenticatedFetch('/api/v1/admin/users');
			if (res.ok) {
				const data = await res.json();
				setUsers(data);
			}
		} catch (e) {
			console.error('Error cargando usuarios:', e);
		}
	};

	const loadLeagues = async () => {
		try {
			const res = await authenticatedFetch('/api/v1/admin/leagues');
			if (res.ok) {
				const data = await res.json();
				setLeagues(data);
			}
		} catch (e) {
			console.error('Error cargando ligas:', e);
		}
	};

	const updateUserRole = async (userId, newRole) => {
		try {
			const res = await authenticatedFetch(`/api/v1/admin/users/${userId}/role`, {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ role: newRole })
			});

			if (res.ok) {
				Alert.alert('Éxito', 'Rol actualizado correctamente');
				loadUsers();
				setEditingUser(null);
			} else {
				Alert.alert('Error', 'No se pudo actualizar el rol');
			}
		} catch (e) {
			Alert.alert('Error', 'Error al actualizar rol');
		}
	};

	const deleteUser = async (userId) => {
		Alert.alert(
			'Confirmar',
			'¿Estás seguro de eliminar este usuario?',
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Eliminar',
					style: 'destructive',
					onPress: async () => {
						try {
							const res = await authenticatedFetch(`/api/v1/admin/users/${userId}`, {
								method: 'DELETE'
							});
							if (res.ok) {
								Alert.alert('Éxito', 'Usuario eliminado');
								loadUsers();
							}
						} catch (e) {
							Alert.alert('Error', 'No se pudo eliminar el usuario');
						}
					}
				}
			]
		);
	};

	const deleteLeague = async (leagueId) => {
		Alert.alert(
			'Confirmar',
			'¿Estás seguro de eliminar esta liga?',
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Eliminar',
					style: 'destructive',
					onPress: async () => {
						try {
							const res = await authenticatedFetch(`/api/v1/admin/leagues/${leagueId}`, {
								method: 'DELETE'
							});
							if (res.ok) {
								Alert.alert('Éxito', 'Liga eliminada');
								loadLeagues();
								loadStats();
							}
						} catch (e) {
							Alert.alert('Error', 'No se pudo eliminar la liga');
						}
					}
				}
			]
		);
	};

	const refreshMarket = async (leagueId) => {
		console.log('=== Refrescando mercado ===');
		console.log('League ID:', leagueId);
		console.log('Tipo de leagueId:', typeof leagueId);
		
		try {
			const res = await authenticatedFetch(`/api/v1/admin/market/${leagueId}/refresh`, {
				method: 'POST'
			});
			if (res.ok) {
				Alert.alert('Éxito', 'Mercado refrescado correctamente');
			} else {
				const errorData = await res.json().catch(() => ({}));
				console.error('Error del servidor:', errorData);
				Alert.alert('Error', errorData.error || 'No se pudo refrescar el mercado');
			}
		} catch (e) {
			console.error('Error al refrescar mercado:', e);
			Alert.alert('Error', 'Error al refrescar mercado: ' + e.message);
		}
	};

	const importPlayers = async () => {
		Alert.alert(
			'Importar Jugadores',
			'¿Estás seguro de importar/actualizar los jugadores desde el JSON? Esta operación puede tardar varios segundos.',
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Importar',
					onPress: async () => {
						setImportingPlayers(true);
						try {
							const res = await authenticatedFetch('/api/v1/admin/import-players', {
								method: 'POST'
							});
							if (res.ok) {
								const message = await res.text();
								Alert.alert('Éxito', message);
								loadStats(); 
							} else {
								const error = await res.text();
								Alert.alert('Error', error || 'No se pudieron importar los jugadores');
							}
						} catch (e) {
							Alert.alert('Error', 'Error al importar jugadores: ' + e.message);
						} finally {
							setImportingPlayers(false);
						}
					}
				}
			]
		);
	};

	const syncMatches = async () => {
		Alert.alert(
			'Sincronizar Partidos',
			'¿Estás seguro de sincronizar los partidos desde API-Football? Esta operación puede tardar varios segundos.',
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Sincronizar',
					onPress: async () => {
						setSyncingMatches(true);
						try {
							const res = await authenticatedFetch('/api/v1/admin/sync-matches', {
								method: 'POST'
							});
							if (res.ok) {
								const data = await res.json();
								Alert.alert('Éxito', data.message);
								await loadMatches();
							} else {
								const error = await res.json();
								Alert.alert('Error', error.error || 'No se pudieron sincronizar los partidos');
							}
						} catch (e) {
							Alert.alert('Error', 'Error al sincronizar partidos: ' + e.message);
						} finally {
							setSyncingMatches(false);
						}
					}
				}
			]
		);
	};

	const syncPlayers = async () => {
		Alert.alert(
			'Sincronizar Jugadores',
			'¿Estás seguro de sincronizar los jugadores desde API-Football? Esta operación puede tardar varios segundos.',
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Sincronizar',
					onPress: async () => {
						setSyncingPlayers(true);
						try {
							const res = await authenticatedFetch('/api/v1/admin/sync-players', {
								method: 'POST'
							});
							if (res.ok) {
								const data = await res.json();
								Alert.alert('Éxito', data.message);
								loadStats(); 
							} else {
								const error = await res.json();
								Alert.alert('Error', error.error || 'No se pudieron sincronizar los jugadores');
							}
						} catch (e) {
							Alert.alert('Error', 'Error al sincronizar jugadores: ' + e.message);
						} finally {
							setSyncingPlayers(false);
						}
					}
				}
			]
		);
	};

	const recalculateGameweek = async () => {
		Alert.alert(
			'Recalcular Puntos',
			`¿Recalcular los puntos de la jornada ${selectedGameweek} para todos los equipos?`,
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Recalcular',
					onPress: async () => {
						setRecalculating(true);
						try {
							const res = await authenticatedFetch(`/api/v1/fantasy-points/gameweek/${selectedGameweek}/recalculate`, {
								method: 'POST'
							});
							if (res.ok) {
								const data = await res.json();
								Alert.alert('Completado', data.message || `Puntos de la jornada ${selectedGameweek} recalculados correctamente`);
							} else {
								const error = await res.json().catch(() => ({}));
								Alert.alert('Error', error.error || 'No se pudieron recalcular los puntos');
							}
						} catch (e) {
							Alert.alert('Error', 'Error al recalcular puntos: ' + e.message);
						} finally {
							setRecalculating(false);
						}
					}
				}
			]
		);
	};

	const updateAllPlayerPoints = async () => {
		Alert.alert(
			'Actualizar Puntos de Jugadores',
			'¿Actualizar los puntos totales de TODOS los jugadores?',
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Actualizar',
					onPress: async () => {
						setUpdatingPlayerPoints(true);
						try {
							const res = await authenticatedFetch('/api/v1/fantasy-points/players/update-all', {
								method: 'POST'
							});
							if (res.ok) {
								const data = await res.json();
								Alert.alert('Completado', data.message || 'Puntos de jugadores actualizados correctamente');
							} else {
								const error = await res.json().catch(() => ({}));
								Alert.alert('Error', error.error || 'No se pudieron actualizar los puntos');
							}
						} catch (e) {
							Alert.alert('Error', 'Error al actualizar puntos: ' + e.message);
						} finally {
							setUpdatingPlayerPoints(false);
						}
					}
				}
			]
		);
	};

	if (loading && !stats) {
		return (
			<View style={[styles.container, { justifyContent: 'center', alignItems: 'center' }]}>
				<ActivityIndicator size="large" color="#1a5c3a" />
				<Text style={styles.loadingText}>Cargando...</Text>
			</View>
		);
	}

	return (
		<View style={styles.container}>
			<View style={styles.header}>
				<Text style={styles.headerTitle}>Panel de Administración</Text>
			</View>

			<View style={styles.tabs}>
				<TouchableOpacity
					style={[styles.tab, activeTab === 'stats' && styles.tabActive]}
					onPress={() => setActiveTab('stats')}
				>
					<Text style={[styles.tabText, activeTab === 'stats' && styles.tabTextActive]}>
						Estadísticas
					</Text>
				</TouchableOpacity>
				<TouchableOpacity
					style={[styles.tab, activeTab === 'users' && styles.tabActive]}
					onPress={() => setActiveTab('users')}
				>
					<Text style={[styles.tabText, activeTab === 'users' && styles.tabTextActive]}>
						Usuarios
					</Text>
				</TouchableOpacity>
				<TouchableOpacity
					style={[styles.tab, activeTab === 'leagues' && styles.tabActive]}
					onPress={() => setActiveTab('leagues')}
				>
					<Text style={[styles.tabText, activeTab === 'leagues' && styles.tabTextActive]}>
						Ligas
					</Text>
				</TouchableOpacity>
				<TouchableOpacity
					style={[styles.tab, activeTab === 'players' && styles.tabActive]}
					onPress={() => setActiveTab('players')}
				>
					<Text style={[styles.tabText, activeTab === 'players' && styles.tabTextActive]}>
						Jugadores
					</Text>
				</TouchableOpacity>
				<TouchableOpacity
					style={[styles.tab, activeTab === 'matches' && styles.tabActive]}
					onPress={() => setActiveTab('matches')}
				>
					<Text style={[styles.tabText, activeTab === 'matches' && styles.tabTextActive]}>
						Partidos
					</Text>
				</TouchableOpacity>
				<TouchableOpacity
					style={[styles.tab, activeTab === 'points' && styles.tabActive]}
					onPress={() => setActiveTab('points')}
				>
					<Text style={[styles.tabText, activeTab === 'points' && styles.tabTextActive]}>
						Puntos
					</Text>
				</TouchableOpacity>
			</View>

			<View style={styles.content}>
				{activeTab === 'stats' && stats && (
					<ScrollView>
						<View style={styles.statsContainer}>
							<View style={styles.statCard}>
								<Text style={styles.statValue}>{stats.totalUsers}</Text>
								<Text style={styles.statLabel}>Usuarios</Text>
							</View>
							<View style={styles.statCard}>
								<Text style={styles.statValue}>{stats.totalLeagues}</Text>
								<Text style={styles.statLabel}>Ligas</Text>
							</View>
							<View style={styles.statCard}>
								<Text style={styles.statValue}>{stats.totalPlayers}</Text>
								<Text style={styles.statLabel}>Jugadores</Text>
							</View>
						</View>
					</ScrollView>
				)}

				{activeTab === 'users' && (
					<FlatList
						data={users}
						keyExtractor={(item) => String(item.id)}
						contentContainerStyle={{ padding: 16 }}
						renderItem={({ item }) => (
							<View style={styles.userCard}>
								<View style={styles.userInfo}>
									<Text style={styles.userName}>{item.displayName}</Text>
									<Text style={styles.userMeta}>@{item.username} · {item.email}</Text>
									<Text style={styles.userRole}>Rol: {item.role}</Text>
								</View>
								<View style={styles.userActions}>
									<TouchableOpacity
										style={styles.btnEdit}
										onPress={() => {
											setEditingUser(item);
											setSelectedRole(item.role);
										}}
									>
										<Text style={styles.btnEditText}>Editar</Text>
									</TouchableOpacity>
									<TouchableOpacity
										style={styles.btnDelete}
										onPress={() => deleteUser(item.id)}
									>
										<Text style={styles.btnDeleteText}>Eliminar</Text>
									</TouchableOpacity>
								</View>
							</View>
						)}
						ItemSeparatorComponent={() => <View style={{ height: 10 }} />}
					/>
				)}

				{activeTab === 'leagues' && (
					<FlatList
						data={leagues}
						keyExtractor={(item) => String(item.id)}
						contentContainerStyle={{ padding: 16 }}
						renderItem={({ item }) => (
							<View style={styles.leagueCard}>
								<View style={styles.leagueInfo}>
									<Text style={styles.leagueName}>{item.name}</Text>
									<Text style={styles.leagueMeta}>
										Código: {item.code} · Max: {item.maxTeams} · Presupuesto: €{item.initialBudget.toLocaleString('es-ES')}
									</Text>
								</View>
								<View style={styles.leagueActions}>
									<TouchableOpacity
										style={styles.btnRefresh}
										onPress={() => refreshMarket(item.id)}
									>
										<Text style={styles.btnRefreshText}>Refrescar Mercado</Text>
									</TouchableOpacity>
									<TouchableOpacity
										style={styles.btnDelete}
										onPress={() => deleteLeague(item.id)}
									>
										<Text style={styles.btnDeleteText}>Eliminar</Text>
									</TouchableOpacity>
								</View>
							</View>
						)}
						ItemSeparatorComponent={() => <View style={{ height: 10 }} />}
					/>
				)}

				{activeTab === 'players' && (
					<ScrollView contentContainerStyle={{ padding: 16 }}>
						<View style={styles.playersContainer}>
							<Text style={styles.playersTitle}>Sincronización de Jugadores</Text>
							<Text style={styles.playersDescription}>
								Sincroniza los jugadores desde API-Football API. Esta operación actualizará el archivo 
								players_data.json con los datos más recientes de LaLiga.
							</Text>
							<TouchableOpacity
								style={[styles.btnImport, syncingPlayers && styles.btnImportDisabled]}
								onPress={syncPlayers}
								disabled={syncingPlayers}
							>
								{syncingPlayers ? (
									<>
										<ActivityIndicator size="small" color="#fff" />
										<Text style={styles.btnImportText}>Sincronizando...</Text>
									</>
								) : (
									<Text style={styles.btnImportText}>Sincronizar Jugadores</Text>
								)}
							</TouchableOpacity>

							<View style={{ height: 20 }} />

							<Text style={styles.playersTitle}>Importar a Base de Datos</Text>
							<Text style={styles.playersDescription}>
								Importa o actualiza jugadores desde el archivo JSON a la base de datos.
								Si un jugador ya existe, se actualizará su información.
							</Text>
							<TouchableOpacity
								style={[styles.btnImport, importingPlayers && styles.btnImportDisabled]}
								onPress={importPlayers}
								disabled={importingPlayers}
							>
								{importingPlayers ? (
									<>
										<ActivityIndicator size="small" color="#fff" />
										<Text style={styles.btnImportText}>Importando...</Text>
									</>
								) : (
									<Text style={styles.btnImportText}>Importar/Actualizar Jugadores</Text>
								)}
							</TouchableOpacity>
							{stats && (
								<View style={styles.playersStat}>
									<Text style={styles.playersStatLabel}>Total de jugadores en BD:</Text>
									<Text style={styles.playersStatValue}>{stats.totalPlayers}</Text>
								</View>
							)}
						</View>
					</ScrollView>
				)}

				{activeTab === 'matches' && (
					<ScrollView contentContainerStyle={{ padding: 16 }}>
						<View style={styles.playersContainer}>
							<Text style={styles.playersTitle}>Sincronización de Partidos</Text>
							<Text style={styles.playersDescription}>
								Sincroniza los partidos desde API-Football API. Esta operación actualizará los archivos 
								matches.json y upcoming_matches.json con los datos más recientes de LaLiga.
							</Text>
							<TouchableOpacity
								style={[styles.btnImport, syncingMatches && styles.btnImportDisabled]}
								onPress={syncMatches}
								disabled={syncingMatches}
							>
								{syncingMatches ? (
									<>
										<ActivityIndicator size="small" color="#fff" />
										<Text style={styles.btnImportText}>Sincronizando...</Text>
									</>
								) : (
									<Text style={styles.btnImportText}>Sincronizar Partidos</Text>
								)}
							</TouchableOpacity>
						</View>
					</ScrollView>
				)}

				{activeTab === 'points' && (
					<ScrollView contentContainerStyle={{ padding: 16 }}>
						<View style={styles.playersContainer}>
							<Text style={styles.playersTitle}>Recalcular Puntos por Jornada</Text>
							<Text style={styles.playersDescription}>
								Recalcula los puntos fantasy de todos los equipos para la jornada seleccionada.
								Esto actualiza los puntos individuales por jugador, los puntos totales del equipo
								y el ranking de la liga.
							</Text>

							<Text style={{ fontSize: 14, fontWeight: '600', color: '#333', marginBottom: 8 }}>
								Seleccionar Jornada:
							</Text>
							<View style={{ borderWidth: 1, borderColor: '#ddd', borderRadius: 8, marginBottom: 16, backgroundColor: '#fff' }}>
								<Picker
									selectedValue={selectedGameweek}
									onValueChange={(value) => setSelectedGameweek(value)}
								>
									{Array.from({ length: 38 }, (_, i) => i + 1).map(gw => (
										<Picker.Item key={gw} label={`Jornada ${gw}`} value={gw} />
									))}
								</Picker>
							</View>

							<TouchableOpacity
								style={[styles.btnImport, recalculating && styles.btnImportDisabled]}
								onPress={recalculateGameweek}
								disabled={recalculating}
							>
								{recalculating ? (
									<>
										<ActivityIndicator size="small" color="#fff" />
										<Text style={styles.btnImportText}>Recalculando...</Text>
									</>
								) : (
									<Text style={styles.btnImportText}>Recalcular Jornada {selectedGameweek}</Text>
								)}
							</TouchableOpacity>

							<View style={{ height: 24 }} />

							<Text style={styles.playersTitle}>Actualizar Puntos de Jugadores</Text>
							<Text style={styles.playersDescription}>
								Recalcula los puntos totales acumulados de todos los jugadores
								sumando los puntos fantasy de todas sus estadisticas de cada partido.
							</Text>
							<TouchableOpacity
								style={[styles.btnImport, updatingPlayerPoints && styles.btnImportDisabled, { backgroundColor: '#2563eb' }]}
								onPress={updateAllPlayerPoints}
								disabled={updatingPlayerPoints}
							>
								{updatingPlayerPoints ? (
									<>
										<ActivityIndicator size="small" color="#fff" />
										<Text style={styles.btnImportText}>Actualizando...</Text>
									</>
								) : (
									<Text style={styles.btnImportText}>Actualizar Puntos de Jugadores</Text>
								)}
							</TouchableOpacity>
						</View>
					</ScrollView>
				)}
			</View>

			<Modal visible={!!editingUser} transparent animationType="fade">
				<View style={styles.modalBackdrop}>
					<View style={styles.modalCard}>
						<Text style={styles.modalTitle}>Editar Usuario</Text>
						{editingUser && (
							<>
								<Text style={styles.modalUserName}>{editingUser.displayName}</Text>
								<Text style={styles.label}>Rol:</Text>
								<View style={styles.roleButtons}>
									<TouchableOpacity
										style={[
											styles.roleBtn,
											selectedRole === 'USER' && styles.roleBtnActive
										]}
										onPress={() => setSelectedRole('USER')}
									>
										<Text style={[
											styles.roleBtnText,
											selectedRole === 'USER' && styles.roleBtnTextActive
										]}>
											USER
										</Text>
									</TouchableOpacity>
									<TouchableOpacity
										style={[
											styles.roleBtn,
											selectedRole === 'ADMIN' && styles.roleBtnActive
										]}
										onPress={() => setSelectedRole('ADMIN')}
									>
										<Text style={[
											styles.roleBtnText,
											selectedRole === 'ADMIN' && styles.roleBtnTextActive
										]}>
											ADMIN
										</Text>
									</TouchableOpacity>
								</View>
								<View style={styles.modalActions}>
									<TouchableOpacity
										style={styles.btnCancel}
										onPress={() => setEditingUser(null)}
									>
										<Text style={styles.btnCancelText}>Cancelar</Text>
									</TouchableOpacity>
									<TouchableOpacity
										style={styles.btnSave}
										onPress={() => updateUserRole(editingUser.id, selectedRole)}
									>
										<Text style={styles.btnSaveText}>Guardar</Text>
									</TouchableOpacity>
								</View>
							</>
						)}
					</View>
				</View>
			</Modal>
		</View>
	);
}

export default withAuth(Admin);

const styles = StyleSheet.create({
	container: { flex: 1, backgroundColor: '#f5f5f5' },
	header: {
		backgroundColor: '#1a5c3a',
		paddingVertical: 16,
		paddingHorizontal: 20,
		borderBottomWidth: 1,
		borderBottomColor: '#ddd'
	},
	headerTitle: { color: '#fff', fontSize: 20, fontWeight: '800' },
	tabs: {
		flexDirection: 'row',
		backgroundColor: '#fff',
		borderBottomWidth: 1,
		borderBottomColor: '#ddd'
	},
	tab: {
		flex: 1,
		paddingVertical: 14,
		alignItems: 'center',
		borderBottomWidth: 2,
		borderBottomColor: 'transparent'
	},
	tabActive: { borderBottomColor: '#1a5c3a' },
	tabText: { fontSize: 14, fontWeight: '600', color: '#666' },
	tabTextActive: { color: '#1a5c3a' },
	content: { flex: 1 },
	loadingText: { marginTop: 10, color: '#666' },
	statsContainer: {
		flexDirection: 'row',
		justifyContent: 'space-around',
		marginTop: 20
	},
	statCard: {
		backgroundColor: '#fff',
		padding: 20,
		borderRadius: 12,
		alignItems: 'center',
		minWidth: 100,
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 2 },
		shadowOpacity: 0.1,
		shadowRadius: 4,
		elevation: 3
	},
	statValue: { fontSize: 32, fontWeight: '800', color: '#1a5c3a' },
	statLabel: { fontSize: 14, color: '#666', marginTop: 4 },
	userCard: {
		backgroundColor: '#fff',
		padding: 14,
		borderRadius: 10,
		flexDirection: 'row',
		justifyContent: 'space-between',
		alignItems: 'center',
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 1 },
		shadowOpacity: 0.1,
		shadowRadius: 2,
		elevation: 2
	},
	userInfo: { flex: 1 },
	userName: { fontSize: 16, fontWeight: '700', color: '#222' },
	userMeta: { fontSize: 12, color: '#666', marginTop: 2 },
	userRole: { fontSize: 12, color: '#1a5c3a', marginTop: 4, fontWeight: '600' },
	userActions: { flexDirection: 'row', gap: 8 },
	btnEdit: {
		backgroundColor: '#4CAF50',
		paddingVertical: 6,
		paddingHorizontal: 12,
		borderRadius: 6
	},
	btnEditText: { color: '#fff', fontSize: 12, fontWeight: '700' },
	btnDelete: {
		backgroundColor: '#f44336',
		paddingVertical: 6,
		paddingHorizontal: 12,
		borderRadius: 6
	},
	btnDeleteText: { color: '#fff', fontSize: 12, fontWeight: '700' },
	leagueCard: {
		backgroundColor: '#fff',
		padding: 14,
		borderRadius: 10,
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 1 },
		shadowOpacity: 0.1,
		shadowRadius: 2,
		elevation: 2
	},
	leagueInfo: { marginBottom: 10 },
	leagueName: { fontSize: 16, fontWeight: '700', color: '#222' },
	leagueMeta: { fontSize: 12, color: '#666', marginTop: 4 },
	leagueActions: { flexDirection: 'row', gap: 8 },
	btnRefresh: {
		backgroundColor: '#2196F3',
		paddingVertical: 8,
		paddingHorizontal: 12,
		borderRadius: 6,
		flex: 1
	},
	btnRefreshText: { color: '#fff', fontSize: 12, fontWeight: '700', textAlign: 'center' },
	modalBackdrop: {
		flex: 1,
		backgroundColor: 'rgba(0,0,0,0.5)',
		justifyContent: 'center',
		alignItems: 'center'
	},
	modalCard: {
		backgroundColor: '#fff',
		borderRadius: 12,
		padding: 20,
		width: '85%',
		maxWidth: 400
	},
	modalTitle: { fontSize: 18, fontWeight: '800', marginBottom: 12, color: '#222' },
	modalUserName: { fontSize: 16, fontWeight: '600', color: '#666', marginBottom: 16 },
	label: { fontSize: 14, fontWeight: '600', color: '#333', marginBottom: 8 },
	roleButtons: { flexDirection: 'row', gap: 10, marginBottom: 20 },
	roleBtn: {
		flex: 1,
		paddingVertical: 10,
		borderRadius: 8,
		borderWidth: 2,
		borderColor: '#ddd',
		alignItems: 'center'
	},
	roleBtnActive: { borderColor: '#1a5c3a', backgroundColor: '#1a5c3a' },
	roleBtnText: { fontSize: 14, fontWeight: '700', color: '#666' },
	roleBtnTextActive: { color: '#fff' },
	modalActions: { flexDirection: 'row', gap: 10, marginTop: 10 },
	btnCancel: {
		flex: 1,
		paddingVertical: 10,
		borderRadius: 8,
		borderWidth: 1,
		borderColor: '#ddd',
		alignItems: 'center'
	},
	btnCancelText: { fontSize: 14, fontWeight: '700', color: '#666' },
	btnSave: {
		flex: 1,
		paddingVertical: 10,
		borderRadius: 8,
		backgroundColor: '#1a5c3a',
		alignItems: 'center'
	},
	btnSaveText: { fontSize: 14, fontWeight: '700', color: '#fff' },
	playersContainer: {
		backgroundColor: '#fff',
		padding: 20,
		borderRadius: 12,
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 2 },
		shadowOpacity: 0.1,
		shadowRadius: 4,
		elevation: 3
	},
	playersTitle: { fontSize: 18, fontWeight: '800', color: '#222', marginBottom: 10 },
	playersDescription: { fontSize: 14, color: '#666', marginBottom: 20, lineHeight: 20 },
	btnImport: {
		backgroundColor: '#1a5c3a',
		paddingVertical: 14,
		paddingHorizontal: 20,
		borderRadius: 8,
		alignItems: 'center',
		flexDirection: 'row',
		justifyContent: 'center',
		gap: 8
	},
	btnImportDisabled: { backgroundColor: '#999', opacity: 0.7 },
	btnImportText: { color: '#fff', fontSize: 16, fontWeight: '700' },
	playersStat: {
		marginTop: 20,
		padding: 16,
		backgroundColor: '#e8f5e9',
		borderRadius: 8,
		flexDirection: 'row',
		justifyContent: 'space-between',
		alignItems: 'center'
	},
	playersStatLabel: { fontSize: 14, color: '#2e7d32', fontWeight: '600' },
	playersStatValue: { fontSize: 24, fontWeight: '800', color: '#1b5e20' }
});
