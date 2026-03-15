import React, { useState, useEffect, useRef } from 'react';
import { View, Text, StyleSheet, ScrollView, Image, ActivityIndicator, TouchableOpacity, Animated, RefreshControl } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useMatches } from '../../context/MatchesContext';
import { useLeague } from '../../context/LeagueContext';
import { colors, fontSize, fontWeight, fontFamily, radius, spacing, shadow } from '../../utils/theme';

export default function Home({ navigation }) {
	const { selectedLeague } = useLeague();
	const { 
		playedMatches: allPlayedData, 
		upcomingMatches: allUpcomingData, 
		teamImages, 
		loading,
		error: matchesError,
		refetch,
	} = useMatches();

	const [refreshing, setRefreshing] = useState(false);

	const onRefresh = async () => {
		setRefreshing(true);
		refetch();
	};

	// Clear refreshing when loading finishes
	useEffect(() => {
		if (!loading) setRefreshing(false);
	}, [loading]);

	const [playedMatches, setPlayedMatches] = useState([]);
	const [upcomingMatches, setUpcomingMatches] = useState([]);
	const [selectedJornada, setSelectedJornada] = useState(null);
	const [availableJornadas, setAvailableJornadas] = useState([]);
	const fadeAnim = useRef(new Animated.Value(1)).current;

	const animateIn = () => {
		fadeAnim.setValue(0);
		Animated.timing(fadeAnim, {
			toValue: 1,
			duration: 280,
			useNativeDriver: true,
		}).start();
	};

	useEffect(() => {
		if (Object.keys(allPlayedData).length === 0 && Object.keys(allUpcomingData).length === 0) return;

		const playedJornadas = Object.keys(allPlayedData);
		const upcomingJornadas = Object.keys(allUpcomingData);
		const allJornadas = [...new Set([...playedJornadas, ...upcomingJornadas])].sort((a, b) => {
			const numA = parseInt(a.split('_')[1]);
			const numB = parseInt(b.split('_')[1]);
			return numA - numB;
		});
		setAvailableJornadas(allJornadas);

		if (selectedJornada === null) {
			if (upcomingJornadas.length > 0) {
				const sortedUpcoming = [...upcomingJornadas].sort((a, b) => {
					const numA = parseInt(a.split('_')[1]);
					const numB = parseInt(b.split('_')[1]);
					return numA - numB;
				});
				setSelectedJornada(sortedUpcoming[0]);
			} else if (playedJornadas.length > 0) {
				// No upcoming matches – default to the most recent played jornada
				const sortedPlayed = [...playedJornadas].sort((a, b) => {
					const numA = parseInt(a.split('_')[1]);
					const numB = parseInt(b.split('_')[1]);
					return numB - numA;
				});
				setSelectedJornada(sortedPlayed[0]);
			}
			return;
		}

		if (selectedJornada) {
			const played = allPlayedData[selectedJornada] || [];
			const playedKeys = new Set(
				played.map(m => `${m.homeTeamId}_${m.awayTeamId}`)
			);
			const upcomingData = (allUpcomingData[selectedJornada] || []).filter(
				m => !playedKeys.has(`${m.homeTeamId}_${m.awayTeamId}`)
			);
			const sortedUpcoming = [...upcomingData].sort((a, b) => {
				if (!a.matchDate || !b.matchDate) return 0;
				return new Date(a.matchDate) - new Date(b.matchDate);
			});
			setPlayedMatches(played);
			setUpcomingMatches(sortedUpcoming);
			animateIn();
		}
	}, [allPlayedData, allUpcomingData, selectedJornada]);

	const formatDate = (dateString) => {
		if (!dateString) return '';
		const date = new Date(dateString);
		return date.toLocaleDateString('es-ES', { 
			day: '2-digit', 
			month: 'short', 
			hour: '2-digit', 
			minute: '2-digit' 
		});
	};

	const MatchCard = ({ match, isUpcoming }) => (
		<View style={styles.matchCard}>
			<View style={styles.matchContent}>
				{/* Equipo Local */}
				<View style={styles.teamContainer}>
					<View style={styles.teamImageWrapper}>
						{teamImages[match.homeTeamId] ? (
							<Image 
								source={{ uri: teamImages[match.homeTeamId] }} 
								style={styles.teamImage}
							/>
						) : (
							<View style={styles.teamImagePlaceholder}>
								<Text style={styles.placeholderText}>?</Text>
							</View>
						)}
					</View>
				</View>

				{/* Marcador o Fecha */}
				<View style={styles.scoreContainer}>
					{isUpcoming ? (
						<View style={styles.dateContainer}>
							<Text style={styles.dateText}>{formatDate(match.matchDate)}</Text>
						</View>
					) : (
						<>
							<View style={styles.scoreBox}>
								<Text style={styles.scoreText}>{match.homeScore}</Text>
								<Text style={styles.scoreSeparator}>-</Text>
								<Text style={styles.scoreText}>{match.awayScore}</Text>
							</View>
							{(match.homeXg != null || match.awayXg != null) && (
								<View style={styles.xgRow}>
									<Text style={styles.xgText}>
										xG: {match.homeXg != null ? match.homeXg.toFixed(2) : '-'}
										{' - '}
										{match.awayXg != null ? match.awayXg.toFixed(2) : '-'}
									</Text>
								</View>
							)}
						</>
					)}
				</View>

				{/* Equipo Visitante */}
				<View style={styles.teamContainer}>
					<View style={styles.teamImageWrapper}>
						{teamImages[match.awayTeamId] ? (
							<Image 
								source={{ uri: teamImages[match.awayTeamId] }} 
								style={styles.teamImage}
							/>
						) : (
							<View style={styles.teamImagePlaceholder}>
								<Text style={styles.placeholderText}>?</Text>
							</View>
						)}
					</View>
				</View>
			</View>
		</View>
	);

	if (loading && !refreshing) {
		return (
			<View style={styles.loadingContainer}>
				<ActivityIndicator size="large" color={colors.primary} />
				<Text style={styles.loadingText}>Cargando partidos...</Text>
			</View>
		);
	}

	if (matchesError && !loading) {
		return (
			<View style={styles.loadingContainer}>
				<Text style={styles.errorText}>No se pudieron cargar los partidos</Text>
				<TouchableOpacity style={styles.retryBtn} onPress={refetch}>
					<Text style={styles.retryBtnText}>Reintentar</Text>
				</TouchableOpacity>
			</View>
		);
	}

	return (
		<ScrollView
			style={styles.container}
			refreshControl={
				<RefreshControl
					refreshing={refreshing}
					onRefresh={onRefresh}
					colors={[colors.primary]}
					tintColor={colors.primary}
				/>
			}
		>
			<View style={styles.content}>
				<View style={styles.header}>
					<Text style={styles.mainTitle}>Home</Text>
					<Text style={styles.subtitle}>Resultados y próximos partidos</Text>
				</View>

				{/* ── Onboarding banner ── */}
				{!selectedLeague && (
					<TouchableOpacity
						style={styles.onboardingBanner}
						onPress={() => navigation.navigate('Leagues')}
						activeOpacity={0.85}
					>
						<Ionicons name="trophy-outline" size={28} color={colors.primary} />
						<View style={styles.onboardingText}>
							<Text style={styles.onboardingTitle}>¡Únete a una liga!</Text>
							<Text style={styles.onboardingSubtitle}>Crea o únete a una liga para competir con tus amigos</Text>
						</View>
						<Ionicons name="chevron-forward" size={20} color={colors.primary} />
					</TouchableOpacity>
				)}

				<View style={styles.jornadaSelector}>
					<Text style={styles.jornadaSelectorLabel}>Jornada:</Text>
					<ScrollView 
						horizontal 
						showsHorizontalScrollIndicator={false}
						style={styles.jornadaScrollView}
					>

						{availableJornadas.map((jornada) => (
							<TouchableOpacity
								key={jornada}
								style={[
									styles.jornadaChip,
									selectedJornada === jornada && styles.jornadaChipSelected
								]}
								onPress={() => setSelectedJornada(jornada)}
							>
								<Text style={[
									styles.jornadaChipText,
									selectedJornada === jornada && styles.jornadaChipTextSelected
								]}>
									{jornada.replace('jornada_', 'J')}
								</Text>
							</TouchableOpacity>
						))}
					</ScrollView>
				</View>

				{upcomingMatches.length > 0 && (
				<Animated.View style={[styles.section, { opacity: fadeAnim }]}>
					<Text style={styles.sectionTitle}>Próximos Partidos</Text>
					{upcomingMatches.map((match, index) => (
						<MatchCard key={`upcoming-${index}`} match={match} isUpcoming={true} />
					))}
				</Animated.View>
			)}

				{playedMatches.length > 0 && (
				<Animated.View style={[styles.section, { opacity: fadeAnim }]}>
					<Text style={styles.sectionTitle}>Resultados</Text>
					{playedMatches.map((match, index) => (
						<MatchCard key={`played-${index}`} match={match} isUpcoming={false} />
					))}
				</Animated.View>
			)}

				{playedMatches.length === 0 && upcomingMatches.length === 0 && (
					<View style={styles.emptyState}>
						<Text style={styles.emptyText}>No hay partidos disponibles</Text>
					</View>
				)}
			</View>
		</ScrollView>
	);
}

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: colors.bgApp,
	},
	content: {
		padding: spacing.lg,
		paddingBottom: 100,
	},
	header: {
		marginBottom: spacing['2xl'],
		paddingTop: spacing.sm,
		alignItems: 'center',
	},
	mainTitle: {
		fontFamily: fontFamily.displayBold,
		fontSize: 34,
		color: colors.textPrimary,
		marginBottom: 2,
		letterSpacing: 0.5,
	},
	subtitle: {
		fontFamily: fontFamily.bodyMedium,
		fontSize: fontSize.sm,
		color: colors.textMuted,
	},
	loadingContainer: {
		flex: 1,
		justifyContent: 'center',
		alignItems: 'center',
		backgroundColor: colors.bgApp,
		gap: spacing.md,
	},
	loadingText: {
		fontSize: fontSize.md,
		color: colors.textMuted,
	},
	errorText: {
		fontSize: fontSize.md,
		color: colors.danger,
		textAlign: 'center',
		marginBottom: spacing.lg,
	},
	retryBtn: {
		backgroundColor: colors.primary,
		paddingHorizontal: spacing.xl,
		paddingVertical: spacing.md,
		borderRadius: radius.pill,
	},
	retryBtnText: {
		color: colors.textInverse,
		fontWeight: fontWeight.bold,
		fontSize: fontSize.sm,
	},
	jornadaSelector: {
		marginBottom: spacing['2xl'],
	},
	jornadaSelectorLabel: {
		fontSize: fontSize.xs,
		fontWeight: fontWeight.bold,
		color: colors.textMuted,
		marginBottom: spacing.md,
		letterSpacing: 1,
		textTransform: 'uppercase',
	},
	jornadaScrollView: {
		flexGrow: 0,
	},
	jornadaChip: {
		backgroundColor: colors.bgCard,
		paddingVertical: 8,
		paddingHorizontal: 16,
		borderRadius: radius.pill,
		marginRight: 8,
		borderWidth: 1.5,
		borderColor: colors.border,
		...shadow.sm,
	},
	jornadaChipSelected: {
		backgroundColor: colors.primary,
		borderColor: colors.primaryDark,
	},
	jornadaChipText: {
		fontSize: fontSize.sm,
		fontWeight: fontWeight.semibold,
		color: colors.textSecondary,
	},
	jornadaChipTextSelected: {
		color: colors.textInverse,
		fontWeight: fontWeight.bold,
	},
	section: {
		marginBottom: spacing['3xl'],
	},
	sectionTitle: {
		fontFamily: fontFamily.displaySemi,
		fontSize: fontSize.xl,
		color: colors.textPrimary,
		marginBottom: spacing.md,
		letterSpacing: 0.3,
	},
	matchCard: {
		backgroundColor: colors.bgCard,
		borderRadius: radius.lg,
		marginBottom: spacing.md,
		...shadow.sm,
		overflow: 'hidden',
	},
	matchContent: {
		flexDirection: 'row',
		alignItems: 'center',
		paddingVertical: spacing.lg,
		paddingHorizontal: spacing.xl,
		justifyContent: 'space-between',
	},
	teamContainer: {
		flex: 1,
		alignItems: 'center',
	},
	teamImageWrapper: {
		width: 52,
		height: 52,
		borderRadius: 26,
		backgroundColor: colors.bgSubtle,
		justifyContent: 'center',
		alignItems: 'center',
		borderWidth: 1.5,
		borderColor: colors.border,
	},
	teamImage: {
		width: 38,
		height: 38,
		resizeMode: 'contain',
	},
	teamImagePlaceholder: {
		width: 38,
		height: 38,
		borderRadius: 19,
		backgroundColor: colors.borderStrong,
		justifyContent: 'center',
		alignItems: 'center',
	},
	placeholderText: {
		fontSize: fontSize.lg,
		fontWeight: fontWeight.bold,
		color: colors.textMuted,
	},
	teamName: {
		fontSize: fontSize.xs,
		fontWeight: fontWeight.semibold,
		color: colors.textSecondary,
		textAlign: 'center',
		maxWidth: 90,
		marginTop: 4,
	},
	scoreContainer: {
		paddingHorizontal: spacing.xl,
		justifyContent: 'center',
		alignItems: 'center',
		gap: 4,
	},
	scoreBox: {
		flexDirection: 'row',
		alignItems: 'center',
		backgroundColor: colors.bgSubtle,
		borderRadius: radius.md,
		paddingVertical: 10,
		paddingHorizontal: 16,
		gap: 10,
	},
	scoreText: {
		fontFamily: fontFamily.displayBold,
		fontSize: 32,
		color: colors.textPrimary,
		minWidth: 24,
		textAlign: 'center',
	},
	scoreSeparator: {
		fontFamily: fontFamily.displaySemi,
		fontSize: fontSize.lg,
		color: colors.textMuted,
	},
	xgRow: {
		alignItems: 'center',
	},
	xgText: {
		fontSize: fontSize.xs,
		fontWeight: fontWeight.semibold,
		color: colors.textMuted,
	},
	dateContainer: {
		backgroundColor: colors.warningBg,
		borderRadius: radius.md,
		paddingVertical: 10,
		paddingHorizontal: 14,
		borderWidth: 1,
		borderColor: colors.warning,
	},
	dateText: {
		fontSize: fontSize.xs,
		fontWeight: fontWeight.bold,
		color: colors.warningDeep,
		textAlign: 'center',
	},
	emptyState: {
		padding: 48,
		alignItems: 'center',
	},
	emptyText: {
		fontSize: fontSize.md,
		color: colors.textMuted,
		textAlign: 'center',
	},
	onboardingBanner: {
		flexDirection: 'row',
		alignItems: 'center',
		backgroundColor: colors.successBg,
		borderRadius: radius.lg,
		padding: spacing.lg,
		marginBottom: spacing.lg,
		gap: spacing.md,
		borderWidth: 1.5,
		borderColor: colors.primaryMuted,
		...shadow.sm,
	},
	onboardingText: {
		flex: 1,
		gap: 2,
	},
	onboardingTitle: {
		fontFamily: fontFamily.displaySemi,
		fontSize: fontSize.md,
		color: colors.primaryDark,
	},
	onboardingSubtitle: {
		fontFamily: fontFamily.body,
		fontSize: fontSize.xs,
		color: colors.primary,
		lineHeight: 16,
	},
});
