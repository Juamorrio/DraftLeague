import React, { useMemo, useState, useEffect } from 'react';
import {
	View,
	Text,
	Modal,
	TextInput,
	TouchableOpacity,
	StyleSheet,
	Switch,
	ActivityIndicator,
	Alert,
	ScrollView,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import authService, { refresh } from '../../services/authService';
import AsyncStorage from '@react-native-async-storage/async-storage';
import FormLeague from './formLeague';
import RankingLeague from './rankingLeague';
import { useLeague } from '../../context/LeagueContext';
import JoinLeagueModal from './joinLeagueModal';
import LeagueCard from '../../components/Leagues/LeagueCard';
import { colors, fontSize, fontWeight, radius, spacing, shadow } from '../../utils/theme';

const DRAFT_KEY = 'leagues.createDraft';

function Leagues({ navigation }) {
	const [creating, setCreating] = useState(false);
	const [editing, setEditing] = useState(false);
	const [editingLeague, setEditingLeague] = useState(null);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState('');
	const [user, setUser] = useState(null);

	const [name, setName] = useState('');
	const [description, setDescription] = useState('');
	const [maxTeams, setMaxTeams] = useState('10');
	const [initialBudget, setInitialBudget] = useState('100000000');
	const [marketEndHour, setMarketEndHour] = useState('20:00');
	const [captainEnable, setCaptainEnable] = useState(true);
	const [leagues, setLeagues] = useState([]);
	const [fieldErrors, setFieldErrors] = useState({});

	const [joinVisible, setJoinVisible] = useState(false);
	const [joinCode, setJoinCode] = useState('');
	const [joinError, setJoinError] = useState('');
	const [joinLoading, setJoinLoading] = useState(false);
	const [assignedVisible, setAssignedVisible] = useState(false);
	const [assignedPlayers, setAssignedPlayers] = useState([]);
	const [assignedLeagueName, setAssignedLeagueName] = useState('');

	const { selectedLeague, setSelectedLeague, viewUser, setViewUser } = useLeague();

	useEffect(() => {
		if (viewUser && navigation) {
			navigation.navigate('Team');
		}
	}, [viewUser]);

	useEffect(() => {
		(async () => {
			try {
				const raw = await AsyncStorage.getItem(DRAFT_KEY);
				if (!raw) return;
				const d = JSON.parse(raw);
				if (d && typeof d === 'object') {
					if (typeof d.name === 'string') setName(d.name);
					if (typeof d.description === 'string') setDescription(d.description);
					if (typeof d.maxTeams !== 'undefined') setMaxTeams(String(d.maxTeams));
					if (typeof d.initialBudget !== 'undefined') setInitialBudget(String(d.initialBudget));
					if (typeof d.marketEndHour === 'string') setMarketEndHour(d.marketEndHour);
					if (typeof d.captainEnable === 'boolean') setCaptainEnable(d.captainEnable);
				}
			} catch {}
		})();
	}, []);

	useEffect(() => {
		const save = async () => {
			const draft = {
				name,
				description,
				maxTeams: Number(maxTeams || 0),
				initialBudget: Number(initialBudget || 0),
				marketEndHour: marketEndHour || '20:00',
				captainEnable: !!captainEnable,
			};
			try { await AsyncStorage.setItem(DRAFT_KEY, JSON.stringify(draft)); } catch {}
		};
		save();
	}, [name, description, maxTeams, initialBudget, marketEndHour, captainEnable]);

	useEffect(() => {
		getLeagues();
	}, [user]);

	useEffect(() => {
		const fetchUser = async () => {
			const current = await authService.getCurrentUser();
			setUser(current);
		};
		fetchUser();
	}, []);

	const canSubmit = useMemo(() => {
		return name.trim().length > 0; 
	}, [name]);

	const resetForm = () => {
		setName('');
		setDescription('');
		setMaxTeams('10');
		setInitialBudget('100000000');
		setMarketEndHour('20:00');
		setCaptainEnable(true);
		setError('');
	};

	const closeModal = async () => {
		setCreating(false);
		resetForm();
		try { await AsyncStorage.removeItem(DRAFT_KEY); } catch {}
	};

	const handleCreate = async () => {
		setError('');
		setFieldErrors({});
		const errs = {};
		if (name.trim().length < 3) errs.name = 'El nombre es obligatorio (mín. 3)';
		const mt = parseInt(maxTeams || '0', 10);
		if (!Number.isFinite(mt) || mt < 2) errs.maxTeams = 'Mínimo 2 equipos';
		const ib = parseInt(initialBudget || 'NaN', 10);
		if (!Number.isFinite(ib) || ib < 0) errs.initialBudget = 'Presupuesto inválido';
		if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(marketEndHour || '')) errs.marketEndHour = 'Formato HH:mm (00-23)';
		if (Object.keys(errs).length) { setFieldErrors(errs); return; }
		setLoading(true);
		try {
			const payload = {
				name: name.trim(),
				description: (description || '').trim() || null,
				maxTeams: parseInt(maxTeams || '0', 10),
				initialBudget: parseInt(initialBudget || '0', 10),
				marketEndHour: (marketEndHour || '20:00'), 
				captainEnable: !!captainEnable,
                chat: null,
                notificationLeague: null,
			};
			const res = await authService.authenticatedFetch('/api/v1/leagues', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(payload),
			});
			if (!res.ok) {
				const txt = await res.text();
				throw new Error(txt || `Error ${res.status}`);
			}
			const created = await res.json();
			Alert.alert('Liga creada', 'Se ha creado la liga correctamente.');
			setCreating(false);
			resetForm();
			try { await AsyncStorage.removeItem(DRAFT_KEY); } catch {}
			try {
				await getLeagues();
			} catch {}
			if (created && typeof created === 'object') {
				setSelectedLeague(created);
			}
		} catch (e) {
			let msg = (e?.message || 'Error').trim();
			try { const parsed = JSON.parse(msg); msg = parsed?.message || msg; } catch {}
			setError(msg.replace(/\s+/g, ' '));
		} finally {
			setLoading(false);
		}
	};

	const getLeagues = async () => {
		if (!user || !user.id) return;
		try {
			const res = await authService.authenticatedFetch('/api/v1/users/' + user.id + '/leagues', {
				method: 'GET',
				headers: { 'Content-Type': 'application/json' },
			});
			if (!res.ok) {
				const txt = await res.text();
				throw new Error(txt || `Error ${res.status}`);
			}
			const data = await res.json();
			setLeagues(Array.isArray(data) ? data : []);
		} catch (e) {
			console.error('Error fetching leagues:', e);
		}
	};

	const handleJoin = async () => {
		setJoinError('');
		if (!joinCode.trim()) {
			setJoinError('Introduce un código.');
			return;
		}
		setJoinLoading(true);
		try {
			const res = await authService.authenticatedFetch('/api/v1/leagues/join', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ code: joinCode.trim() })
			});
			if (!res.ok) {
				const txt = await res.text();
				throw new Error(txt || 'Error al unirse');
			}
			const joined = await res.json(); // { id, code, name }
			Alert.alert('Unión correcta', 'Te has unido a la liga.');
			setJoinVisible(false);
			setJoinCode('');
			await getLeagues();
			// Cargar jugadores asignados automáticamente y mostrar modal
			try {
				const me = await authService.getCurrentUser();
				if (me?.id && joined?.id) {
					const teamRes = await authService.authenticatedFetch(`/api/v1/teams/league/${joined.id}/${me.id}`);
					if (teamRes.ok) {
						const team = await teamRes.json();
						const list = Array.isArray(team?.playerTeams) ? team.playerTeams.map(pt => pt?.player?.fullName ?? pt?.player?.name).filter(Boolean) : [];
						setAssignedPlayers(list);
						setAssignedLeagueName(joined?.name || '');
						setAssignedVisible(true);
					}
				}
			} catch {}
		} catch (e) {
			setJoinError((e?.message || 'Error').replace(/\s+/g, ' '));
		} finally {
			setJoinLoading(false);
		}
	};

	const handleEditLeague = (league) => {
		setEditingLeague(league);
		setName(league.name || '');
		setDescription(league.description || '');
		setMaxTeams(String(league.maxTeams || '10'));
		setInitialBudget(String(league.initialBudget || '100000000'));
		setMarketEndHour(league.marketEndHour || '20:00');
		setCaptainEnable(league.captainEnable ?? true);
		setEditing(true);
	};

	const handleUpdateLeague = async () => {
		setError('');
		setFieldErrors({});
		const errs = {};
		if (name.trim().length < 3) errs.name = 'El nombre es obligatorio (mín. 3)';
		if (Object.keys(errs).length) { setFieldErrors(errs); return; }
		setLoading(true);
		try {
			const payload = {
				name: name.trim(),
				description: (description || '').trim() || null,
			};
			const res = await authService.authenticatedFetch(`/api/v1/leagues/${editingLeague.id}`, {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(payload),
			});
			if (!res.ok) {
				const txt = await res.text();
				throw new Error(txt || `Error ${res.status}`);
			}
			Alert.alert('Liga actualizada', 'Los cambios han sido guardados.');
			setEditing(false);
			setEditingLeague(null);
			resetForm();
			await getLeagues();
		} catch (e) {
			let msg = (e?.message || 'Error').trim();
			try { const parsed = JSON.parse(msg); msg = parsed?.message || msg; } catch {}
			setError(msg.replace(/\s+/g, ' '));
		} finally {
			setLoading(false);
		}
	};

	const closeEditModal = () => {
		setEditing(false);
		setEditingLeague(null);
		resetForm();
	};

	const handleDeleteLeague = async (league) => {
		Alert.alert(
			'Eliminar liga',
			`¿Estás seguro de que quieres eliminar la liga "${league.name}"? Esta acción no se puede deshacer.`,
			[
				{ text: 'Cancelar', style: 'cancel' },
				{
					text: 'Eliminar',
					style: 'destructive',
					onPress: async () => {
						try {
							const res = await authService.authenticatedFetch(`/api/v1/leagues/${league.id}`, {
								method: 'DELETE'
							});
							if (!res.ok) {
								const txt = await res.text();
								throw new Error(txt || 'Error al eliminar');
							}
							Alert.alert('Liga eliminada', 'La liga ha sido eliminada correctamente.');
							await getLeagues();
						} catch (e) {
							Alert.alert('Error', e?.message || 'No se pudo eliminar la liga');
						}
					}
				}
			]
		);
	};


	return (
		<View style={styles.screen}>
			<View style={styles.topButtons}>
				<TouchableOpacity style={styles.primaryBtn} onPress={() => setCreating(true)} activeOpacity={0.85}>
					<Text style={styles.primaryBtnText}>Crear liga</Text>
				</TouchableOpacity>
				<TouchableOpacity style={styles.secondaryBtn} onPress={() => setJoinVisible(true)} activeOpacity={0.85}>
					<Text style={styles.secondaryBtnText}>Unirse a liga</Text>
				</TouchableOpacity>
			</View>
			<FormLeague
				visible={creating}
				onClose={closeModal}
				name={name}
				setName={setName}
				description={description}
				setDescription={setDescription}
				maxTeams={maxTeams}
				setMaxTeams={setMaxTeams}
				initialBudget={initialBudget}
				setInitialBudget={setInitialBudget}
				marketEndHour={marketEndHour}
				setMarketEndHour={setMarketEndHour}
				captainEnable={captainEnable}
				setCaptainEnable={setCaptainEnable}
				canSubmit={canSubmit}
				loading={loading}
				error={error}
				fieldErrors={fieldErrors}
				onCreate={handleCreate}
				isEditing={false}
			/>
			<FormLeague
				visible={editing}
				onClose={closeEditModal}
				name={name}
				setName={setName}
				description={description}
				setDescription={setDescription}
				maxTeams={maxTeams}
				setMaxTeams={setMaxTeams}
				initialBudget={initialBudget}
				setInitialBudget={setInitialBudget}
				marketEndHour={marketEndHour}
				setMarketEndHour={setMarketEndHour}
				captainEnable={captainEnable}
				setCaptainEnable={setCaptainEnable}
				canSubmit={canSubmit}
				loading={loading}
				error={error}
				fieldErrors={fieldErrors}
				onCreate={handleUpdateLeague}
				isEditing={true}
			/>
			{!selectedLeague && (
				<ScrollView style={styles.list} contentContainerStyle={styles.listContent}>
					{leagues.map(l => (
						<LeagueCard
							key={l.id || l.code || l.name}
							league={l}
							user={user}
							onEdit={handleEditLeague}
							onDelete={handleDeleteLeague}
							onOpen={(league) => { setViewUser(null); setSelectedLeague(league); }}
						/>
					))}
					{leagues.length === 0 && user && (
						<View style={styles.onboardingWrap}>
							<Ionicons name="trophy" size={56} color={colors.primaryMuted} />
							<Text style={styles.onboardingTitle}>¡Bienvenido a DraftLeague!</Text>
							<Text style={styles.onboardingSubtitle}>
								Crea tu propia liga o únete a una existente con un código para empezar a competir.
							</Text>
							<View style={styles.onboardingBtns}>
								<TouchableOpacity style={styles.onboardingPrimaryBtn} onPress={() => setCreating(true)} activeOpacity={0.85}>
									<Ionicons name="add-circle-outline" size={18} color={colors.textInverse} />
									<Text style={styles.onboardingPrimaryBtnText}>Crear liga</Text>
								</TouchableOpacity>
								<TouchableOpacity style={styles.onboardingSecondaryBtn} onPress={() => setJoinVisible(true)} activeOpacity={0.85}>
									<Ionicons name="enter-outline" size={18} color={colors.primary} />
									<Text style={styles.onboardingSecondaryBtnText}>Unirte con código</Text>
								</TouchableOpacity>
							</View>
						</View>
					)}
				</ScrollView>
			)}
			{selectedLeague && (
				<RankingLeague league={selectedLeague} onBack={() => setSelectedLeague(null)} />
			)}
			<JoinLeagueModal
				visible={joinVisible}
				onClose={() => { setJoinVisible(false); setJoinCode(''); setJoinError(''); }}
				code={joinCode}
				setCode={setJoinCode}
				onJoin={handleJoin}
				loading={joinLoading}
				error={joinError}
			/>
			<Modal
				visible={assignedVisible}
				transparent
				animationType="fade"
				onRequestClose={() => setAssignedVisible(false)}
			>
				<View style={styles.modalBackdrop}>
					<View style={[styles.modalCard, { backgroundColor: '#fff' }] }>
						<Text style={[styles.title, { color: '#0f172a' } ]}>Equipo asignado{assignedLeagueName ? ` · ${assignedLeagueName}` : ''}</Text>
						<ScrollView style={{ maxHeight: 320 }} contentContainerStyle={{ paddingVertical: 8, gap: 6 }}>
							{assignedPlayers.length === 0 && (
								<Text style={{ color: '#475569' }}>No se encontraron jugadores asignados.</Text>
							)}
							{assignedPlayers.map((name, idx) => (
								<View key={`${name}-${idx}`} style={{ padding: 8, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 8 }}>
									<Text style={{ color: '#111827', fontWeight: '600' }}>{name}</Text>
								</View>
							))}
						</ScrollView>
						<View style={{ flexDirection: 'row', justifyContent: 'flex-end', marginTop: 10 }}>
							<TouchableOpacity style={[styles.actionBtn, styles.save]} onPress={() => setAssignedVisible(false)}>
								<Text style={{ color: '#fff', fontWeight: '700' }}>Entendido</Text>
							</TouchableOpacity>
						</View>
					</View>
				</View>
			</Modal>
		</View>
	);
}

