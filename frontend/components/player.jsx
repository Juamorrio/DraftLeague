import React, { useState, useEffect } from 'react';
import { View, Text, Image, StyleSheet, TouchableOpacity } from 'react-native';
import placeholder from '../assets/Player/placeholder.png';
import { authenticatedFetch } from '../services/authService';
import { colors, radius, shadow, fontFamily } from '../utils/theme';

// Module-scope cache: teamId → base64 data URI. Shared across all Player instances.
const teamImageCache = new Map();

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

	useEffect(() => {
		if (!teamId) return;
		// Return immediately if already cached
		if (teamImageCache.has(teamId)) {
			setTeamImage(teamImageCache.get(teamId));
			return;
		}
		let cancelled = false;
		authenticatedFetch(`/api/v1/players/load-image-team-player?teamId=${teamId}`)
			.then(res => res.ok ? res.json() : null)
			.then(data => {
				if (!cancelled && data?.imageBytes) {
					const uri = `data:image/png;base64,${data.imageBytes}`;
					teamImageCache.set(teamId, uri);
					setTeamImage(uri);
				}
			})
			.catch(e => console.error('Error cargando imagen del equipo:', e));
		return () => { cancelled = true; };
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
		backgroundColor: colors.bgSubtle,
		overflow: 'hidden',
		justifyContent: 'center',
		alignItems: 'center',
		borderWidth: 2.5,
		borderColor: colors.bgCard,
		...shadow.sm,
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
		backgroundColor: colors.primaryLight,
	},
	fallbackText: {
		fontSize: 20,
		fontWeight: '800',
		color: colors.primaryDark,
	},
	badge: {
		position: 'absolute',
		bottom: -2,
		right: -2,
		backgroundColor: colors.primary,
		borderRadius: radius.pill,
		minWidth: 30,
		height: 30,
		paddingHorizontal: 6,
		justifyContent: 'center',
		alignItems: 'center',
		borderWidth: 2.5,
		borderColor: colors.bgCard,
		...shadow.sm,
	},
	badgeText: {
		color: colors.textInverse,
		fontFamily: fontFamily.displayBold,
		fontSize: 12,
	},
	teamBadge: {
		position: 'absolute',
		top: -3,
		right: -3,
		backgroundColor: colors.bgCard,
		borderRadius: radius.pill,
		width: 26,
		height: 26,
		justifyContent: 'center',
		alignItems: 'center',
		borderWidth: 2,
		borderColor: colors.border,
		...shadow.sm,
	},
	teamImage: {
		width: 20,
		height: 20,
		resizeMode: 'contain',
	},
	captainBorder: {
		borderColor: colors.warning,
		borderWidth: 2.5,
	},
	captainBadge: {
		position: 'absolute',
		top: -3,
		left: -3,
		backgroundColor: colors.warning,
		borderRadius: radius.pill,
		width: 22,
		height: 22,
		justifyContent: 'center',
		alignItems: 'center',
		borderWidth: 2,
		borderColor: colors.bgCard,
		...shadow.sm,
	},
	captainText: {
		color: colors.textInverse,
		fontSize: 11,
		fontWeight: '900',
	},
});
