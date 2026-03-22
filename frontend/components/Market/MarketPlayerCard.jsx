import React, { useRef, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Animated } from 'react-native';
import Player from '../player';
import {
	colors, fontSize, fontWeight, fontFamily,
	radius, spacing, shadow, marketPositionColors,
} from '../../utils/theme';

function formatCountdown(endTime) {
	const diff = new Date(endTime) - new Date();
	if (diff <= 0) return 'Finalizada';
	const h = Math.floor(diff / 3600000);
	const m = Math.floor((diff % 3600000) / 60000);
	if (h > 24) return `${Math.floor(h / 24)}d ${h % 24}h`;
	if (h > 0) return `${h}h ${m}m`;
	return `${m}m`;
}

/**
 * Single player card in the auction market list.
 *
 * Props:
 *   item         – AuctionItem object from the API
 *   onViewStats  – (player) => void
 *   onBid        – (item) => void
 *   onCancelBid  – (marketPlayerId) => void
 *   formatNumber – (num) => string
 */
export default function MarketPlayerCard({ item, onViewStats, onBid, onCancelBid, formatNumber }) {
	const pos = item.player.position;
	const posColor = marketPositionColors[pos] ?? marketPositionColors.COACH;
	const countdown = formatCountdown(item.auctionEndTime);
	const isExpired = countdown === 'Finalizada';

	return (
		<View style={styles.card}>
			{/* Player info — tappable → PlayerStats */}
			<TouchableOpacity
				style={styles.cardTop}
				activeOpacity={0.75}
				onPress={() => onViewStats(item.player)}
			>
				<Player
					name={item.player.fullName ?? item.player.name}
					avatar={item.player.avatarUrl ? { uri: item.player.avatarUrl } : null}
					teamId={item.player.teamId}
					size={54}
					points={item.player.totalPoints ?? 0}
				/>
				<View style={styles.playerInfo}>
					<View style={styles.nameRow}>
						<Text style={styles.playerName} numberOfLines={1}>
							{item.player.fullName ?? item.player.name}
						</Text>
						<View style={[styles.posBadge, { backgroundColor: posColor.bg, borderColor: posColor.border }]}>
							<Text style={[styles.posBadgeText, { color: posColor.text }]}>{pos}</Text>
						</View>
					</View>
					<View style={styles.playerMetaRow}>
						<Text style={styles.playerMeta}>
							€{item.player.marketValue.toLocaleString('es-ES')} valor de mercado
						</Text>
					</View>
					<View style={[styles.countdownBadge, isExpired && styles.countdownExpired]}>
						<Text style={[styles.countdownText, isExpired && styles.countdownTextExpired]}>
							{isExpired ? 'Subasta finalizada' : `⏱ Cierra en ${countdown}`}
						</Text>
					</View>
				</View>
				<Text style={styles.statsHint}>Ver stats →</Text>
			</TouchableOpacity>

			{/* Bid row */}
			<View style={styles.cardBottom}>
				{item.hasBid ? (
					<View style={styles.activeBidRow}>
						<View style={styles.activeBidInfo}>
							<Text style={styles.activeBidLabel}>Tu puja activa</Text>
							<Text style={styles.activeBidAmount}>€{formatNumber(item.myBid)}</Text>
						</View>
						<TouchableOpacity style={styles.cancelBtn} onPress={() => onCancelBid(item.id)}>
							<Text style={styles.cancelBtnText}>Cancelar puja</Text>
						</TouchableOpacity>
					</View>
				) : (
					<TouchableOpacity
						style={[styles.bidBtn, isExpired && styles.bidBtnDisabled]}
						onPress={() => !isExpired && onBid(item)}
						disabled={isExpired}
						activeOpacity={0.8}
					>
						<Text style={styles.bidBtnText}>
							{isExpired ? 'Subasta cerrada' : 'Hacer puja'}
						</Text>
					</TouchableOpacity>
				)}
			</View>
		</View>
	);
}

/**
 * Animated skeleton placeholder shown while the market list is loading.
 * Uses React Native's built-in Animated API — no extra dependencies needed.
 */
