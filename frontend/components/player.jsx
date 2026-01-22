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
	teamId
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
				<View style={styles.avatarContainer}>
					{source ? (
						<Image source={source} style={styles.avatarImage} />
					) : (
						<View style={styles.avatarFallback}>
							<Text style={styles.fallbackText}>{initials || '?'}</Text>
						</View>
					)}
				</View>
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
		padding: 4,
	},
	playerContainer: {
		position: 'relative',
		width: 72,
		height: 72,
	},
	avatarContainer: {
		width: 72,
		height: 72,
		borderRadius: 36,
		backgroundColor: '#e5e7eb',
		overflow: 'hidden',
		justifyContent: 'center',
		alignItems: 'center',
	},
	avatarImage: {
		width: '80%',
		height: '80%',
		resizeMode: 'cover',
	},
	avatarFallback: {
		flex: 1,
		width: '100%',
		height: '100%',
		justifyContent: 'center',
		alignItems: 'center',
		backgroundColor: '#d1d5db',
	},
	fallbackText: {
		fontSize: 18,
		fontWeight: '700',
		color: '#374151',
	},
	badge: {
		position: 'absolute',
		bottom: 2,
		right: 2,
		backgroundColor: '#065f46', 
		borderRadius: 16,
		minWidth: 28,
		height: 28,
		paddingHorizontal: 8,
		justifyContent: 'center',
		alignItems: 'center',
		borderWidth: 2,
		borderColor: '#ffffff',
	},
	badgeText: {
		color: '#ffffff',
		fontWeight: '700',
		fontSize: 14,
	},
	teamBadge: {
		position: 'absolute',
		top: -4,
		right: -4,
		backgroundColor: '#ffffff',
		borderRadius: 12,
		width: 24,
		height: 24,
		justifyContent: 'center',
		alignItems: 'center',
		borderWidth: 2,
		borderColor: '#e5e7eb',
	},
	teamImage: {
		width: 20,
		height: 20,
		resizeMode: 'contain',
	},
});
