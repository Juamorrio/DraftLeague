import React from 'react';
import { Modal, View, Text, TextInput, TouchableOpacity, StyleSheet, Switch, ActivityIndicator, ScrollView, KeyboardAvoidingView, Platform } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { colors, fontSize, fontWeight, radius, spacing } from '../../utils/theme';

export default function FormLeague({
	visible,
	onClose,
	name,
	setName,
	description,
	setDescription,
	maxTeams,
	setMaxTeams,
	initialBudget,
	setInitialBudget,
	marketEndHour,
	setMarketEndHour,
	captainEnable,
	setCaptainEnable,
	canSubmit,
	loading,
	error,
	fieldErrors = {},
	onCreate,
	isEditing = false,
}) {
	return (
		<Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
			<View style={styles.modalBackdrop}>
				<LinearGradient colors={[colors.gradientStart, colors.gradientEnd]} start={{ x: 0, y: 0 }} end={{ x: 0, y: 1 }} style={styles.modalCard}>
					<Text style={styles.title}>{isEditing ? 'Editar Liga' : 'Nueva Liga'}</Text>				<KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
				<ScrollView keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
					<View style={styles.row}>
						<Text style={styles.label}>Nombre:</Text>
						<TextInput value={name} onChangeText={setName} style={[styles.input, fieldErrors.name ? styles.inputError : null]} />
						{fieldErrors.name ? <Text style={styles.errorText}>{fieldErrors.name}</Text> : null}
					</View>

					<View style={styles.row}>
						<Text style={styles.label}>Descripción:</Text>
						<TextInput value={description} onChangeText={setDescription} style={styles.input} />
					</View>

					{!isEditing && (
						<>
							<View style={styles.row}>
								<Text style={styles.label}>Equipos máx.:</Text>
								<TextInput value={maxTeams} onChangeText={setMaxTeams} keyboardType="number-pad" style={[styles.input, fieldErrors.maxTeams ? styles.inputError : null]} />
								{fieldErrors.maxTeams ? <Text style={styles.errorText}>{fieldErrors.maxTeams}</Text> : null}
							</View>

							<View style={styles.row}>
								<Text style={styles.label}>Presupuesto inicial:</Text>
								<TextInput value={initialBudget} onChangeText={setInitialBudget} keyboardType="number-pad" style={[styles.input, fieldErrors.initialBudget ? styles.inputError : null]} />
								{fieldErrors.initialBudget ? <Text style={styles.errorText}>{fieldErrors.initialBudget}</Text> : null}
							</View>

							<View style={styles.row}>
								<Text style={styles.label}>Cierre mercado (HH:mm):</Text>
								<TextInput value={marketEndHour} onChangeText={setMarketEndHour} style={[styles.input, fieldErrors.marketEndHour ? styles.inputError : null]} />
								{fieldErrors.marketEndHour ? <Text style={styles.errorText}>{fieldErrors.marketEndHour}</Text> : null}
							</View>

							<View style={styles.rowSwitch}>
								<Text style={styles.label}>Capitán habilitado</Text>
								<Switch value={captainEnable} onValueChange={setCaptainEnable} />
							</View>

						</>
					)}

					{error ? <Text style={styles.error}>{error}</Text> : null}

					<View style={styles.actions}>
						<TouchableOpacity style={[styles.actionBtn, styles.cancel]} onPress={onClose}>
							<Text style={styles.actionText}>Cancelar</Text>
						</TouchableOpacity>
						<TouchableOpacity
							style={[styles.actionBtn, !canSubmit || loading ? styles.disabled : styles.save]}
							onPress={onCreate}
							disabled={!canSubmit || loading}
						>
							{loading ? <ActivityIndicator color={colors.textInverse} /> : <Text style={styles.actionText}>Guardar</Text>}
						</TouchableOpacity>
					</View>				</ScrollView>
				</KeyboardAvoidingView>				</LinearGradient>
			</View>
		</Modal>
	);
}

const styles = StyleSheet.create({
	modalBackdrop: { flex: 1, backgroundColor: colors.overlay, alignItems: 'center', justifyContent: 'center', padding: spacing.lg },
	modalCard: { width: '94%', maxWidth: 520, maxHeight: '90%', borderRadius: radius.xl, paddingVertical: spacing.xl, paddingHorizontal: spacing.xl },
	title: { fontSize: fontSize.xl, color: colors.textInverse, fontWeight: fontWeight.extrabold, textAlign: 'center', marginBottom: spacing.md },
	row: { marginVertical: 6 },
	rowSwitch: { marginTop: spacing.sm, paddingHorizontal: 6, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
	label: { color: colors.border, marginBottom: spacing.sm / 2, fontWeight: fontWeight.semibold },
	input: { backgroundColor: colors.bgCard, borderRadius: radius.md, paddingHorizontal: spacing.md, height: 48, color: colors.textPrimary, borderWidth: 1.5, borderColor: colors.border },
	inputError: { borderColor: colors.danger },
	actions: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: spacing.md, gap: spacing.sm },
	actionBtn: { paddingHorizontal: 18, paddingVertical: spacing.sm, borderRadius: radius.md },
	cancel: { backgroundColor: colors.textSecondary },
	save: { backgroundColor: colors.primary },
	disabled: { backgroundColor: colors.textMuted },
	error: {
		color: colors.dangerBg,
		backgroundColor: 'rgba(239,68,68,0.20)',
		borderRadius: radius.md,
		paddingHorizontal: spacing.sm,
		paddingVertical: spacing.xs,
		marginTop: spacing.sm,
		fontWeight: fontWeight.semibold,
	},
	errorText: { color: colors.dangerBg, marginTop: spacing.xs, fontSize: fontSize.sm, fontWeight: fontWeight.semibold },
	actionText: { color: colors.textInverse, fontWeight: fontWeight.bold },
});