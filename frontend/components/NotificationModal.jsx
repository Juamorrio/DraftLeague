import React, { useState, useEffect } from 'react';
import {
	Modal,
	View,
	Text,
	StyleSheet,
	TouchableOpacity,
	ScrollView,
	ActivityIndicator,
} from 'react-native';
import { authenticatedFetch } from '../services/authService';
import { useLeague } from '../context/LeagueContext';

export default function NotificationModal({ visible, onClose }) {
	const { selectedLeague } = useLeague();
	const [notifications, setNotifications] = useState([]);
	const [loading, setLoading] = useState(false);

	useEffect(() => {
		if (visible && selectedLeague?.id) {
			loadNotifications();
		}
	}, [visible, selectedLeague]);

	const loadNotifications = async () => {
		setLoading(true);
		try {
			const res = await authenticatedFetch(
				`/api/v1/notifications/league/${selectedLeague.id}`
			);
			if (res.ok) {
				const data = await res.json();
				setNotifications(data);
			}
		} catch (e) {
			console.error('Error loading notifications:', e);
		} finally {
			setLoading(false);
		}
	};

	const renderNotification = (notification) => {
		try {
			const payload = JSON.parse(notification.payload);
			
			switch (notification.type) {
				case 'CLAUSE':
					return (
						<View key={notification.id} style={styles.notificationCard}>
							<View style={styles.iconContainer}>
								<Text style={styles.icon}>⚡</Text>
							</View>
							<View style={styles.contentContainer}>
								<Text style={styles.title}>¡Clausulazo!</Text>
								<Text style={styles.description}>
									<Text style={styles.bold}>{payload.buyerUsername}</Text> ha activado
									la cláusula de rescisión de{' '}
									<Text style={styles.bold}>{payload.playerName}</Text> del equipo de{' '}
									<Text style={styles.bold}>{payload.sellerUsername}</Text>
								</Text>
								<Text style={styles.price}>Precio: {payload.price.toLocaleString()}€</Text>
								<Text style={styles.date}>
									{new Date(notification.createdAt).toLocaleString('es-ES')}
								</Text>
							</View>
						</View>
					);
				
				case 'SELL':
					return (
						<View key={notification.id} style={styles.notificationCard}>
							<View style={styles.iconContainer}>
								<Text style={styles.icon}>💰</Text>
							</View>
							<View style={styles.contentContainer}>
								<Text style={styles.title}>Venta</Text>
								<Text style={styles.description}>
									Un jugador ha sido vendido
								</Text>
								<Text style={styles.date}>
									{new Date(notification.createdAt).toLocaleString('es-ES')}
								</Text>
							</View>
						</View>
					);
				
				case 'BUY':
					return (
						<View key={notification.id} style={styles.notificationCard}>
							<View style={styles.iconContainer}>
								<Text style={styles.icon}>🛒</Text>
							</View>
							<View style={styles.contentContainer}>
								<Text style={styles.title}>Compra en el Mercado</Text>
								<Text style={styles.description}>
									<Text style={styles.bold}>{payload.buyerUsername}</Text> ha ganado la
									subasta de{' '}
									<Text style={styles.bold}>{payload.playerName}</Text>
								</Text>
								<Text style={styles.price}>Precio: {payload.price.toLocaleString()}€</Text>
								<Text style={styles.date}>
									{new Date(notification.createdAt).toLocaleString('es-ES')}
								</Text>
							</View>
						</View>
					);
				
				default:
					return null;
			}
		} catch (e) {
			console.error('Error rendering notification:', e);
			return null;
		}
	};

	return (
		<Modal
			visible={visible}
			animationType="slide"
			transparent={true}
			onRequestClose={onClose}
		>
			<View style={styles.overlay}>
				<View style={styles.modalContainer}>
					<View style={styles.header}>
						<Text style={styles.headerTitle}>Notificaciones</Text>
						<TouchableOpacity onPress={onClose} style={styles.closeButton}>
							<Text style={styles.closeButtonText}>✕</Text>
						</TouchableOpacity>
					</View>

					{loading ? (
						<View style={styles.loadingContainer}>
							<ActivityIndicator size="large" color="#007AFF" />
						</View>
					) : notifications.length === 0 ? (
						<View style={styles.emptyContainer}>
							<Text style={styles.emptyText}>No hay notificaciones</Text>
						</View>
					) : (
						<ScrollView style={styles.scrollView}>
							{notifications.map(renderNotification)}
						</ScrollView>
					)}
				</View>
			</View>
		</Modal>
	);
}

const styles = StyleSheet.create({
	overlay: {
		flex: 1,
		backgroundColor: 'rgba(0, 0, 0, 0.5)',
		justifyContent: 'flex-end',
	},
	modalContainer: {
		backgroundColor: 'white',
		borderTopLeftRadius: 20,
		borderTopRightRadius: 20,
		maxHeight: '80%',
		paddingBottom: 20,
	},
	header: {
		flexDirection: 'row',
		justifyContent: 'space-between',
		alignItems: 'center',
		padding: 20,
		borderBottomWidth: 1,
		borderBottomColor: '#e0e0e0',
	},
	headerTitle: {
		fontSize: 20,
		fontWeight: 'bold',
		color: '#333',
	},
	closeButton: {
		padding: 5,
	},
	closeButtonText: {
		fontSize: 24,
		color: '#666',
	},
	scrollView: {
		padding: 15,
	},
	notificationCard: {
		flexDirection: 'row',
		backgroundColor: '#f8f9fa',
		borderRadius: 12,
		padding: 15,
		marginBottom: 12,
		borderLeftWidth: 4,
		borderLeftColor: '#007AFF',
	},
	iconContainer: {
		marginRight: 12,
		justifyContent: 'center',
	},
	icon: {
		fontSize: 32,
	},
	contentContainer: {
		flex: 1,
	},
	title: {
		fontSize: 16,
		fontWeight: 'bold',
		color: '#333',
		marginBottom: 4,
	},
	description: {
		fontSize: 14,
		color: '#666',
		marginBottom: 4,
		lineHeight: 20,
	},
	bold: {
		fontWeight: 'bold',
		color: '#333',
	},
	price: {
		fontSize: 14,
		color: '#007AFF',
		fontWeight: '600',
		marginBottom: 4,
	},
	date: {
		fontSize: 12,
		color: '#999',
	},
	loadingContainer: {
		padding: 40,
		alignItems: 'center',
		justifyContent: 'center',
	},
	emptyContainer: {
		padding: 40,
		alignItems: 'center',
		justifyContent: 'center',
	},
	emptyText: {
		fontSize: 16,
		color: '#999',
	},
});
