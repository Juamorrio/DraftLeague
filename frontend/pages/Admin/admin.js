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
import { authenticatedFetch } from '../../services/authService';
import withAuth from '../../components/withAuth';

function Admin() {
	const [stats, setStats] = useState(null);
	const [users, setUsers] = useState([]);
	const [leagues, setLeagues] = useState([]);
	const [loading, setLoading] = useState(false);
	const [activeTab, setActiveTab] = useState('stats');
	const [editingUser, setEditingUser] = useState(null);
	const [selectedRole, setSelectedRole] = useState('USER');

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
		try {
			const res = await authenticatedFetch(`/api/v1/admin/market/${leagueId}/refresh`, {
				method: 'POST'
			});
			if (res.ok) {
				Alert.alert('Éxito', 'Mercado refrescado correctamente');
			} else {
				Alert.alert('Error', 'No se pudo refrescar el mercado');
			}
		} catch (e) {
			Alert.alert('Error', 'Error al refrescar mercado');
		}
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
	btnSaveText: { fontSize: 14, fontWeight: '700', color: '#fff' }
});
