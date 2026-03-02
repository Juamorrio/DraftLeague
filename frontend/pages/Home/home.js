import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, Image, ActivityIndicator, TouchableOpacity } from 'react-native';
import { useMatches } from '../../context/MatchesContext';

export default function Home() {
	const { 
		playedMatches: allPlayedData, 
		upcomingMatches: allUpcomingData, 
		teamImages, 
		loading 
	} = useMatches();

	const [playedMatches, setPlayedMatches] = useState([]);
	const [upcomingMatches, setUpcomingMatches] = useState([]);
	const [selectedJornada, setSelectedJornada] = useState(null);
	const [availableJornadas, setAvailableJornadas] = useState([]);

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

	if (loading) {
		return (
			<View style={styles.loadingContainer}>
				<ActivityIndicator size="large" color="#159d9d" />
				<Text style={styles.loadingText}>Cargando partidos...</Text>
			</View>
		);
	}

	return (
		<ScrollView style={styles.container}>
			<View style={styles.content}>
				<View style={styles.header}>
					<Text style={styles.mainTitle}>Home</Text>
					<Text style={styles.subtitle}>Resultados y próximos partidos</Text>
				</View>
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
					<View style={styles.section}>
						<Text style={styles.sectionTitle}>Próximos Partidos</Text>
						{upcomingMatches.map((match, index) => (
							<MatchCard key={`upcoming-${index}`} match={match} isUpcoming={true} />
						))}
					</View>
				)}

				{playedMatches.length > 0 && (
					<View style={styles.section}>
						<Text style={styles.sectionTitle}>Resultados</Text>
						{playedMatches.map((match, index) => (
							<MatchCard key={`played-${index}`} match={match} isUpcoming={false} />
						))}
					</View>
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
		backgroundColor: '#f8fafc',
	},
	content: {
		padding: 16,
		paddingBottom: 100,
	},
	header: {
		marginBottom: 24,
		paddingTop: 8,
	},
	mainTitle: {
		fontSize: 32,
		fontWeight: '800',
		color: '#0f172a',
        alignSelf: 'center',
		marginBottom: 4,
	},
	subtitle: {
		fontSize: 16,
		color: '#64748b',
        alignSelf: 'center',
	},
	jornadaSelector: {
		marginBottom: 24,
	},
	jornadaSelectorLabel: {
		fontSize: 14,
		fontWeight: '700',
		color: '#64748b',
		marginBottom: 12,
		paddingLeft: 4,
	},
	jornadaScrollView: {
		flexGrow: 0,
	},
	jornadaChip: {
		backgroundColor: '#ffffff',
		paddingVertical: 10,
		paddingHorizontal: 16,
		borderRadius: 20,
		marginRight: 8,
		borderWidth: 2,
		borderColor: '#e2e8f0',
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 2 },
		shadowOpacity: 0.05,
		shadowRadius: 4,
		elevation: 2,
	},
	jornadaChipSelected: {
		backgroundColor: '#197319ff',
		borderColor: '#013055',
	},
	jornadaChipText: {
		fontSize: 14,
		fontWeight: '600',
		color: '#64748b',
	},
	jornadaChipTextSelected: {
		color: '#ffffff',
		fontWeight: '700',
	},
	section: {
		marginBottom: 32,
	},
	sectionTitle: {
		fontSize: 20,
		fontWeight: '700',
		color: '#1e293b',
		marginBottom: 12,
		paddingLeft: 4,
	},
	matchCard: {
		backgroundColor: '#ffffff',
		borderRadius: 16,
		marginBottom: 12,
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 2 },
		shadowOpacity: 0.06,
		shadowRadius: 8,
		elevation: 2,
		overflow: 'hidden',
	},
	matchContent: {
		flexDirection: 'row',
		alignItems: 'center',
		padding: 16,
		justifyContent: 'space-between',
	},
	teamContainer: {
		flex: 1,
		alignItems: 'center',
		gap: 8,
	},
	teamImageWrapper: {
		width: 50,
		height: 50,
		borderRadius: 25,
		backgroundColor: '#f1f5f9',
		justifyContent: 'center',
		alignItems: 'center',
		borderWidth: 2,
		borderColor: '#e2e8f0',
	},
	teamImage: {
		width: 40,
		height: 40,
		resizeMode: 'contain',
	},
	teamImagePlaceholder: {
		width: 40,
		height: 40,
		borderRadius: 20,
		backgroundColor: '#cbd5e1',
		justifyContent: 'center',
		alignItems: 'center',
	},
	placeholderText: {
		fontSize: 18,
		fontWeight: '700',
		color: '#64748b',
	},
	teamName: {
		fontSize: 13,
		fontWeight: '600',
		color: '#334155',
		textAlign: 'center',
		maxWidth: 90,
	},
	scoreContainer: {
		paddingHorizontal: 20,
		justifyContent: 'center',
		alignItems: 'center',
	},
	scoreBox: {
		flexDirection: 'row',
		alignItems: 'center',
		backgroundColor: '#f1f5f9',
		borderRadius: 12,
		paddingVertical: 8,
		paddingHorizontal: 16,
		gap: 8,
	},
	scoreText: {
		fontSize: 24,
		fontWeight: '800',
		color: '#0f172a',
		minWidth: 28,
		textAlign: 'center',
	},
	scoreSeparator: {
		fontSize: 20,
		fontWeight: '600',
		color: '#94a3b8',
	},
	xgRow: {
		marginTop: 4,
		alignItems: 'center',
	},
	xgText: {
		fontSize: 11,
		fontWeight: '600',
		color: '#94a3b8',
	},
	dateContainer: {
		backgroundColor: '#fef3c7',
		borderRadius: 12,
		paddingVertical: 8,
		paddingHorizontal: 12,
	},
	dateText: {
		fontSize: 12,
		fontWeight: '700',
		color: '#92400e',
		textAlign: 'center',
	},
	emptyState: {
		padding: 48,
		alignItems: 'center',
	},
	emptyText: {
		fontSize: 16,
		color: '#94a3b8',
		textAlign: 'center',
	},
});
