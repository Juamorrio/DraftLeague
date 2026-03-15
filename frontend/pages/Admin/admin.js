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
import { colors, fontSize, fontWeight, radius, spacing, shadow, positionBadgeColors } from '../../utils/theme';

function Admin() {
	const [stats, setStats] = useState(null);
	const [users, setUsers] = useState([]);
	const [leagues, setLeagues] = useState([]);
	const [loading, setLoading] = useState(false);
	const [activeTab, setActiveTab] = useState('stats');
	const [editingUser, setEditingUser] = useState(null);
	const [selectedRole, setSelectedRole] = useState('USER');
	const [selectedGameweek, setSelectedGameweek] = useState(1);
	// Gameweek state
	const [gameweekStatus, setGameweekStatus] = useState({ activeGameweek: null, teamsLocked: false });
	// Consolidated async-operation flags — replaces 6 separate boolean useState calls
	const [ops, setOps] = useState({
		syncingMatches: false,
		syncingPlayers: false,
		activatingGameweek: false,
		calculatingPoints: false,
		unlockingTeams: false,
		recalculatingPrices: false,
	});
	const setOp = (key, value) => setOps(prev => ({ ...prev, [key]: value }));
	// Destructure for drop-in compatibility with existing JSX references
	const { syncingMatches, syncingPlayers, activatingGameweek, calculatingPoints, unlockingTeams, recalculatingPrices } = ops;
	const { loadMatches } = useMatches();

	useEffect(() => {
		loadStats();
		loadUsers();
		loadLeagues();
		loadGameweekStatus();
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

	const syncMatches = async () => {
		Alert.alert(
			'Sincronizar Partidos',
			'¿Estás seguro de sincronizar los partidos desde API-Football? Esta operación puede tardar varios segundos.',
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Sincronizar',
					onPress: async () => {
						setOp('syncingMatches', true);
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
							setOp('syncingMatches', false);
						}
					}
				}
			]
		);
	};

	const syncPlayers = async () => {
		Alert.alert(
			'Sincronizar Jugadores',
			'¿Sincronizar jugadores desde API-Football y añadir los nuevos a la base de datos? Esta operación puede tardar varios segundos.',
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Sincronizar',
					onPress: async () => {
						setOp('syncingPlayers', true);
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
							setOp('syncingPlayers', false);
						}
					}
				}
			]
		);
	};

	const loadGameweekStatus = async () => {
		try {
			const res = await authenticatedFetch('/api/v1/admin/gameweek/status');
			if (res.ok) {
				const data = await res.json();
				setGameweekStatus(data);
			}
		} catch (e) {
			console.error('Error cargando estado de jornada:', e);
		}
	};

	const activateGameweek = async () => {
		Alert.alert(
			'Activar Jornada',
			`¿Activar la jornada ${selectedGameweek}? Los equipos quedarán BLOQUEADOS y los usuarios no podrán modificarlos.`,
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Activar y Bloquear',
					style: 'destructive',
					onPress: async () => {
						setOp('activatingGameweek', true);
						try {
							const res = await authenticatedFetch('/api/v1/admin/gameweek/activate', {
								method: 'POST',
								headers: { 'Content-Type': 'application/json' },
								body: JSON.stringify({ gameweek: selectedGameweek })
							});
							if (res.ok) {
								const data = await res.json();
								Alert.alert('✅ Jornada Activada', data.message);
								await loadGameweekStatus();
							} else {
								const error = await res.json().catch(() => ({}));
								Alert.alert('Error', error.error || 'No se pudo activar la jornada');
							}
						} catch (e) {
							Alert.alert('Error', 'Error al activar jornada: ' + e.message);
						} finally {
							setOp('activatingGameweek', false);
						}
					}
				}
			]
		);
	};

	const unlockTeams = async () => {
		Alert.alert(
			'Desbloquear Equipos',
			'¿Desbloquear los equipos? Los usuarios podrán volver a modificar sus plantillas.',
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Desbloquear',
					onPress: async () => {
						setOp('unlockingTeams', true);
						try {
							const res = await authenticatedFetch('/api/v1/admin/gameweek/unlock', {
								method: 'POST'
							});
							if (res.ok) {
								const data = await res.json();
								Alert.alert('✅ Equipos Desbloqueados', data.message);
								await loadGameweekStatus();
							} else {
								const error = await res.json().catch(() => ({}));
								Alert.alert('Error', error.error || 'No se pudo desbloquear');
							}
						} catch (e) {
							Alert.alert('Error', 'Error al desbloquear: ' + e.message);
						} finally {
							setOp('unlockingTeams', false);
						}
					}
				}
			]
		);
	};

	const calculateActiveGameweekPoints = async () => {
		if (!gameweekStatus.activeGameweek) {
			Alert.alert('Sin jornada activa', 'Activa una jornada primero.');
			return;
		}
		Alert.alert(
			'Calcular Puntos',
			`¿Obtener estadísticas de la jornada ${gameweekStatus.activeGameweek} desde API-Football y calcular los puntos fantasy de todos los equipos? Esta operación puede tardar varios minutos.`,
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Calcular',
					onPress: async () => {
						setOp('calculatingPoints', true);
						try {
							const res = await authenticatedFetch('/api/v1/admin/gameweek/calculate-points', {
								method: 'POST'
							});
							if (res.ok) {
								const data = await res.json();
								const title = data.warning ? '⚠️ Completado con advertencia' : '✅ Completado';
								const lines = [
									data.message,
									'Stats sincronizadas: ' + (data.statsPlayersSynced ?? '?'),
									'Mercado → actualizados: ' + (data.marketValueUpdated ?? 0) + ' · sin cambios: ' + (data.marketValueSkipped ?? 0),
									data.warning ? ('⚠️ ' + data.warning) : null,
								].filter(Boolean);
								Alert.alert(title, lines.join('\n'));
							} else {
								const error = await res.json().catch(() => ({}));
								Alert.alert('Error', error.error || 'No se pudieron calcular los puntos');
							}
						} catch (e) {
							Alert.alert('Error', 'Error al calcular puntos: ' + e.message);
						} finally {
							setOp('calculatingPoints', false);
						}
					}
				}
			]
		);
	};

	const recalculateMarketPrices = async () => {
		Alert.alert(
			'Recalcular precios de mercado',
			'Esto recalcula el valor de mercado de todos los jugadores. Puede tardar varios segundos.',
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Recalcular',
					onPress: async () => {
						setOp('recalculatingPrices', true);
						try {
							const res = await authenticatedFetch('/api/v1/admin/market/recalculate-prices', {
								method: 'POST'
							});
							if (res.ok) {
								const data = await res.json();
								Alert.alert(
									'✅ Precios recalculados',
									`Actualizados: ${data.updatedCount} · Sin cambios: ${data.skippedCount} · Errores: ${data.errorCount}`
								);
							} else {
								const error = await res.json().catch(() => ({}));
								Alert.alert('Error', error.error || 'No se pudieron recalcular los precios');
							}
						} catch (e) {
							Alert.alert('Error', 'Error al recalcular precios: ' + e.message);
						} finally {
							setOp('recalculatingPrices', false);
						}
					}
				}
			]
		);
	};

	if (loading && !stats) {
		return (
			<View style={[styles.container, { justifyContent: 'center', alignItems: 'center' }]}>
				<ActivityIndicator size="large" color={colors.primary} />
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
					onPress={() => { setActiveTab('points'); loadGameweekStatus(); }}
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
								Obtiene los planteles actuales de LaLiga desde API-Football y añade a la base de datos
								los jugadores que aún no existen. Los jugadores ya registrados no se modifican.
							</Text>
							<TouchableOpacity
								style={[styles.btnImport, syncingPlayers && styles.btnImportDisabled]}
								onPress={syncPlayers}
								disabled={syncingPlayers}
							>
								{syncingPlayers ? (
									<>
										<ActivityIndicator size="small" color={colors.textInverse} />
										<Text style={styles.btnImportText}>Sincronizando...</Text>
									</>
								) : (
									<Text style={styles.btnImportText}>Sincronizar Jugadores</Text>
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
							Obtiene los resultados de LaLiga desde API-Football y añade a la base de datos
							los partidos y jornadas que aún no estén registrados.
							</Text>
							<TouchableOpacity
								style={[styles.btnImport, syncingMatches && styles.btnImportDisabled]}
								onPress={syncMatches}
								disabled={syncingMatches}
							>
								{syncingMatches ? (
									<>
										<ActivityIndicator size="small" color={colors.textInverse} />
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

						{/* ── Estado actual de la jornada ── */}
						<View style={[styles.playersContainer, { marginBottom: 16 }]}>
							<Text style={styles.playersTitle}>Estado de la Jornada</Text>

							<View style={styles.statusRow}>
								<View style={styles.statusItem}>
									<Text style={styles.statusLabel}>Jornada activa</Text>
									<Text style={styles.statusValue}>
										{gameweekStatus.activeGameweek ?? '—'}
									</Text>
								</View>
								<View style={[
									styles.lockBadge,
									gameweekStatus.teamsLocked ? styles.lockBadgeLocked : styles.lockBadgeOpen
								]}>
									<Text style={styles.lockBadgeText}>
										{gameweekStatus.teamsLocked ? '🔒 EQUIPOS BLOQUEADOS' : '🔓 EQUIPOS ABIERTOS'}
									</Text>
								</View>
							</View>

							{/* Activar jornada */}
							<Text style={{ fontSize: fontSize.sm, fontWeight: fontWeight.semibold, color: colors.textPrimary, marginTop: spacing.lg, marginBottom: spacing.sm }}>
								Seleccionar jornada a activar:
							</Text>
							<View style={{ borderWidth: 1, borderColor: colors.border, borderRadius: radius.sm, marginBottom: spacing.md, backgroundColor: colors.bgCard }}>
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
								style={[styles.btnImport, { backgroundColor: colors.warningDark }, activatingGameweek && styles.btnImportDisabled]}
								onPress={activateGameweek}
								disabled={activatingGameweek}
							>
								{activatingGameweek ? (
									<>
										<ActivityIndicator size="small" color={colors.textInverse} />
										<Text style={styles.btnImportText}>Activando...</Text>
									</>
								) : (
									<Text style={styles.btnImportText}>🔒 Activar Jornada {selectedGameweek}</Text>
								)}
							</TouchableOpacity>

							<View style={{ height: 12 }} />
						{/* Desbloquear equipos */}
						<TouchableOpacity
							style={[
								styles.btnImport,
								{ backgroundColor: positionBadgeColors.DEF.bar },
								(!gameweekStatus.teamsLocked || unlockingTeams) && styles.btnImportDisabled
							]}
							onPress={unlockTeams}
							disabled={!gameweekStatus.teamsLocked || unlockingTeams}
						>
							{unlockingTeams ? (
								<>
									<ActivityIndicator size="small" color={colors.textInverse} />
									<Text style={styles.btnImportText}>Desbloqueando...</Text>
								</>
							) : (
								<Text style={styles.btnImportText}>🔓 Desbloquear Equipos</Text>
							)}
						</TouchableOpacity>

						<View style={{ height: 12 }} />
							{/* Calcular puntos de la jornada activa */}
							<TouchableOpacity
								style={[
									styles.btnImport,
								{ backgroundColor: colors.primaryDark },
									(!gameweekStatus.activeGameweek || calculatingPoints) && styles.btnImportDisabled
								]}
								onPress={calculateActiveGameweekPoints}
								disabled={!gameweekStatus.activeGameweek || calculatingPoints}
							>
								{calculatingPoints ? (
									<>
										<ActivityIndicator size="small" color={colors.textInverse} />
										<Text style={styles.btnImportText}>Calculando...</Text>
									</>
								) : (
									<Text style={styles.btnImportText}>
										⚡ Calcular Puntos{gameweekStatus.activeGameweek ? ` — J${gameweekStatus.activeGameweek}` : ''}
									</Text>
								)}
							</TouchableOpacity>

							<View style={{ height: 12 }} />
							<TouchableOpacity
								style={[
									styles.btnImport,
									{ backgroundColor: colors.accent },
									recalculatingPrices && styles.btnImportDisabled
								]}
								onPress={recalculateMarketPrices}
								disabled={recalculatingPrices}
							>
								{recalculatingPrices ? (
									<>
										<ActivityIndicator size="small" color={colors.textInverse} />
										<Text style={styles.btnImportText}>Recalculando...</Text>
									</>
								) : (
									<Text style={styles.btnImportText}>📈 Recalcular Valores de Mercado</Text>
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
	container: { flex: 1, backgroundColor: colors.bgApp },
	header: {
		backgroundColor: colors.primaryDeep, paddingVertical: spacing.lg,
		paddingHorizontal: spacing.xl, borderBottomWidth: 1, borderBottomColor: colors.primaryDark,
	},
	headerTitle: { color: colors.textInverse, fontSize: fontSize.xl, fontWeight: fontWeight.extrabold },
	tabs: { flexDirection: 'row', backgroundColor: colors.bgCard, borderBottomWidth: 1, borderBottomColor: colors.border },
	tab: { flex: 1, paddingVertical: spacing.lg, alignItems: 'center', borderBottomWidth: 2, borderBottomColor: 'transparent' },
	tabActive: { borderBottomColor: colors.primaryDark },
	tabText: { fontSize: fontSize.sm, fontWeight: fontWeight.semibold, color: colors.textMuted },
	tabTextActive: { color: colors.primaryDark },
	content: { flex: 1 },
	loadingText: { marginTop: 10, color: colors.textSecondary },
	statsContainer: { flexDirection: 'row', justifyContent: 'space-around', marginTop: spacing.xl },
	statCard: { backgroundColor: colors.bgCard, padding: spacing.xl, borderRadius: radius.lg, alignItems: 'center', minWidth: 100, ...shadow.sm },
	statValue: { fontSize: 32, fontWeight: fontWeight.extrabold, color: colors.primaryDark },
	statLabel: { fontSize: fontSize.sm, color: colors.textSecondary, marginTop: 4 },
	userCard: {
		backgroundColor: colors.bgCard, padding: spacing.lg, borderRadius: radius.lg,
		flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', ...shadow.sm,
	},
	userInfo: { flex: 1 },
	userName: { fontSize: fontSize.md, fontWeight: fontWeight.bold, color: colors.textPrimary },
	userMeta: { fontSize: fontSize.sm, color: colors.textSecondary, marginTop: 2 },
	userRole: { fontSize: fontSize.sm, color: colors.primaryDark, marginTop: 4, fontWeight: fontWeight.semibold },
	userActions: { flexDirection: 'row', gap: spacing.sm },
	btnEdit: { backgroundColor: colors.primary, paddingVertical: 6, paddingHorizontal: spacing.md, borderRadius: radius.sm },
	btnEditText: { color: colors.textInverse, fontSize: fontSize.sm, fontWeight: fontWeight.bold },
	btnDelete: { backgroundColor: colors.danger, paddingVertical: 6, paddingHorizontal: spacing.md, borderRadius: radius.sm },
	btnDeleteText: { color: colors.textInverse, fontSize: fontSize.sm, fontWeight: fontWeight.bold },
	leagueCard: { backgroundColor: colors.bgCard, padding: spacing.lg, borderRadius: radius.lg, ...shadow.sm },
	leagueInfo: { marginBottom: spacing.sm },
	leagueName: { fontSize: fontSize.md, fontWeight: fontWeight.bold, color: colors.textPrimary },
	leagueMeta: { fontSize: fontSize.sm, color: colors.textSecondary, marginTop: 4 },
	leagueActions: { flexDirection: 'row', gap: spacing.sm },
	btnRefresh: { backgroundColor: colors.accent, paddingVertical: spacing.sm, paddingHorizontal: spacing.md, borderRadius: radius.sm, flex: 1 },
	btnRefreshText: { color: colors.textInverse, fontSize: fontSize.sm, fontWeight: fontWeight.bold, textAlign: 'center' },
	modalBackdrop: { flex: 1, backgroundColor: colors.overlay, justifyContent: 'center', alignItems: 'center' },
	modalCard: { backgroundColor: colors.bgCard, borderRadius: radius.lg, padding: spacing.xl, width: '85%', maxWidth: 400, ...shadow.lg },
	modalTitle: { fontSize: fontSize.lg, fontWeight: fontWeight.extrabold, marginBottom: spacing.md, color: colors.textPrimary },
	modalUserName: { fontSize: fontSize.md, fontWeight: fontWeight.semibold, color: colors.textSecondary, marginBottom: spacing.lg },
	label: { fontSize: fontSize.sm, fontWeight: fontWeight.semibold, color: colors.textSecondary, marginBottom: spacing.sm },
	roleButtons: { flexDirection: 'row', gap: spacing.sm, marginBottom: spacing.xl },
	roleBtn: { flex: 1, paddingVertical: spacing.sm, borderRadius: radius.md, borderWidth: 2, borderColor: colors.border, alignItems: 'center' },
	roleBtnActive: { borderColor: colors.primaryDark, backgroundColor: colors.primaryDark },
	roleBtnText: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textSecondary },
	roleBtnTextActive: { color: colors.textInverse },
	modalActions: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.sm },
	btnCancel: { flex: 1, paddingVertical: spacing.sm, borderRadius: radius.md, borderWidth: 1, borderColor: colors.border, alignItems: 'center' },
	btnCancelText: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textSecondary },
	btnSave: { flex: 1, paddingVertical: spacing.sm, borderRadius: radius.md, backgroundColor: colors.primaryDark, alignItems: 'center' },
	btnSaveText: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textInverse },
	playersContainer: { backgroundColor: colors.bgCard, padding: spacing.xl, borderRadius: radius.lg, ...shadow.sm },
	playersTitle: { fontSize: fontSize.lg, fontWeight: fontWeight.extrabold, color: colors.textPrimary, marginBottom: spacing.sm },
	playersDescription: { fontSize: fontSize.sm, color: colors.textSecondary, marginBottom: spacing.xl, lineHeight: 20 },
	btnImport: {
		backgroundColor: colors.primaryDark, paddingVertical: spacing.lg, paddingHorizontal: spacing.xl,
		borderRadius: radius.md, alignItems: 'center', flexDirection: 'row', justifyContent: 'center', gap: spacing.sm,
	},
	btnImportDisabled: { backgroundColor: colors.textMuted, opacity: 0.7 },
	btnImportText: { color: colors.textInverse, fontSize: fontSize.md, fontWeight: fontWeight.bold },
	playersStat: {
		marginTop: spacing.xl, padding: spacing.lg, backgroundColor: colors.successBg,
		borderRadius: radius.md, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
	},
	playersStatLabel: { fontSize: fontSize.sm, color: colors.primaryDark, fontWeight: fontWeight.semibold },
	playersStatValue: { fontSize: 24, fontWeight: fontWeight.extrabold, color: colors.primaryDeep },
	statusRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 },
	statusItem: {},
	statusLabel: { fontSize: fontSize.xs, color: colors.textSecondary, fontWeight: fontWeight.bold, textTransform: 'uppercase' },
	statusValue: { fontSize: 32, fontWeight: fontWeight.extrabold, color: colors.primaryDark },
	lockBadge: { paddingVertical: 6, paddingHorizontal: spacing.md, borderRadius: radius.pill },
	lockBadgeLocked: { backgroundColor: colors.dangerBg, borderWidth: 1, borderColor: colors.danger },
	lockBadgeOpen: { backgroundColor: colors.successBg, borderWidth: 1, borderColor: colors.primaryMuted },
	lockBadgeText: { fontSize: fontSize.sm, fontWeight: fontWeight.bold },
});
