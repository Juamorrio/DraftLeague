import React from 'react';
import {
	View, Text, StyleSheet, TouchableOpacity, TextInput,
	Modal, KeyboardAvoidingView, Platform,
} from 'react-native';
import Player from '../player';
import {
	colors, fontSize, fontWeight, radius, spacing, shadow, marketPositionColors,
} from '../../utils/theme';

function formatCountdown(endTime) {
	const diff = new Date(endTime) - new Date();
	if (diff <= 0) return 'Finalizada';
	const h = Math.floor(diff / 3600000);
	const m = Math.floor((diff % 3600000) / 60000);
	if (h > 24) return `${Math.floor(h / 24)}d ${h % 24}h`;
	if (h > 0) return `${h}h ${m}m`;
	return `${m}m`;
}

/**
 * Bottom-sheet modal for placing a bid on a market player.
 *
 * Props:
 *   visible      – boolean
 *   item         – AuctionItem | null
 *   budget       – number | null  (current user budget)
 *   bidAmount    – string         (controlled input value)
 *   onChangeBid  – (text: string) => void
 *   onClose      – () => void
 *   onConfirm    – () => void
 *   submitting   – boolean
 *   formatNumber – (num) => string
 */
export default function BidModal({ visible, item, budget, bidAmount, onChangeBid, onClose, onConfirm, submitting, formatNumber }) {
	const parsed = parseInt(bidAmount, 10) || 0;
	const budgetAfter = budget != null ? budget - parsed : null;
	const budgetNegative = budgetAfter !== null && budgetAfter < 0;
	const posColor = item ? (marketPositionColors[item.player.position] ?? marketPositionColors.COACH) : null;
	const countdown = item ? formatCountdown(item.auctionEndTime) : '';
	const expired = countdown === 'Finalizada';

	return (
		<Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
			<KeyboardAvoidingView
				style={styles.backdrop}
				behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
			>
				<View style={styles.card}>
					{/* Header */}
					<View style={styles.header}>
						<Text style={styles.title}>Hacer puja</Text>
						<TouchableOpacity onPress={onClose} style={styles.closeBtn}>
							<Text style={styles.closeTxt}>✕</Text>
						</TouchableOpacity>
					</View>

					{item && (
						<>
							{/* Player summary */}
							<View style={styles.playerRow}>
								<Player
									name={item.player.fullName ?? item.player.name}
									avatar={item.player.avatarUrl ? { uri: item.player.avatarUrl } : null}
									teamId={item.player.teamId}
									size={52}
								/>
								<View style={styles.playerInfo}>
									<Text style={styles.playerName} numberOfLines={1}>
										{item.player.fullName ?? item.player.name}
									</Text>
									{posColor && (
										<View style={[styles.posBadge, { backgroundColor: posColor.bg, borderColor: posColor.border, alignSelf: 'flex-start' }]}>
											<Text style={[styles.posBadgeText, { color: posColor.text }]}>
												{item.player.position}
											</Text>
										</View>
									)}
									<Text style={styles.playerMeta}>
										Valor de mercado: €{item.player.marketValue.toLocaleString('es-ES')}
									</Text>
									<Text style={[styles.playerMeta, expired && { color: colors.danger }]}>
										{expired ? 'Subasta finalizada' : `⏱ Cierra en ${countdown}`}
									</Text>
								</View>
							</View>

							<View style={styles.divider} />

							{/* Budget info */}
							{budget != null && (
								<View style={styles.budgetRow}>
									<Text style={styles.budgetLabel}>Tu presupuesto</Text>
									<Text style={styles.budgetValue}>€{formatNumber(budget)}</Text>
								</View>
							)}

							{/* Amount input */}
							<Text style={styles.inputLabel}>Importe de tu puja</Text>
							<TextInput
								style={styles.input}
								placeholder="Introduce el importe (€)"
								placeholderTextColor={colors.textMuted}
								keyboardType="numeric"
								value={bidAmount}
								onChangeText={onChangeBid}
								autoFocus
							/>

							{/* Remaining budget preview */}
							{budget != null && parsed > 0 && (
								<View style={[styles.previewRow, budgetNegative && styles.previewRowDanger]}>
									<Text style={[styles.previewLabel, budgetNegative && styles.previewLabelDanger]}>
										Presupuesto restante
									</Text>
									<Text style={[styles.previewAmount, budgetNegative && styles.previewAmountDanger]}>
										€{formatNumber(budgetAfter)}
									</Text>
								</View>
							)}

							{/* Actions */}
							<View style={styles.actions}>
								<TouchableOpacity style={styles.cancelActionBtn} onPress={onClose}>
									<Text style={styles.cancelActionTxt}>Cancelar</Text>
								</TouchableOpacity>
								<TouchableOpacity
									style={[
										styles.confirmBtn,
										(submitting || !bidAmount || budgetNegative) && styles.confirmBtnDisabled,
									]}
									onPress={onConfirm}
									disabled={submitting || !bidAmount || budgetNegative}
								>
									<Text style={styles.confirmTxt}>
										{submitting ? 'Enviando...' : 'Confirmar puja'}
									</Text>
								</TouchableOpacity>
							</View>
						</>
					)}
				</View>
			</KeyboardAvoidingView>
		</Modal>
	);
}

