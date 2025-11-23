import React from 'react';
import { Modal, View, Text, TextInput, TouchableOpacity, StyleSheet, Switch, ActivityIndicator } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';

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
	wildCardsEnable,
	setWildCardsEnable,
	canSubmit,
	loading,
	error,
	onCreate,
}) {
	return (
		<Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
			<View style={styles.modalBackdrop}>
				<LinearGradient colors={["#197319", "#013055"]} start={{ x: 0, y: 0 }} end={{ x: 0, y: 1 }} style={styles.modalCard}>
					<Text style={styles.title}>Nueva Liga</Text>

					<View style={styles.row}>
						<Text style={styles.label}>Nombre:</Text>
						<TextInput value={name} onChangeText={setName} style={styles.input} />
					</View>

					<View style={styles.row}>
						<Text style={styles.label}>Descripción:</Text>
						<TextInput value={description} onChangeText={setDescription} style={styles.input} />
					</View>

					<View style={styles.row}>
						<Text style={styles.label}>Equipos máx.:</Text>
						<TextInput value={maxTeams} onChangeText={setMaxTeams} keyboardType="number-pad" style={styles.input} />
					</View>

					<View style={styles.row}>
						<Text style={styles.label}>Presupuesto inicial:</Text>
						<TextInput value={initialBudget} onChangeText={setInitialBudget} keyboardType="number-pad" style={styles.input} />
					</View>

					<View style={styles.row}>
						<Text style={styles.label}>Cierre mercado (HH:mm):</Text>
						<TextInput value={marketEndHour} onChangeText={setMarketEndHour} style={styles.input} />
					</View>

					<View style={styles.rowSwitch}>
						<Text style={styles.label}>Capitán habilitado</Text>
						<Switch value={captainEnable} onValueChange={setCaptainEnable} />
					</View>

					<View style={styles.rowSwitch}>
						<Text style={styles.label}>Wildcards habilitados</Text>
						<Switch value={wildCardsEnable} onValueChange={setWildCardsEnable} />
					</View>

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
							{loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.actionText}>Guardar</Text>}
						</TouchableOpacity>
					</View>
				</LinearGradient>
			</View>
		</Modal>
	);
}

const styles = StyleSheet.create({
	modalBackdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.35)', alignItems: 'center', justifyContent: 'center', padding: 16 },
	modalCard: { width: '94%', maxWidth: 520, maxHeight: '90%', borderRadius: 18, paddingVertical: 20, paddingHorizontal: 20 },
	title: { fontSize: 22, color: '#fff', fontWeight: '800', textAlign: 'center', marginBottom: 10 },
	row: { marginVertical: 6 },
	rowSwitch: { marginTop: 8, paddingHorizontal: 6, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
	label: { color: '#e2e8f0', marginBottom: 6, fontWeight: '600' },
	input: { backgroundColor: '#ffffff', borderRadius: 12, paddingHorizontal: 12, height: 42, color: '#0f172a' },
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
	actionText: { color: '#fff', fontWeight: '700' },
});