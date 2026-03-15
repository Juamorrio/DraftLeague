import React, { useReducer, useEffect, useCallback } from 'react';
import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { authenticatedFetch } from '../services/authService';
import { useLeague } from '../context/LeagueContext';
import { colors, fontSize, fontWeight, radius } from '../utils/theme';

const STORAGE_KEY = 'notif_last_seen_id';

const initialState = { unreadCount: 0, lastNotificationId: 0, ready: false };

function reducer(state, action) {
	switch (action.type) {
		case 'INIT':
			return { ...state, lastNotificationId: action.id, ready: true };
		case 'ADD_NOTIFICATIONS':
			return { ...state, unreadCount: state.unreadCount + action.count, lastNotificationId: action.maxId };
		case 'MARK_READ':
			return {
				...state,
				unreadCount: 0,
				lastNotificationId: action.maxId > state.lastNotificationId ? action.maxId : state.lastNotificationId,
			};
		default:
			return state;
	}
}

export default function NotificationBell({ onPress, onRequestSync }) {
	const { selectedLeague } = useLeague();
	const [state, dispatch] = useReducer(reducer, initialState);
	const { unreadCount, lastNotificationId, ready } = state;

	// Load persisted lastNotificationId on mount
	useEffect(() => {
		AsyncStorage.getItem(STORAGE_KEY)
			.then(val => dispatch({ type: 'INIT', id: val ? parseInt(val, 10) : 0 }))
			.catch(() => dispatch({ type: 'INIT', id: 0 }));
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
						const maxId = Math.max(...newNotifications.map(n => n.id));
						currentLastId = maxId;
						dispatch({ type: 'ADD_NOTIFICATIONS', count: newNotifications.length, maxId });
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
		dispatch({ type: 'MARK_READ', maxId: typeof maxId === 'number' ? maxId : 0 });
	}, []);

	const handlePress = () => {
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
	bellContainer: { position: 'relative', padding: 8 },
	bellIcon: { fontSize: 24 },
	badge: {
		position: 'absolute', top: 4, right: 4,
		backgroundColor: colors.danger, borderRadius: radius.pill,
		minWidth: 20, height: 20, justifyContent: 'center', alignItems: 'center', paddingHorizontal: 4,
		borderWidth: 1.5, borderColor: colors.bgCard,
	},
	badgeText: { color: colors.textInverse, fontSize: fontSize.xs, fontWeight: fontWeight.bold },
});
