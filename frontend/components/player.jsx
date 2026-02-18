import React, { useState, useEffect } from 'react';
import { View, Text, Image, StyleSheet, TouchableOpacity } from 'react-native';
import placeholder from '../assets/Player/placeholder.png';
import { authenticatedFetch } from '../services/authService'; 

export default function Player({
	name,
	points = 0,
	avatar,
	onPress,
	style,
	teamId,
	isCaptain = false
}) {
	const [teamImage, setTeamImage] = useState(null);
	const source = avatar || placeholder;
	const initials = (name || '')
		.split(' ')
		.map(s => s[0])
		.join('')
		.slice(0, 2)
		.toUpperCase();

	const loadTeamImage = async (teamId) => {
		try {
			const res = await authenticatedFetch(`/api/v1/players/load-image-team-player?teamId=${teamId}`);
			if (res.ok) {
				const data = await res.json();
				if (data.imageBytes) {
					return `data:image/png;base64,${data.imageBytes}`;
				}
			}
		} catch (e) {
			console.error('Error cargando imagen del equipo:', e);
		}
		return null;
	};

	useEffect(() => {
		if (teamId) {
			loadTeamImage(teamId).then(url => {
				if (url) setTeamImage(url);
			});
		}
	}, [teamId]);

	return (
		<TouchableOpacity onPress={onPress} style={[styles.wrapper, style]} activeOpacity={0.8}>
			<View style={styles.playerContainer}>
				<View style={[styles.avatarContainer, isCaptain && styles.captainBorder]}>
					{source ? (
						<Image source={source} style={styles.avatarImage} />
					) : (
						<View style={styles.avatarFallback}>
							<Text style={styles.fallbackText}>{initials || '?'}</Text>
						</View>
					)}
				</View>
				{isCaptain && (
					<View style={styles.captainBadge}>
						<Text style={styles.captainText}>C</Text>
					</View>
				)}
				{teamImage && (
					<View style={styles.teamBadge}>
						<Image source={{ uri: teamImage }} style={styles.teamImage} />
					</View>
				)}
				<View style={styles.badge}>
					<Text style={styles.badgeText}>{points}</Text>
				</View>
			</View>
		</TouchableOpacity>
	);
}

const styles = StyleSheet.create({
	wrapper: {
		padding: 6,
	},
	playerContainer: {
		position: 'relative',
		width: 80,
		height: 80,
	},
	avatarContainer: {
		width: 80,
		height: 80,
		borderRadius: 40,
		backgroundColor: '#f3f4f6',
		overflow: 'hidden',
		justifyContent: 'center',
		alignItems: 'center',
		borderWidth: 3,
		borderColor: '#ffffff',
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 2 },
		shadowOpacity: 0.1,
		shadowRadius: 4,
		elevation: 3,
	},
	avatarImage: {
		width: '85%',
		height: '85%',
		resizeMode: 'cover',
	},
	avatarFallback: {
		flex: 1,
		width: '100%',
		height: '100%',
		justifyContent: 'center',
		alignItems: 'center',
		backgroundColor: '#e0e7ff',
	},
	fallbackText: {
		fontSize: 20,
		fontWeight: '800',
		color: '#4f46e5',
	},
	badge: {
		position: 'absolute',
		bottom: -2,
		right: -2,
		backgroundColor: '#10b981', 
		borderRadius: 18,
		minWidth: 32,
		height: 32,
		paddingHorizontal: 8,
		justifyContent: 'center',
		alignItems: 'center',
		borderWidth: 3,
		borderColor: '#ffffff',
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 2 },
		shadowOpacity: 0.15,
		shadowRadius: 3,
		elevation: 4,
	},
	badgeText: {
		color: '#ffffff',
		fontWeight: '800',
		fontSize: 13,
	},
	teamBadge: {
		position: 'absolute',
		top: -4,
		right: -4,
		backgroundColor: '#ffffff',
		borderRadius: 14,
		width: 28,
		height: 28,
		justifyContent: 'center',
		alignItems: 'center',
		borderWidth: 2.5,
		borderColor: '#f3f4f6',
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 1 },
		shadowOpacity: 0.1,
		shadowRadius: 2,
		elevation: 3,
	},
	teamImage: {
		width: 22,
		height: 22,
		resizeMode: 'contain',
	},
	captainBorder: {
		borderColor: '#f59e0b',
		borderWidth: 3,
	},
	captainBadge: {
		position: 'absolute',
		top: -4,
		left: -4,
		backgroundColor: '#f59e0b',
		borderRadius: 12,
		width: 24,
		height: 24,
		justifyContent: 'center',
		alignItems: 'center',
		borderWidth: 2.5,
		borderColor: '#ffffff',
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 1 },
		shadowOpacity: 0.15,
		shadowRadius: 2,
		elevation: 4,
	},
	captainText: {
		color: '#ffffff',
		fontSize: 12,
		fontWeight: '900',
	},
});
