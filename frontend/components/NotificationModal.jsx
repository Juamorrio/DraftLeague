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
import { authenticatedFetch, getCurrentUser } from '../services/authService';
import { acceptOffer, rejectOffer, cancelOffer } from '../services/tradeOfferService';
import { useLeague } from '../context/LeagueContext';
import { colors, fontSize, fontWeight, radius, spacing, shadow } from '../utils/theme';

export default function NotificationModal({ visible, onClose }) {
	const { selectedLeague } = useLeague();
	const [notifications, setNotifications] = useState([]);
	const [loading, setLoading] = useState(false);
	const [maxSeenId, setMaxSeenId] = useState(0);
	const [currentUserId, setCurrentUserId] = useState(null);
	const [actionLoading, setActionLoading] = useState({});

	useEffect(() => {
		getCurrentUser().then(user => {
			if (user?.id) setCurrentUserId(Number(user.id));
		}).catch(() => {});
	}, []);

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

	const handleOfferAction = async (offerId, action) => {
		setActionLoading(prev => ({ ...prev, [offerId]: action }));
		try {
			if (action === 'accept') await acceptOffer(offerId);
			else if (action === 'reject') await rejectOffer(offerId);
			else if (action === 'cancel') await cancelOffer(offerId);
			await loadNotifications();
		} catch (e) {
			Alert.alert('Error', e?.message || 'No se pudo completar la acción');
		} finally {
			setActionLoading(prev => { const n = { ...prev }; delete n[offerId]; return n; });
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
									<Text style={styles.bold}>{payload.sellerUsername}</Text> ha vendido a{' '}
									<Text style={styles.bold}>{payload.playerName}</Text>
								</Text>
								{payload.price != null && (
									<Text style={styles.price}>Precio: {Number(payload.price).toLocaleString('es-ES')}€</Text>
								)}
								<Text style={styles.date}>
									{new Date(notification.createdAt).toLocaleString('es-ES')}
								</Text>
							</View>
						</View>
					);

				case 'CESSION':
					return (
						<View key={notification.id} style={[styles.notificationCard, { borderLeftColor: colors.accent }]}>
							<View style={styles.iconContainer}>
								<Text style={styles.icon}>🔄</Text>
							</View>
							<View style={styles.contentContainer}>
								<Text style={styles.title}>Cesión</Text>
								<Text style={styles.description}>
									<Text style={styles.bold}>{payload.playerName}</Text> ha sido cedido por{' '}
									<Text style={styles.bold}>{payload.sellerUsername}</Text> a{' '}
									<Text style={styles.bold}>{payload.buyerUsername}</Text>
								</Text>
								{payload.price != null && (
									<Text style={styles.price}>Precio: {Number(payload.price).toLocaleString('es-ES')}€</Text>
								)}
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
					const offerId = payload.offerId;
					const isSeller = Number(payload.sellerId) === currentUserId;
					const isBuyer = Number(payload.buyerId) === currentUserId;
					const isActing = !!actionLoading[offerId];
					return (
						<View key={notification.id} style={[styles.notificationCard, { borderLeftColor: colors.purple }]}>
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
									<View style={styles.tradeActions}>
										<TouchableOpacity
											style={[styles.tradeBtn, styles.tradeBtnAccept, isActing && styles.tradeBtnDisabled]}
											onPress={() => handleOfferAction(offerId, 'accept')}
											disabled={isActing}
										>
											<Text style={styles.tradeBtnText}>{actionLoading[offerId] === 'accept' ? '...' : 'Aceptar'}</Text>
										</TouchableOpacity>
										<TouchableOpacity
											style={[styles.tradeBtn, styles.tradeBtnReject, isActing && styles.tradeBtnDisabled]}
											onPress={() => handleOfferAction(offerId, 'reject')}
											disabled={isActing}
										>
											<Text style={styles.tradeBtnText}>{actionLoading[offerId] === 'reject' ? '...' : 'Rechazar'}</Text>
										</TouchableOpacity>
									</View>
								)}
								{isBuyer && (
									<View style={styles.tradeActions}>
										<TouchableOpacity
											style={[styles.tradeBtn, styles.tradeBtnCancel, isActing && styles.tradeBtnDisabled]}
											onPress={() => handleOfferAction(offerId, 'cancel')}
											disabled={isActing}
										>
											<Text style={styles.tradeBtnText}>{actionLoading[offerId] === 'cancel' ? '...' : 'Cancelar oferta'}</Text>
										</TouchableOpacity>
									</View>
								)}
							</View>
						</View>
					);
				}

				case 'TRADE_ACCEPTED':
					return (
						<View key={notification.id} style={[styles.notificationCard, { borderLeftColor: colors.primary }]}>
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
						<View key={notification.id} style={[styles.notificationCard, { borderLeftColor: colors.dangerDark }]}>
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
							<ActivityIndicator size="large" color={colors.primary} />
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
	overlay: { flex: 1, backgroundColor: colors.overlay, justifyContent: 'flex-end' },
	modalContainer: {
		backgroundColor: colors.bgCard, borderTopLeftRadius: radius.xl, borderTopRightRadius: radius.xl,
		maxHeight: '80%', paddingBottom: spacing.xl,
	},
	header: {
		flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
		padding: spacing.xl, borderBottomWidth: 1, borderBottomColor: colors.border,
	},
	headerTitle: { fontSize: fontSize.xl, fontWeight: fontWeight.bold, color: colors.textPrimary },
	closeButton: { padding: 5 },
	closeButtonText: { fontSize: fontSize.xl, color: colors.textSecondary },
	scrollView: { padding: spacing.lg },
	notificationCard: {
		flexDirection: 'row', backgroundColor: colors.bgSubtle, borderRadius: radius.lg,
		padding: spacing.lg, marginBottom: spacing.md, borderLeftWidth: 4, borderLeftColor: colors.accent,
	},
	iconContainer: { marginRight: spacing.md, justifyContent: 'center' },
	icon: { fontSize: fontSize['2xl'] },
	contentContainer: { flex: 1 },
	title: { fontSize: fontSize.md, fontWeight: fontWeight.bold, color: colors.textPrimary, marginBottom: 4 },
	description: { fontSize: fontSize.sm, color: colors.textSecondary, marginBottom: 4, lineHeight: 20 },
	bold: { fontWeight: fontWeight.bold, color: colors.textPrimary },
	price: { fontSize: fontSize.sm, color: colors.accent, fontWeight: fontWeight.semibold, marginBottom: 4 },
	date: { fontSize: fontSize.xs, color: colors.textMuted },
	loadingContainer: { padding: 40, alignItems: 'center', justifyContent: 'center' },
	emptyContainer: { padding: 40, alignItems: 'center', justifyContent: 'center' },
	emptyText: { fontSize: fontSize.md, color: colors.textMuted },
	tradeActions: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.sm },
	tradeBtn: { paddingVertical: spacing.xs, paddingHorizontal: spacing.md, borderRadius: radius.sm },
	tradeBtnAccept: { backgroundColor: colors.primary },
	tradeBtnReject: { backgroundColor: colors.danger },
	tradeBtnCancel: { backgroundColor: colors.textMuted },
	tradeBtnDisabled: { opacity: 0.5 },
	tradeBtnText: { color: colors.textInverse, fontSize: fontSize.xs, fontWeight: fontWeight.bold },
});