const styles = StyleSheet.create({
	backdrop: { flex: 1, backgroundColor: colors.overlay, justifyContent: 'flex-end' },
	card: {
		backgroundColor: colors.bgCard,
		borderTopLeftRadius: 28,
		borderTopRightRadius: 28,
		paddingHorizontal: spacing.lg,
		paddingTop: spacing.lg,
		paddingBottom: 36,
		...shadow.lg,
	},
	header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: spacing.lg },
	title: { fontSize: fontSize.xl, fontWeight: fontWeight.extrabold, color: colors.textPrimary },
	closeBtn: {
		width: 32, height: 32, borderRadius: 16,
		backgroundColor: colors.bgSubtle,
		alignItems: 'center', justifyContent: 'center',
		borderWidth: 1, borderColor: colors.border,
	},
	closeTxt: { fontSize: fontSize.sm, color: colors.textSecondary, fontWeight: fontWeight.bold },
	playerRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, marginBottom: spacing.md },
	playerInfo: { flex: 1, gap: 4 },
	playerName: { fontSize: fontSize.lg, fontWeight: fontWeight.extrabold, color: colors.textPrimary },
	posBadge: { paddingHorizontal: 8, paddingVertical: 2, borderRadius: radius.pill, borderWidth: 1 },
	posBadgeText: { fontSize: 10, fontWeight: fontWeight.bold, letterSpacing: 0.5 },
	playerMeta: { fontSize: fontSize.xs, color: colors.textSecondary },
	divider: { height: 1, backgroundColor: colors.border, marginVertical: spacing.md },
	budgetRow: {
		flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
		backgroundColor: colors.successBg, borderRadius: radius.md,
		paddingHorizontal: spacing.md, paddingVertical: spacing.sm,
		marginBottom: spacing.md, borderWidth: 1, borderColor: colors.primaryMuted,
	},
	budgetLabel: { fontSize: fontSize.sm, color: colors.primaryDark, fontWeight: fontWeight.semibold },
	budgetValue: { fontSize: fontSize.md, fontWeight: fontWeight.extrabold, color: colors.primaryDeep },
	inputLabel: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textPrimary, marginBottom: 6 },
	input: {
		borderWidth: 1.5, borderColor: colors.border, borderRadius: radius.md,
		paddingHorizontal: spacing.md, paddingVertical: 12,
		fontSize: fontSize.lg, color: colors.textPrimary,
		backgroundColor: colors.bgSubtle, marginBottom: spacing.sm,
	},
	previewRow: {
		flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
		backgroundColor: colors.successBg, borderRadius: radius.md,
		paddingHorizontal: spacing.md, paddingVertical: spacing.sm,
		marginBottom: spacing.lg, borderWidth: 1, borderColor: colors.primaryMuted,
	},
	previewRowDanger: { backgroundColor: colors.dangerBg, borderColor: marketPositionColors.DEL.border },
	previewLabel: { fontSize: fontSize.sm, color: colors.primaryDark, fontWeight: fontWeight.semibold },
	previewLabelDanger: { color: colors.dangerDark },
	previewAmount: { fontSize: fontSize.md, fontWeight: fontWeight.extrabold, color: colors.primaryDeep },
	previewAmountDanger: { color: colors.dangerDark },
	actions: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.sm },
	cancelActionBtn: {
		flex: 1, paddingVertical: 13, borderRadius: radius.pill,
		borderWidth: 1.5, borderColor: colors.border,
		alignItems: 'center', backgroundColor: colors.bgSubtle,
	},
	cancelActionTxt: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textSecondary },
	confirmBtn: {
		flex: 2, paddingVertical: 13, borderRadius: radius.pill,
		backgroundColor: colors.primary, alignItems: 'center',
		...shadow.sm,
	},
	confirmBtnDisabled: { backgroundColor: colors.textMuted },
	confirmTxt: { fontSize: fontSize.sm, fontWeight: fontWeight.extrabold, color: colors.textInverse },
});