import withAuth from '../../components/withAuth';
export default withAuth(Leagues);

const styles = StyleSheet.create({
	screen: { flex: 1, alignItems: 'stretch', justifyContent: 'flex-start', paddingTop: spacing.md },
	topButtons: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, paddingHorizontal: spacing.lg, marginBottom: spacing.sm },
	primaryBtn: { backgroundColor: colors.primary, paddingHorizontal: spacing.xl, paddingVertical: spacing.md, borderRadius: radius.md },
	primaryBtnText: { color: colors.textInverse, fontWeight: fontWeight.bold },
	secondaryBtn: { backgroundColor: colors.primaryDeep, paddingHorizontal: spacing.xl, paddingVertical: spacing.md, borderRadius: radius.md },
	secondaryBtnText: { color: colors.textInverse, fontWeight: fontWeight.bold },
	backBtn: { backgroundColor: colors.textSecondary, paddingHorizontal: spacing.lg, paddingVertical: spacing.md, borderRadius: radius.md },
	backBtnText: { color: colors.textInverse, fontWeight: fontWeight.bold },
	modalBackdrop: { flex: 1, backgroundColor: colors.overlay, alignItems: 'center', justifyContent: 'center', padding: spacing.lg },
	modalCard: { width: '94%', maxWidth: 520, maxHeight: '90%', borderRadius: radius.xl, paddingVertical: spacing.xl, paddingHorizontal: spacing.xl },
	scroll: {},
	scrollContent: { paddingBottom: spacing.md },
	title: { fontSize: fontSize.xl, color: colors.textInverse, fontWeight: fontWeight.extrabold, textAlign: 'center', marginBottom: spacing.md },
	row: { marginVertical: 6 },
	rowSwitch: { marginTop: spacing.sm, paddingHorizontal: 6, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
	label: { color: '#E2E8F0', marginBottom: 6, fontWeight: fontWeight.semibold },
	input: { backgroundColor: colors.bgCard, borderRadius: radius.md, paddingHorizontal: spacing.md, height: 48, color: colors.textPrimary, borderWidth: 1.5, borderColor: colors.border },
	smallBtn: { marginTop: 6, alignSelf: 'flex-start', backgroundColor: colors.primaryDeep, paddingHorizontal: spacing.sm, paddingVertical: 6, borderRadius: radius.sm },
	smallBtnText: { color: colors.textInverse },
	actions: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: spacing.md, gap: spacing.sm },
	actionBtn: { paddingHorizontal: 18, paddingVertical: spacing.sm, borderRadius: radius.md },
	cancel: { backgroundColor: colors.textSecondary },
	save: { backgroundColor: colors.primary },
	disabled: { backgroundColor: colors.textMuted },
	error: {
		color: '#FEE2E2',
		backgroundColor: 'rgba(239,68,68,0.20)',
		borderRadius: radius.md,
		paddingHorizontal: spacing.sm,
		paddingVertical: 6,
		marginTop: spacing.sm,
		fontWeight: fontWeight.semibold,
	},
	list: { marginTop: spacing.sm, alignSelf: 'stretch', paddingHorizontal: spacing.sm },
	listContent: { paddingBottom: 40, paddingLeft: spacing.sm, paddingRight: spacing.sm, gap: spacing.md },
	leagueCard: {
		flexDirection: 'row',
		backgroundColor: colors.bgCard,
		borderRadius: radius.xl,
		paddingVertical: spacing.md,
		paddingHorizontal: spacing.md,
		gap: spacing.md,
		borderWidth: 1,
		borderColor: colors.border,
		...shadow.sm,
	},
	cardAccent: {
		width: 8,
		borderRadius: 999,
		backgroundColor: colors.primary,
	},
	cardBody: { flex: 1 },
	cardHeader: {
		flexDirection: 'row',
		alignItems: 'flex-start',
		justifyContent: 'space-between',
		gap: spacing.md,
	},
	identityWrap: { flex: 1, gap: 4 },
	sectionEyebrow: {
		fontSize: 10,
		textTransform: 'uppercase',
		letterSpacing: 0.8,
		color: colors.textMuted,
		fontWeight: fontWeight.semibold,
	},
	leagueName: {
		fontSize: fontSize.lg,
		fontWeight: fontWeight.extrabold,
		color: colors.textPrimary,
	},
	leagueDesc: {
		color: colors.textSecondary,
		fontSize: fontSize.sm,
		lineHeight: 18,
	},
	leagueDescMuted: {
		color: colors.textMuted,
		fontSize: fontSize.sm,
	},
	headerActions: {
		alignItems: 'flex-end',
		gap: spacing.sm,
	},
	codePill: {
		backgroundColor: colors.successBg,
		borderRadius: radius.md,
		paddingHorizontal: spacing.sm,
		paddingVertical: 6,
		borderWidth: 1,
		borderColor: colors.primaryMuted,
		minWidth: 84,
	},
	codeLabel: {
		fontSize: 9,
		textTransform: 'uppercase',
		letterSpacing: 0.8,
		color: colors.primaryDark,
		fontWeight: fontWeight.semibold,
	},
	codeText: {
		marginTop: 2,
		color: colors.primaryDeep,
		fontWeight: fontWeight.bold,
		fontSize: fontSize.xs,
		letterSpacing: 0.8,
	},
	metricsWrap: {
		marginTop: spacing.md,
		flexDirection: 'row',
		gap: spacing.sm,
	},
	metricCard: {
		flex: 1,
		backgroundColor: colors.bgSubtle,
		borderRadius: radius.md,
		borderWidth: 1,
		borderColor: colors.border,
		paddingVertical: spacing.sm,
		paddingHorizontal: spacing.sm,
		alignItems: 'center',
	},
	metricLabel: {
		fontSize: 10,
		color: colors.textMuted,
		textTransform: 'uppercase',
		letterSpacing: 0.7,
		fontWeight: fontWeight.semibold,
	},
	metricValue: {
		marginTop: 3,
		color: colors.textPrimary,
		fontSize: fontSize.md,
		fontWeight: fontWeight.extrabold,
	},
	ctaBtn: {
		marginTop: spacing.md,
		backgroundColor: colors.primary,
		borderRadius: radius.md,
		paddingVertical: spacing.sm,
		alignItems: 'center',
		justifyContent: 'center',
		...shadow.sm,
	},
	ctaText: {
		color: colors.textInverse,
		fontSize: fontSize.sm,
		fontWeight: fontWeight.bold,
		letterSpacing: 0.3,
	},
	gearIcon: { width: 22, height: 22, tintColor: colors.textSecondary },
	gearBtn: {
		width: 34,
		height: 34,
		borderRadius: 17,
		backgroundColor: colors.bgSubtle,
		borderWidth: 1,
		borderColor: colors.border,
		justifyContent: 'center',
		alignItems: 'center',
	},
	empty: { textAlign: 'center', color: colors.textMuted, marginTop: 30, fontSize: fontSize.sm },
	onboardingWrap: {
		alignItems: 'center',
		paddingVertical: spacing['4xl'],
		paddingHorizontal: spacing.xl,
		gap: spacing.md,
	},
	onboardingTitle: {
		fontSize: fontSize.xl,
		fontWeight: fontWeight.extrabold,
		color: colors.textPrimary,
		textAlign: 'center',
	},
	onboardingSubtitle: {
		fontSize: fontSize.sm,
		color: colors.textSecondary,
		textAlign: 'center',
		lineHeight: 20,
	},
	onboardingBtns: {
		flexDirection: 'row',
		gap: spacing.md,
		marginTop: spacing.sm,
		flexWrap: 'wrap',
		justifyContent: 'center',
	},
	onboardingPrimaryBtn: {
		flexDirection: 'row',
		alignItems: 'center',
		gap: spacing.sm,
		backgroundColor: colors.primary,
		paddingHorizontal: spacing.xl,
		paddingVertical: spacing.md,
		borderRadius: radius.md,
	},
	onboardingPrimaryBtnText: {
		color: colors.textInverse,
		fontWeight: fontWeight.bold,
		fontSize: fontSize.sm,
	},
	onboardingSecondaryBtn: {
		flexDirection: 'row',
		alignItems: 'center',
		gap: spacing.sm,
		backgroundColor: colors.successBg,
		paddingHorizontal: spacing.xl,
		paddingVertical: spacing.md,
		borderRadius: radius.md,
		borderWidth: 1.5,
		borderColor: colors.primaryMuted,
	},
	onboardingSecondaryBtnText: {
		color: colors.primary,
		fontWeight: fontWeight.bold,
		fontSize: fontSize.sm,
	},
});

