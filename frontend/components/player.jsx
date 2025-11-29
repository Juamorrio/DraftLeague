import React from 'react';
import { View, Text, Image, StyleSheet, TouchableOpacity } from 'react-native';
import placeholder from '../assets/Player/placeholder.png'; 

export default function Player({
	name,
	points = 0,
	avatar,
	onPress,
	style,
}) {
	const source = avatar || placeholder;
	const initials = (name || '')
		.split(' ')
		.map(s => s[0])
		.join('')
		.slice(0, 2)
		.toUpperCase();

	return (
		<TouchableOpacity onPress={onPress} style={[styles.wrapper, style]} activeOpacity={0.8}>
			<View style={styles.avatarContainer}>
				{source ? (
					<Image source={source} style={styles.avatarImage} />
				) : (
					<View style={styles.avatarFallback}>
						<Text style={styles.fallbackText}>{initials || '?'}</Text>
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
		bottom: 4,
		right: 4,
		backgroundColor: '#065f46', 
		borderRadius: 16,
		minWidth: 32,
		height: 32,
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
});
