import React, { useState, useEffect } from 'react';
import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';
import { authenticatedFetch } from '../services/authService';
import { useLeague } from '../context/LeagueContext';

export default function NotificationBell({ onPress }) {
	const { selectedLeague } = useLeague();
	const [unreadCount, setUnreadCount] = useState(0);
	const [lastNotificationId, setLastNotificationId] = useState(0);

	useEffect(() => {
		if (!selectedLeague?.id) return;
		
		const checkNewNotifications = async () => {
			try {
				const res = await authenticatedFetch(
					`/api/v1/notifications/league/${selectedLeague.id}/new?lastId=${lastNotificationId}`
				);
				if (res.ok) {
					const newNotifications = await res.json();
					if (newNotifications.length > 0) {
						setUnreadCount(prev => prev + newNotifications.length);
						const maxId = Math.max(...newNotifications.map(n => n.id));
						setLastNotificationId(maxId);
					}
				}
			} catch (e) {
				console.error('Error checking notifications:', e);
			}
		};

		// Cargar notificaciones iniciales
		checkNewNotifications();

		// Verificar cada 30 segundos
		const interval = setInterval(checkNewNotifications, 30000);
		return () => clearInterval(interval);
	}, [selectedLeague, lastNotificationId]);

	const handlePress = () => {
		setUnreadCount(0);
		if (onPress) onPress();
	};

	return (
		<TouchableOpacity style={styles.bellContainer} onPress={handlePress}>
			<Text style={styles.bellIcon}>🔔</Text>
			{unreadCount > 0 && (
				<View style={styles.badge}>
					<Text style={styles.badgeText}>{unreadCount > 9 ? '9+' : unreadCount}</Text>
				</View>
			)}
		</TouchableOpacity>
	);
}

const styles = StyleSheet.create({
	bellContainer: {
		position: 'relative',
		padding: 8,
	},
	bellIcon: {
		fontSize: 24,
	},
	badge: {
		position: 'absolute',
		top: 4,
		right: 4,
		backgroundColor: '#ff3b30',
		borderRadius: 10,
		minWidth: 20,
		height: 20,
		justifyContent: 'center',
		alignItems: 'center',
		paddingHorizontal: 4,
	},
	badgeText: {
		color: 'white',
		fontSize: 12,
		fontWeight: 'bold',
	},
});
