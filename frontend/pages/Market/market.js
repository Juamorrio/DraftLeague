import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, FlatList, Alert, RefreshControl } from 'react-native';
import { useLeague } from '../../context/LeagueContext';
import authService, { authenticatedFetch } from '../../services/authService';
import withAuth from '../../components/withAuth';
import MarketPlayerCard, { MarketPlayerCardSkeleton } from '../../components/Market/MarketPlayerCard';
import BidModal from '../../components/Market/BidModal';
import { colors, fontSize, fontWeight, radius, spacing } from '../../utils/theme';

function Market({ navigation }) {
	const { selectedLeague, setSelectedPlayer } = useLeague();
	const [marketPlayers, setMarketPlayers] = useState([]);
	const [loading, setLoading] = useState(false);
	const [refreshing, setRefreshing] = useState(false);
	const [userTeam, setUserTeam] = useState(null);
	const [bidModal, setBidModal] = useState({ visible: false, item: null });
	const [bidAmount, setBidAmount] = useState('');
	const [bidSubmitting, setBidSubmitting] = useState(false);

	const formatNumber = (num) => {
		if (!num && num !== 0) return '0';
		return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
	};

	const loadMarketPlayers = async () => {
		if (!selectedLeague?.id) return;
		setLoading(true);
		try {
			const res = await authenticatedFetch(`/api/v1/market?leagueId=${selectedLeague.id}`);
			if (!res.ok) throw new Error(await res.text());
			const json = await res.json();
			setMarketPlayers(Array.isArray(json) ? json : []);
		} catch (e) {
			Alert.alert('Error', 'No se pudieron cargar jugadores del mercado: ' + e.message);
		} finally {
			setLoading(false);
		}
	};

	const loadUserTeam = async () => {
		if (!selectedLeague?.id) return;
		try {
			const user = await authService.getCurrentUser();
			if (!user?.id) return;
			const res = await authenticatedFetch(`/api/v1/teams/my-team/${selectedLeague.id}/${user.id}`);
			if (res.ok) setUserTeam(await res.json());
		} catch {}
	};

	useEffect(() => {
		loadMarketPlayers();
		loadUserTeam();
	}, [selectedLeague?.id]);

	const onRefresh = async () => {
		setRefreshing(true);
		try {
			await Promise.all([loadMarketPlayers(), loadUserTeam()]);
		} finally {
			setRefreshing(false);
		}
	};

	const openBidModal = (item) => {
		setBidModal({ visible: true, item });
		setBidAmount('');
	};

	const closeBidModal = () => {
		setBidModal({ visible: false, item: null });
		setBidAmount('');
	};

	const confirmBid = async () => {
		const amount = parseInt(bidAmount, 10);
		if (!amount || amount <= 0) {
			Alert.alert('Error', 'Ingresa una cantidad válida');
			return;
		}
		setBidSubmitting(true);
		try {
			const res = await authenticatedFetch(
				`/api/v1/market/bid?marketPlayerId=${bidModal.item.id}&bidAmount=${amount}`,
				{ method: 'POST' }
			);
			if (res.ok) {
				closeBidModal();
				Alert.alert('Puja realizada', `Has pujado €${formatNumber(amount)} por ${bidModal.item.player.fullName ?? bidModal.item.player.name}`);
				await loadUserTeam();
				await loadMarketPlayers();
			} else {
				let msg = 'No se pudo realizar la puja';
				try { const d = await res.json(); msg = d?.error || msg; } catch {}
				Alert.alert('Error', msg);
			}
		} catch {
			Alert.alert('Error', 'Error al pujar');
		} finally {
			setBidSubmitting(false);
		}
	};

	const cancelBid = async (marketPlayerId) => {
		Alert.alert('Cancelar puja', '¿Estás seguro de que quieres cancelar tu puja?', [
			{ text: 'No', style: 'cancel' },
			{
				text: 'Sí, cancelar', style: 'destructive',
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
							let msg = 'No se pudo cancelar la puja';
							try { const d = await res.json(); msg = d?.error || msg; } catch {}
							Alert.alert('Error', msg);
						}
					} catch {
						Alert.alert('Error', 'Error al cancelar puja');
					}
				}
			}
		]);
	};

	const goToPlayerStats = (player) => {
		if (!player?.id) return;
		setSelectedPlayer(player);
		navigation.navigate('PlayerStats');
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
			{/* Top bar */}
			<View style={styles.topBar}>
				<Text style={styles.topBarTitle}>Mercado · {selectedLeague.name}</Text>
				{userTeam && (
					<View style={styles.budgetPill}>
						<Text style={styles.budgetPillLabel}>Presupuesto</Text>
						<Text style={styles.budgetPillAmount}>€{formatNumber(userTeam.budget)}</Text>
					</View>
				)}
			</View>

			{loading ? (
				<FlatList
					data={Array.from({ length: 7 })}
					keyExtractor={(_, i) => `skeleton-${i}`}
					contentContainerStyle={styles.listContent}
					ItemSeparatorComponent={() => <View style={{ height: 10 }} />}
					renderItem={() => <MarketPlayerCardSkeleton />}
					scrollEnabled={false}
				/>
			) : (
				<FlatList
					data={marketPlayers}
					keyExtractor={(item) => String(item.id)}
					contentContainerStyle={styles.listContent}
					ItemSeparatorComponent={() => <View style={{ height: 10 }} />}
					refreshControl={
						<RefreshControl
							refreshing={refreshing}
							onRefresh={onRefresh}
							colors={[colors.primary]}
							tintColor={colors.primary}
						/>
					}
					ListEmptyComponent={
						<Text style={styles.emptyText}>No hay jugadores en el mercado</Text>
					}
					renderItem={({ item }) => (
						<MarketPlayerCard
							item={item}
							onViewStats={goToPlayerStats}
							onBid={openBidModal}
							onCancelBid={cancelBid}
							formatNumber={formatNumber}
						/>
					)}
				/>
			)}

			<BidModal
				visible={bidModal.visible}
				item={bidModal.item}
				budget={userTeam?.budget ?? null}
				bidAmount={bidAmount}
				onChangeBid={setBidAmount}
				onClose={closeBidModal}
				onConfirm={confirmBid}
				submitting={bidSubmitting}
				formatNumber={formatNumber}
			/>
		</View>
	);
}

