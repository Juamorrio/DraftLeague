import React, { useState, useEffect, useCallback } from 'react';
import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { authenticatedFetch } from '../services/authService';
import { useLeague } from '../context/LeagueContext';

const STORAGE_KEY = 'notif_last_seen_id';

export default function NotificationBell({ onPress, onRequestSync }) {
	const { selectedLeague } = useLeague();
	const [unreadCount, setUnreadCount] = useState(0);
	const [lastNotificationId, setLastNotificationId] = useState(0);
	const [ready, setReady] = useState(false);

	// Load persisted lastNotificationId on mount
	useEffect(() => {
		AsyncStorage.getItem(STORAGE_KEY).then(val => {
			if (val) setLastNotificationId(parseInt(val, 10));
			setReady(true);
		}).catch(() => setReady(true));
	}, []);

	// Persist whenever lastNotificationId changes
	useEffect(() => {
		if (!ready) return;
		AsyncStorage.setItem(STORAGE_KEY, String(lastNotificationId)).catch(() => {});
	}, [lastNotificationId, ready]);

	useEffect(() => {
		if (!selectedLeague?.id || !ready) return;

		let currentLastId = lastNotificationId;

		const checkNewNotifications = async () => {
			try {
				const res = await authenticatedFetch(
					`/api/v1/notifications/league/${selectedLeague.id}/new?lastId=${currentLastId}`
				);
				if (res.ok) {
					const newNotifications = await res.json();
					if (newNotifications.length > 0) {
						setUnreadCount(prev => prev + newNotifications.length);
						const maxId = Math.max(...newNotifications.map(n => n.id));
						currentLastId = maxId;
						setLastNotificationId(maxId);
					}
				}
			} catch (e) {
				console.error('Error checking notifications:', e);
			}
		};

		checkNewNotifications();
		const interval = setInterval(checkNewNotifications, 30000);
		return () => clearInterval(interval);
	}, [selectedLeague?.id, ready]);

	// Called by parent when modal is closed, with the max notification id seen in the modal
	const syncMaxSeen = useCallback((maxId) => {
		if (typeof maxId === 'number' && maxId > lastNotificationId) {
			setLastNotificationId(maxId);
		}
		setUnreadCount(0);
	}, [lastNotificationId]);

	const handlePress = () => {
		setUnreadCount(0);
		if (onPress) onPress();
		if (onRequestSync) onRequestSync(syncMaxSeen);
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
