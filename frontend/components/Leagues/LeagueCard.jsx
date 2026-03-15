import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, Alert } from 'react-native';
import { colors, fontSize, fontWeight, radius, spacing, shadow } from '../../utils/theme';

/**
 * Card representing a single league in the user's league list.
 *
 * Props:
 *   league   – league object from the API
 *   user     – current user object (to determine creator status)
 *   onEdit   – (league) => void
 *   onDelete – (league) => void
 *   onOpen   – (league) => void  (navigates to ranking)
 */
export default function LeagueCard({ league, user, onEdit, onDelete, onOpen }) {
	const pts = league.totalPoints ?? league.points ?? league.pts ?? '-';
	const pos = league.position ?? league.ranking ?? '-';
	const participants = league.participants ?? league.currentTeams ?? league.maxTeams ?? '-';
	const isCreator = user && (league.createdById === user.id || league.createdBy?.id === user.id);
	const description = (league.description || '').trim();

	const showOptions = () => {
		const buttons = [
			{ text: 'Ver código', onPress: () => Alert.alert('Código de la liga', league.code ? String(league.code) : 'Sin código') },
		];
		if (isCreator) {
			buttons.push(
				{ text: 'Editar', onPress: () => onEdit(league) },
				{ text: 'Eliminar', onPress: () => onDelete(league), style: 'destructive' }
			);
		}
		buttons.push({ text: 'Cancelar', style: 'cancel' });
		Alert.alert('Opciones de liga', league.name, buttons);
	};

	return (
		<View style={styles.leagueCard}>
			<View style={styles.cardAccent} />
			<View style={styles.cardBody}>
				<View style={styles.cardHeader}>
					<View style={styles.identityWrap}>
						<Text style={styles.sectionEyebrow}>Liga</Text>
						<Text style={styles.leagueName} numberOfLines={1}>{league.name}</Text>
						{description ? (
							<Text style={styles.leagueDesc} numberOfLines={2}>{description}</Text>
						) : (
							<Text style={styles.leagueDescMuted}>Sin descripcion</Text>
						)}
					</View>
					<View style={styles.headerActions}>
						{league.code ? (
							<View style={styles.codePill}>
								<Text style={styles.codeLabel}>Codigo</Text>
								<Text style={styles.codeText}>{String(league.code)}</Text>
							</View>
						) : null}
						<TouchableOpacity onPress={showOptions} style={styles.gearBtn} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
							<Image source={require('../../assets/header/gear.png')} style={styles.gearIcon} />
						</TouchableOpacity>
					</View>
				</View>

				<View style={styles.metricsWrap}>
					<View style={styles.metricCard}>
						<Text style={styles.metricLabel}>Puntos</Text>
						<Text style={styles.metricValue}>{pts}</Text>
					</View>
					<View style={styles.metricCard}>
						<Text style={styles.metricLabel}>Posicion</Text>
						<Text style={styles.metricValue}>{pos ?? '-'}</Text>
					</View>
					<View style={styles.metricCard}>
						<Text style={styles.metricLabel}>Jugadores</Text>
						<Text style={styles.metricValue}>{participants}</Text>
					</View>
				</View>

				<TouchableOpacity style={styles.ctaBtn} activeOpacity={0.85} onPress={() => onOpen(league)}>
					<Text style={styles.ctaText}>Ver clasificacion</Text>
				</TouchableOpacity>
			</View>
		</View>
	);
}

const styles = StyleSheet.create({
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
	cardAccent: { width: 8, borderRadius: 999, backgroundColor: colors.primary },
	cardBody: { flex: 1 },
	cardHeader: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between', gap: spacing.md },
	identityWrap: { flex: 1, gap: 4 },
	sectionEyebrow: { fontSize: 10, textTransform: 'uppercase', letterSpacing: 0.8, color: colors.textMuted, fontWeight: fontWeight.semibold },
	leagueName: { fontSize: fontSize.lg, fontWeight: fontWeight.extrabold, color: colors.textPrimary },
	leagueDesc: { color: colors.textSecondary, fontSize: fontSize.sm, lineHeight: 18 },
	leagueDescMuted: { color: colors.textMuted, fontSize: fontSize.sm },
	headerActions: { alignItems: 'flex-end', gap: spacing.sm },
	codePill: {
		backgroundColor: colors.successBg,
		borderRadius: radius.md,
		paddingHorizontal: spacing.sm,
		paddingVertical: 6,
		borderWidth: 1,
		borderColor: colors.primaryMuted,
		minWidth: 84,
	},
	codeLabel: { fontSize: 9, textTransform: 'uppercase', letterSpacing: 0.8, color: colors.primaryDark, fontWeight: fontWeight.semibold },
	codeText: { marginTop: 2, color: colors.primaryDeep, fontWeight: fontWeight.bold, fontSize: fontSize.xs, letterSpacing: 0.8 },
	metricsWrap: { marginTop: spacing.md, flexDirection: 'row', gap: spacing.sm },
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
	metricLabel: { fontSize: 10, color: colors.textMuted, textTransform: 'uppercase', letterSpacing: 0.7, fontWeight: fontWeight.semibold },
	metricValue: { marginTop: 3, color: colors.textPrimary, fontSize: fontSize.md, fontWeight: fontWeight.extrabold },
	ctaBtn: {
		marginTop: spacing.md,
		backgroundColor: colors.primary,
		borderRadius: radius.md,
		paddingVertical: spacing.sm,
		alignItems: 'center',
		justifyContent: 'center',
		...shadow.sm,
	},
	ctaText: { color: colors.textInverse, fontSize: fontSize.sm, fontWeight: fontWeight.bold, letterSpacing: 0.3 },
	gearIcon: { width: 22, height: 22, tintColor: colors.textSecondary },
	gearBtn: {
		width: 34, height: 34, borderRadius: 17,
		backgroundColor: colors.bgSubtle,
		borderWidth: 1, borderColor: colors.border,
		justifyContent: 'center', alignItems: 'center',
	},
});