export default withAuth(Market);

const styles = StyleSheet.create({
	container: { flex: 1, backgroundColor: colors.bgApp },
	topBar: {
		flexDirection: 'row',
		alignItems: 'center',
		justifyContent: 'space-between',
		backgroundColor: colors.primaryDeep,
		paddingHorizontal: spacing.lg,
		paddingVertical: spacing.md,
		gap: spacing.sm,
	},
	topBarTitle: {
		flex: 1,
		color: colors.textInverse,
		fontSize: fontSize.xl,
		fontWeight: fontWeight.extrabold,
		letterSpacing: 0.5,
	},
	budgetPill: {
		backgroundColor: 'rgba(255,255,255,0.15)',
		borderRadius: radius.pill,
		paddingHorizontal: spacing.md,
		paddingVertical: 6,
		borderWidth: 1,
		borderColor: 'rgba(255,255,255,0.25)',
		alignItems: 'flex-end',
	},
	budgetPillLabel: { fontSize: fontSize.xs, color: 'rgba(255,255,255,0.7)', letterSpacing: 0.3 },
	budgetPillAmount: { fontSize: fontSize.md, color: colors.textInverse, fontWeight: fontWeight.extrabold },
	loadingWrap: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.md },
	loadingText: { fontSize: fontSize.sm, color: colors.textMuted, fontWeight: fontWeight.semibold },
	emptyText: { fontSize: fontSize.md, color: colors.textMuted, textAlign: 'center', marginTop: 48 },
	listContent: { padding: spacing.md, paddingBottom: 32 },
});
