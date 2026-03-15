import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Modal, TextInput } from 'react-native';
import { colors, fontSize, fontWeight, radius, spacing, shadow } from '../../utils/theme';

/**
 * Modal for entering a trade offer price for a target player.
 *
 * Props:
 *   visible      – boolean
 *   player       – player object | null  ({ fullName, name, marketValue })
 *   priceText    – string  (controlled input)
 *   onChangePrice – (text: string) => void
 *   onSend       – () => void
 *   onClose      – () => void
 *   submitting   – boolean
 */
export default function TradeOfferPriceModal({ visible, player, priceText, onChangePrice, onSend, onClose, submitting }) {
	return (
		<Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
			<View style={styles.backdrop}>
				<View style={styles.card}>
					<Text style={styles.title}>
						Hacer oferta por {player?.fullName || player?.name}
					</Text>
					<Text style={styles.meta}>
						Valor de mercado: €{(player?.marketValue || 0).toLocaleString('es-ES')}
					</Text>
					<Text style={styles.inputLabel}>Tu oferta (€)</Text>
					<TextInput
						style={styles.input}
						placeholder="Introduce el precio"
						keyboardType="numeric"
						value={priceText}
						onChangeText={onChangePrice}
					/>
					<View style={styles.actions}>
						<TouchableOpacity
							style={[styles.sendBtn, submitting && styles.sendBtnDisabled]}
							onPress={onSend}
							disabled={submitting}
						>
							<Text style={styles.sendBtnText}>
								{submitting ? 'Enviando...' : 'Enviar oferta'}
							</Text>
						</TouchableOpacity>
						<TouchableOpacity style={styles.cancelBtn} onPress={onClose}>
							<Text style={styles.cancelBtnText}>Cancelar</Text>
						</TouchableOpacity>
					</View>
				</View>
			</View>
		</Modal>
	);
}

const styles = StyleSheet.create({
	backdrop: { flex: 1, backgroundColor: colors.overlay, alignItems: 'center', justifyContent: 'center' },
	card: {
		width: '88%', backgroundColor: colors.bgCard,
		borderRadius: radius.xl, padding: spacing.lg, ...shadow.lg,
	},
	title: { fontSize: fontSize.md, fontWeight: fontWeight.bold, marginBottom: 4, color: colors.textPrimary },
	meta: { fontSize: 12, color: '#555', marginBottom: 8 },
	inputLabel: { fontSize: 12, fontWeight: '700', marginBottom: 4 },
	input: {
		borderWidth: 1, borderColor: '#ccc', borderRadius: 8,
		padding: 10, fontSize: 16, marginBottom: 12,
	},
	actions: { flexDirection: 'row', gap: 8 },
	sendBtn: {
		flex: 1, backgroundColor: '#7c3aed',
		paddingVertical: 8, paddingHorizontal: spacing.lg,
		borderRadius: radius.pill, alignItems: 'center',
	},
	sendBtnDisabled: { opacity: 0.6 },
	sendBtnText: { color: colors.textInverse, fontWeight: fontWeight.bold, fontSize: fontSize.sm, textAlign: 'center' },
	cancelBtn: {
		flex: 1, alignSelf: 'stretch', justifyContent: 'center', alignItems: 'center',
		paddingVertical: 8, paddingHorizontal: spacing.lg,
		borderRadius: radius.pill,
		backgroundColor: colors.bgSubtle, borderWidth: 1, borderColor: colors.border,
	},
	cancelBtnText: { fontSize: fontSize.sm, fontWeight: fontWeight.bold, color: colors.textSecondary },
});
