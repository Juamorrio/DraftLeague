import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Alert } from 'react-native';
import { acceptOffer, rejectOffer, cancelOffer } from '../../services/tradeOfferService';
import { colors, fontSize, fontWeight, radius, spacing } from '../../utils/theme';

/**
 * Displays incoming and outgoing pending trade offers with accept/reject/cancel actions.
 *
 * Props:
 *   incomingOffers – TradeOffer[]
 *   outgoingOffers – TradeOffer[]
 *   onRefresh      – () => Promise<void>  (called after any action to reload offers + team)
 */
export default function TradesSection({ incomingOffers, outgoingOffers, onRefresh }) {
	const pending = (list) => list.filter(o => o.status === 'PENDING');

	if (pending(incomingOffers).length === 0 && pending(outgoingOffers).length === 0) {
		return null;
	}

	const handleAccept = async (offerId) => {
		try {
			await acceptOffer(offerId);
			await onRefresh();
			Alert.alert('Aceptada', 'Traspaso completado.');
		} catch (e) {
			Alert.alert('Error', e?.message || 'No se pudo aceptar');
		}
	};

	const handleReject = async (offerId) => {
		try {
			await rejectOffer(offerId);
			await onRefresh();
			Alert.alert('Rechazada', 'Oferta rechazada.');
		} catch (e) {
			Alert.alert('Error', e?.message || 'No se pudo rechazar');
		}
	};

	const handleCancel = async (offerId) => {
		try {
			await cancelOffer(offerId);
			await onRefresh();
			Alert.alert('Cancelada', 'Oferta cancelada.');
		} catch (e) {
			Alert.alert('Error', e?.message || 'No se pudo cancelar');
		}
	};

	return (
		<View style={styles.section}>
			<Text style={styles.sectionTitle}>🤝 Traspasos pendientes</Text>

			{pending(incomingOffers).map(offer => (
				<View key={offer.id} style={styles.offerCard}>
					<View style={styles.offerInfo}>
						<Text style={styles.offerPlayer}>{offer.player?.fullName}</Text>
						<Text style={styles.offerMeta}>
							{offer.fromTeam?.user?.username} ofrece €{offer.offerPrice?.toLocaleString('es-ES')}
						</Text>
					</View>
					<View style={styles.offerActions}>
						<TouchableOpacity
							style={[styles.actionBtn, { backgroundColor: colors.primary }]}
							onPress={() => handleAccept(offer.id)}
						>
							<Text style={styles.actionBtnText}>Aceptar</Text>
						</TouchableOpacity>
						<TouchableOpacity
							style={[styles.actionBtn, { backgroundColor: colors.danger }]}
							onPress={() => handleReject(offer.id)}
						>
							<Text style={styles.actionBtnText}>Rechazar</Text>
						</TouchableOpacity>
					</View>
				</View>
			))}

			{pending(outgoingOffers).map(offer => (
				<View key={offer.id} style={[styles.offerCard, styles.offerCardOutgoing]}>
					<View style={styles.offerInfo}>
						<Text style={styles.offerPlayer}>{offer.player?.fullName}</Text>
						<Text style={styles.offerMeta}>
							enviada a {offer.toTeam?.user?.username} · €{offer.offerPrice?.toLocaleString('es-ES')}
						</Text>
					</View>
					<TouchableOpacity
						style={[styles.actionBtn, { backgroundColor: colors.textMuted }]}
						onPress={() => handleCancel(offer.id)}
					>
						<Text style={styles.actionBtnText}>Cancelar</Text>
					</TouchableOpacity>
				</View>
			))}
		</View>
	);
}

const styles = StyleSheet.create({
	section: {
		marginHorizontal: spacing.md,
		marginTop: spacing.md,
		marginBottom: spacing.sm,
		backgroundColor: colors.bgCard,
		borderRadius: radius.lg,
		padding: spacing.md,
		borderWidth: 1,
		borderColor: colors.border,
	},
	sectionTitle: { fontSize: fontSize.md, fontWeight: fontWeight.bold, color: colors.textPrimary, marginBottom: spacing.sm },
	offerCard: {
		flexDirection: 'row',
		alignItems: 'center',
		justifyContent: 'space-between',
		backgroundColor: colors.bgSubtle,
		borderRadius: radius.md,
		padding: spacing.md,
		marginBottom: spacing.sm,
		borderLeftWidth: 4,
		borderLeftColor: colors.purple,
	},
	offerCardOutgoing: { borderLeftColor: colors.textMuted },
	offerInfo: { flex: 1, marginRight: spacing.sm },
	offerPlayer: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textPrimary },
	offerMeta: { fontSize: fontSize.xs, color: colors.textSecondary, marginTop: 2 },
	offerActions: { flexDirection: 'row', gap: 6 },
	actionBtn: { paddingVertical: 6, paddingHorizontal: 12, borderRadius: radius.pill },
	actionBtnText: { color: colors.textInverse, fontSize: fontSize.xs, fontWeight: fontWeight.bold },
});
