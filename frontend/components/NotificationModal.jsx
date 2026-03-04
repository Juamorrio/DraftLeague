import React, { useState, useEffect } from 'react';
import {
	Modal,
	View,
	Text,
	StyleSheet,
	TouchableOpacity,
	ScrollView,
	ActivityIndicator,
	Alert,
} from 'react-native';
import authService, { authenticatedFetch } from '../services/authService';
import { useLeague } from '../context/LeagueContext';
import { acceptOffer, rejectOffer } from '../services/tradeOfferService';

export default function NotificationModal({ visible, onClose }) {
	const { selectedLeague } = useLeague();
	const [notifications, setNotifications] = useState([]);
	const [loading, setLoading] = useState(false);
	const [maxSeenId, setMaxSeenId] = useState(0);
	const [currentUserId, setCurrentUserId] = useState(null);
	const [actionLoading, setActionLoading] = useState({});

	useEffect(() => {
		if (visible && selectedLeague?.id) {
			loadNotifications();
			authService.getCurrentUser().then(u => {
				if (u?.id) setCurrentUserId(u.id);
			}).catch(() => {});
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
				if (data.length > 0) {
					const max = Math.max(...data.map(n => n.id));
					setMaxSeenId(max);
				}
			}
		} catch (e) {
			console.error('Error loading notifications:', e);
		} finally {
			setLoading(false);
		}
	};

	const handleClose = () => {
		if (onClose) onClose(maxSeenId);
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

				case 'TRADE_OFFER': {
					const isSeller = currentUserId && String(payload.sellerId) === String(currentUserId);
					const offerId = payload.offerId;
					const isActing = actionLoading[offerId];
					return (
						<View key={notification.id} style={[styles.notificationCard, { borderLeftColor: '#7c3aed' }]}>
							<View style={styles.iconContainer}>
								<Text style={styles.icon}>🤝</Text>
							</View>
							<View style={styles.contentContainer}>
								<Text style={styles.title}>Oferta de traspaso</Text>
								<Text style={styles.description}>
									<Text style={styles.bold}>{payload.buyerUsername}</Text> ofrece{' '}
									<Text style={styles.bold}>€{Number(payload.price).toLocaleString('es-ES')}</Text> por{' '}
									<Text style={styles.bold}>{payload.playerName}</Text>
								</Text>
								<Text style={styles.date}>
									{new Date(notification.createdAt).toLocaleString('es-ES')}
								</Text>
								{isSeller && (
									<View style={styles.actionRow}>
										<TouchableOpacity
											style={[styles.actionBtn, styles.acceptBtn, isActing && styles.actionBtnDisabled]}
											disabled={!!isActing}
											onPress={async () => {
												setActionLoading(prev => ({ ...prev, [offerId]: 'accept' }));
												try {
													await acceptOffer(offerId);
													Alert.alert('¡Oferta aceptada!', `Has transferido a ${payload.playerName} por €${Number(payload.price).toLocaleString('es-ES')}.`);
													loadNotifications();
												} catch (e) {
													Alert.alert('Error', e?.message || 'No se pudo aceptar');
												} finally {
													setActionLoading(prev => { const n = {...prev}; delete n[offerId]; return n; });
												}
											}}
										>
											<Text style={styles.actionBtnText}>{isActing === 'accept' ? '...' : 'Aceptar'}</Text>
										</TouchableOpacity>
										<TouchableOpacity
											style={[styles.actionBtn, styles.rejectBtn, isActing && styles.actionBtnDisabled]}
											disabled={!!isActing}
											onPress={async () => {
												setActionLoading(prev => ({ ...prev, [offerId]: 'reject' }));
												try {
													await rejectOffer(offerId);
													Alert.alert('Oferta rechazada', `Has rechazado la oferta por ${payload.playerName}.`);
													loadNotifications();
												} catch (e) {
													Alert.alert('Error', e?.message || 'No se pudo rechazar');
												} finally {
													setActionLoading(prev => { const n = {...prev}; delete n[offerId]; return n; });
												}
											}}
										>
											<Text style={styles.actionBtnText}>{isActing === 'reject' ? '...' : 'Rechazar'}</Text>
										</TouchableOpacity>
									</View>
								)}
							</View>
						</View>
					);
				}

				case 'TRADE_ACCEPTED':
					return (
						<View key={notification.id} style={[styles.notificationCard, { borderLeftColor: '#16a34a' }]}>
							<View style={styles.iconContainer}>
								<Text style={styles.icon}>✅</Text>
							</View>
							<View style={styles.contentContainer}>
								<Text style={styles.title}>Oferta aceptada</Text>
								<Text style={styles.description}>
									<Text style={styles.bold}>{payload.sellerUsername}</Text> aceptó la oferta de{' '}
									<Text style={styles.bold}>{payload.buyerUsername}</Text> por{' '}
									<Text style={styles.bold}>{payload.playerName}</Text>
								</Text>
								<Text style={styles.price}>€{Number(payload.price).toLocaleString('es-ES')}</Text>
								<Text style={styles.date}>
									{new Date(notification.createdAt).toLocaleString('es-ES')}
								</Text>
							</View>
						</View>
					);

				case 'TRADE_REJECTED':
					return (
						<View key={notification.id} style={[styles.notificationCard, { borderLeftColor: '#dc2626' }]}>
							<View style={styles.iconContainer}>
								<Text style={styles.icon}>❌</Text>
							</View>
							<View style={styles.contentContainer}>
								<Text style={styles.title}>Oferta rechazada</Text>
								<Text style={styles.description}>
									<Text style={styles.bold}>{payload.sellerUsername}</Text> rechazó la oferta de{' '}
									<Text style={styles.bold}>{payload.buyerUsername}</Text> por{' '}
									<Text style={styles.bold}>{payload.playerName}</Text>
								</Text>
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
			onRequestClose={handleClose}
		>
			<View style={styles.overlay}>
				<View style={styles.modalContainer}>
					<View style={styles.header}>
						<Text style={styles.headerTitle}>Notificaciones</Text>
						<TouchableOpacity onPress={handleClose} style={styles.closeButton}>
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
	actionRow: {
		flexDirection: 'row',
		gap: 8,
		marginTop: 8,
	},
	actionBtn: {
		paddingVertical: 6,
		paddingHorizontal: 14,
		borderRadius: 8,
	},
	acceptBtn: {
		backgroundColor: '#16a34a',
	},
	rejectBtn: {
		backgroundColor: '#dc2626',
	},
	actionBtnDisabled: {
		opacity: 0.5,
	},
	actionBtnText: {
		color: '#fff',
		fontWeight: '700',
		fontSize: 13,
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