export function MarketPlayerCardSkeleton() {
	const shimmer = useRef(new Animated.Value(0)).current;

	useEffect(() => {
		const anim = Animated.loop(
			Animated.sequence([
				Animated.timing(shimmer, { toValue: 1, duration: 700, useNativeDriver: true }),
				Animated.timing(shimmer, { toValue: 0, duration: 700, useNativeDriver: true }),
			])
		);
		anim.start();
		return () => anim.stop();
	}, [shimmer]);

	const opacity = shimmer.interpolate({ inputRange: [0, 1], outputRange: [0.45, 0.9] });

	return (
		<View style={styles.card}>
			<View style={[styles.cardTop, { gap: spacing.sm }]}>
				{/* Avatar placeholder */}
				<Animated.View style={[sk.circle, { opacity }]} />
				<View style={{ flex: 1, gap: 8 }}>
					<View style={{ flexDirection: 'row', gap: 8, alignItems: 'center' }}>
						<Animated.View style={[sk.line, { width: '55%', opacity }]} />
						<Animated.View style={[sk.pill, { width: 34, opacity }]} />
					</View>
					<Animated.View style={[sk.line, { width: '70%', opacity }]} />
					<Animated.View style={[sk.pill, { width: 110, opacity }]} />
				</View>
			</View>
			<View style={[styles.cardBottom, { paddingVertical: 14 }]}>
				<Animated.View style={[sk.button, { opacity }]} />
			</View>
		</View>
	);
}

const sk = StyleSheet.create({
	circle: { width: 54, height: 54, borderRadius: 27, backgroundColor: colors.bgSubtle },
	line:   { height: 14, borderRadius: radius.sm,   backgroundColor: colors.bgSubtle },
	pill:   { height: 20, borderRadius: radius.pill,  backgroundColor: colors.bgSubtle },
	button: { height: 38, borderRadius: radius.pill,  backgroundColor: colors.bgSubtle },
});

const styles = StyleSheet.create({
	card: {
		backgroundColor: colors.bgCard,
		borderRadius: radius.xl,
		borderWidth: 1,
		borderColor: colors.border,
		overflow: 'hidden',
		...shadow.sm,
	},
	cardTop: {
		flexDirection: 'row',
		alignItems: 'center',
		padding: spacing.md,
		gap: spacing.sm,
	},
	playerInfo: { flex: 1, gap: 4 },
	nameRow: {
		flexDirection: 'row',
		alignItems: 'center',
		gap: spacing.sm,
		flexWrap: 'wrap',
	},
	playerName: {
		fontFamily: fontFamily.bodyBold,
		fontSize: fontSize.md,
		color: colors.textPrimary,
		flexShrink: 1,
	},
	posBadge: {
		paddingHorizontal: 8,
		paddingVertical: 2,
		borderRadius: radius.pill,
		borderWidth: 1,
	},
	posBadgeText: {
		fontFamily: fontFamily.bodyBold,
		fontSize: 10,
		letterSpacing: 0.5,
	},
	playerMetaRow: { flexDirection: 'row', alignItems: 'center', gap: 8 },
	playerMeta: {
		fontFamily: fontFamily.body,
		fontSize: fontSize.xs,
		color: colors.textSecondary,
	},
	countdownBadge: {
		alignSelf: 'flex-start',
		backgroundColor: colors.bgSubtle,
		paddingHorizontal: 8,
		paddingVertical: 3,
		borderRadius: radius.pill,
		borderWidth: 1,
		borderColor: colors.border,
	},
	countdownExpired: { backgroundColor: colors.dangerBg, borderColor: marketPositionColors.DEL.border },
	countdownText: { fontSize: 10, fontWeight: fontWeight.semibold, color: colors.textMuted },
	countdownTextExpired: { color: colors.dangerDark },
	statsHint: {
		fontSize: 10,
		color: colors.primary,
		fontWeight: fontWeight.semibold,
		alignSelf: 'flex-start',
		marginTop: 2,
	},
	cardBottom: {
		borderTopWidth: 1,
		borderTopColor: colors.border,
		backgroundColor: colors.bgSubtle,
		paddingHorizontal: spacing.md,
		paddingVertical: spacing.sm,
	},
	bidBtn: {
		backgroundColor: colors.primary,
		paddingVertical: 10,
		borderRadius: radius.pill,
		alignItems: 'center',
		...shadow.sm,
	},
	bidBtnDisabled: { backgroundColor: colors.textMuted },
	bidBtnText: { color: colors.textInverse, fontWeight: fontWeight.bold, fontSize: fontSize.sm },
	activeBidRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
	activeBidInfo: { gap: 2 },
	activeBidLabel: {
		fontSize: fontSize.xs,
		color: colors.warningDark,
		fontWeight: fontWeight.semibold,
		textTransform: 'uppercase',
		letterSpacing: 0.4,
	},
	activeBidAmount: { fontSize: fontSize.lg, fontWeight: fontWeight.extrabold, color: colors.warningDeep },
	cancelBtn: {
		backgroundColor: colors.danger,
		paddingVertical: 8,
		paddingHorizontal: spacing.md,
		borderRadius: radius.pill,
	},
	cancelBtnText: { color: colors.textInverse, fontWeight: fontWeight.bold, fontSize: fontSize.xs },
});
