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
	Image,
} from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import authService, { refresh } from '../../services/authService';
import AsyncStorage from '@react-native-async-storage/async-storage';
import FormLeague from './formLeague';
import RankingLeague from './rankingLeague';
import { useLeague } from '../../context/LeagueContext';
import JoinLeagueModal from './joinLeagueModal';

const DRAFT_KEY = 'leagues.createDraft';

export default function Leagues() {
	const [creating, setCreating] = useState(false);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState('');
	const [user, setUser] = useState(null);

	const [name, setName] = useState('');
	const [description, setDescription] = useState('');
	const [maxTeams, setMaxTeams] = useState('10');
	const [initialBudget, setInitialBudget] = useState('100');
	const [marketEndHour, setMarketEndHour] = useState('20:00');
	const [captainEnable, setCaptainEnable] = useState(true);
	const [wildCardsEnable, setWildCardsEnable] = useState(false);
	const [leagues, setLeagues] = useState([]);
	const [fieldErrors, setFieldErrors] = useState({});

	const [joinVisible, setJoinVisible] = useState(false);
	const [joinCode, setJoinCode] = useState('');
	const [joinError, setJoinError] = useState('');
	const [joinLoading, setJoinLoading] = useState(false);

	const { selectedLeague, setSelectedLeague } = useLeague();

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
					if (typeof d.wildCardsEnable === 'boolean') setWildCardsEnable(d.wildCardsEnable);
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
				wildCardsEnable: !!wildCardsEnable,
			};
			try { await AsyncStorage.setItem(DRAFT_KEY, JSON.stringify(draft)); } catch {}
		};
		save();
	}, [name, description, maxTeams, initialBudget, marketEndHour, captainEnable, wildCardsEnable]);

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
		return name.trim().length > 0; // el resto se valida al guardar para mostrar mensajes por campo
	}, [name]);

	const resetForm = () => {
		setName('');
		setDescription('');
		setMaxTeams('10');
		setInitialBudget('100');
		setMarketEndHour('20:00');
		setCaptainEnable(true);
		setWildCardsEnable(false);
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
				wildCardsEnable: !!wildCardsEnable,
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
			
			Alert.alert('Liga creada', 'Se ha creado la liga correctamente.');
			setCreating(false);
			resetForm();
			try { await AsyncStorage.removeItem(DRAFT_KEY); } catch {}
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
			Alert.alert('Unión correcta', 'Te has unido a la liga.');
			setJoinVisible(false);
			setJoinCode('');
			await getLeagues();
		} catch (e) {
			setJoinError((e?.message || 'Error').replace(/\s+/g, ' '));
		} finally {
			setJoinLoading(false);
		}
	};

	const LeagueCard = ({ league }) => {
		const pts = league.points ?? league.pts ?? '-';
		const pos = league.position ?? league.ranking ?? '-';
		const participants = league.participants ?? league.currentTeams ?? league.maxTeams ?? '-';
		const showCode = () => {
			Alert.alert('Código de la liga', league.code ? String(league.code) : 'Sin código');
		};
		const openRanking = () => setSelectedLeague(league);
		return (
			<View style={styles.leagueCard}>
				<View style={styles.leagueFlagWrapper}>
					<Text style={styles.star}>★</Text>
					<Text style={styles.flagEmoji}>🇪🇸</Text>
				</View>
				<TouchableOpacity style={styles.leagueInfo} onPress={openRanking} activeOpacity={0.8}>
					<Text style={styles.leagueName}>{league.name}</Text>
					<Text style={styles.leagueMeta}>Pts: {pts}   Posición: {pos}   Participantes: {participants}</Text>
				</TouchableOpacity>
				<TouchableOpacity onPress={showCode} style={styles.gearBtn} hitSlop={{top:8,bottom:8,left:8,right:8}}>
					<Image source={require('../../assets/header/gear.png')} style={styles.gearIcon} />
				</TouchableOpacity>
			</View>
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
				wildCardsEnable={wildCardsEnable}
				setWildCardsEnable={setWildCardsEnable}
				canSubmit={canSubmit}
				loading={loading}
				error={error}
				fieldErrors={fieldErrors}
				onCreate={handleCreate}
			/>
			{!selectedLeague && (
				<ScrollView style={styles.list} contentContainerStyle={styles.listContent}>
					{leagues.map(l => (
						<LeagueCard key={l.id || l.code || l.name} league={l} />
					))}
					{leagues.length === 0 && user && (
						<Text style={styles.empty}>No tienes ligas todavía.</Text>
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
		</View>
	);
}

const styles = StyleSheet.create({
	    screen: { flex: 1, alignItems: 'stretch', justifyContent: 'flex-start', paddingTop: 12 },
		topButtons: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 16, marginBottom: 8 },
	primaryBtn: { backgroundColor: '#1d4ed8', paddingHorizontal: 20, paddingVertical: 12, borderRadius: 10 },
	primaryBtnText: { color: '#fff', fontWeight: '700' },
	secondaryBtn: { backgroundColor: '#111827', paddingHorizontal: 20, paddingVertical: 12, borderRadius: 10 },
	secondaryBtnText: { color:'#fff', fontWeight:'700' },
	    backBtn: { backgroundColor: '#6b7280', paddingHorizontal: 16, paddingVertical: 12, borderRadius: 10 },
	    backBtnText: { color: '#fff', fontWeight: '700' },

    modalBackdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.35)', alignItems: 'center', justifyContent: 'center', padding: 16 },
	modalCard: { width: '94%', maxWidth: 520, maxHeight: '90%', borderRadius: 18, paddingVertical: 20, paddingHorizontal: 20 },
	scroll: { },
	scrollContent: { paddingBottom: 12 },
    title: { fontSize: 22, color: '#fff', fontWeight: '800', textAlign: 'center', marginBottom: 10 },
    row: { marginVertical: 6 },
    rowSwitch: { marginTop: 8, paddingHorizontal: 6, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
    label: { color: '#e2e8f0', marginBottom: 6, fontWeight: '600' },
    input: { backgroundColor: '#ffffff', borderRadius: 12, paddingHorizontal: 12, height: 42, color: '#0f172a' },
    smallBtn: { marginTop: 6, alignSelf: 'flex-start', backgroundColor: '#111827', paddingHorizontal: 10, paddingVertical: 6, borderRadius: 8 },
    smallBtnText: { color: '#fff' },
    actions: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: 12, gap: 10 },
    actionBtn: { paddingHorizontal: 18, paddingVertical: 10, borderRadius: 10 },
    cancel: { backgroundColor: '#6b7280' },
    save: { backgroundColor: '#16a34a' },
    disabled: { backgroundColor: '#9ca3af' },
    error: {
        color: '#fecaca',
        backgroundColor: 'rgba(239,68,68,0.25)',
        borderRadius: 10,
        paddingHorizontal: 10,
        paddingVertical: 6,
        marginTop: 8,
        fontWeight: '600',
    },
	list: { marginTop: 8, alignSelf: 'stretch', paddingHorizontal: 8 },
	listContent: { paddingBottom: 40, paddingLeft: 8, paddingRight: 8, gap: 12 },
	leagueCard: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#e1e1e1', borderRadius: 22, paddingVertical: 10, paddingHorizontal: 14, gap: 14, shadowColor: '#000', shadowOpacity: 0.08, shadowRadius: 4, shadowOffset: { width: 0, height: 2 } },
	leagueFlagWrapper: { alignItems: 'center', justifyContent: 'center', width: 50 },
	star: { position: 'absolute', top: -6, left: -2, fontSize: 16, color: '#fbbf24', fontWeight: '700' },
	flagEmoji: { fontSize: 42 },
	leagueInfo: { flex: 1 },
	leagueName: { fontSize: 18, fontWeight: '700', color: '#111827' },
	leagueMeta: { marginTop: 4, fontSize: 12, color: '#374151', fontWeight: '500' },
	gearIcon: { width: 24, height: 24, tintColor: '#111827' },
	gearBtn: { paddingLeft: 6, justifyContent: 'center', alignItems: 'center' },
	empty: { textAlign: 'center', color: '#6b7280', marginTop: 30, fontSize: 14 },

});

