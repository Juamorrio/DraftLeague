import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Modal, ScrollView } from 'react-native';
import { colors, fontSize, fontWeight, radius, spacing, shadow } from '../../utils/theme';

/**
 * Bottom-sheet modal for selecting, activating, or cancelling a chip.
 *
 * Props:
 *   visible      – boolean
 *   chips        – array of { id, name, desc, icon }
 *   usedChips    – string[]  (ids already spent)
 *   activeChip   – string | null  (currently active chip id)
 *   teamsLocked  – boolean
 *   onActivate   – (chipId: string) => void
 *   onCancel     – () => void  (cancel active chip)
 *   onClose      – () => void
 */
export default function ChipSelectorModal({
	visible,
	chips,
	usedChips,
	activeChip,
	teamsLocked,
	onActivate,
	onCancel,
	onClose,
}) {
	return (
		<Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
			<View style={styles.backdrop}>
				<View style={[styles.card, { maxHeight: '80%', width: '92%' }]}>
					<Text style={styles.title}>🎰 Tus Chips ({usedChips.length}/10 usados)</Text>
					<Text style={styles.subtitle}>
						{teamsLocked
							? activeChip
								? '🔒 Jornada bloqueada — chip activo para esta jornada.'
								: '🔒 Jornada bloqueada — ya no puedes activar chips.'
							: 'Solo puedes activar un chip por jornada. Puedes cancelarlo antes de que la jornada se bloquee.'}
					</Text>
					<ScrollView showsVerticalScrollIndicator={false}>
						{chips.map(chip => {
							const isUsed = usedChips.includes(chip.id);
							const isActive = activeChip === chip.id;
							return (
								<TouchableOpacity
									key={chip.id}
									disabled={isUsed || isActive || teamsLocked}
									onPress={() => onActivate(chip.id)}
									style={[
										styles.chipCard,
										isActive && styles.chipCardActive,
										isUsed && styles.chipCardUsed,
									]}
								>
									<Text style={styles.chipIcon}>{chip.icon}</Text>
									<View style={{ flex: 1 }}>
										<Text style={[styles.chipName, isUsed && styles.chipTextUsed]}>
											{chip.name}{isActive ? '  ✓ Activo' : ''}{isUsed ? '  ✗ Usado' : ''}
										</Text>
										<Text style={[styles.chipDesc, isUsed && styles.chipTextUsed]}>{chip.desc}</Text>
									</View>
									{isActive && !teamsLocked && (
										<TouchableOpacity
											onPress={onCancel}
											style={styles.cancelChipBtn}
										>
											<Text style={styles.cancelChipBtnText}>Cancelar</Text>
										</TouchableOpacity>
									)}
								</TouchableOpacity>
							);
						})}
					</ScrollView>
					<TouchableOpacity style={styles.closeBtn} onPress={onClose}>
						<Text style={styles.closeText}>Cerrar</Text>
					</TouchableOpacity>
				</View>
			</View>
		</Modal>
	);
}

const styles = StyleSheet.create({
	backdrop: { flex: 1, backgroundColor: colors.overlay, alignItems: 'center', justifyContent: 'center' },
	card: {
		backgroundColor: colors.bgCard,
		borderRadius: radius.xl,
		padding: spacing.lg,
		...shadow.lg,
	},
	title: { fontSize: fontSize.md, fontWeight: fontWeight.bold, marginBottom: spacing.sm, color: colors.textPrimary },
	subtitle: { fontSize: fontSize.xs, color: colors.textSecondary, marginBottom: spacing.md },
	chipCard: {
		flexDirection: 'row', alignItems: 'center', gap: spacing.md,
		paddingVertical: spacing.md, paddingHorizontal: spacing.md,
		borderRadius: radius.sm, borderWidth: 1.5, borderColor: colors.border,
		backgroundColor: colors.bgSubtle, marginBottom: spacing.sm,
	},
	chipCardActive: { borderColor: colors.warning, backgroundColor: colors.warningBg },
	chipCardUsed: { opacity: 0.4, backgroundColor: colors.bgApp },
	chipIcon: { fontSize: 26 },
	chipName: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textPrimary, marginBottom: 2 },
	chipDesc: { fontSize: fontSize.xs, color: colors.textSecondary },
	chipTextUsed: { color: colors.textMuted },
	cancelChipBtn: {
		paddingHorizontal: spacing.sm, paddingVertical: spacing.xs,
		backgroundColor: colors.danger, borderRadius: radius.sm,
	},
	cancelChipBtnText: { color: colors.textInverse, fontSize: fontSize.xs, fontWeight: fontWeight.bold },
	closeBtn: {
		marginTop: spacing.md, alignSelf: 'flex-end', paddingVertical: 8,
		paddingHorizontal: spacing.lg, borderRadius: radius.pill,
		backgroundColor: colors.bgSubtle, borderWidth: 1, borderColor: colors.border,
	},
	closeText: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textSecondary },
});
