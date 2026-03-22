import React, { useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import footballPidge from '../assets/layout/football-field.png'
import trophy from '../assets/layout/trophy.png'
import cart from '../assets/layout/cart.png'
import robot from '../assets/layout/robot.png'
import footballPidge2 from '../assets/layout/football-field white.png'
import trophy2 from '../assets/layout/trophy white.png'
import cart2 from '../assets/layout/cart white.png'
import robot2 from '../assets/layout/robot white.png'
import home from '../assets/layout/home.png'
import home2 from '../assets/layout/home white.png'
import { useLeague } from '../context/LeagueContext';
import { colors, fontSize, fontWeight, radius, spacing } from '../utils/theme';

export default function Layout({ children, onNavigate, activeKey, isAdmin }) {
	const { selectedLeague, viewUser, navTarget, setNavTarget } = useLeague();
	const insets = useSafeAreaInsets();
	const baseItems = [
		{ key: 'home', label: 'Home', type: 'image', activeSrc: home2, inactiveSrc: home },
		{ key: 'league', label: 'Ligas', type: 'image', activeSrc: trophy2, inactiveSrc: trophy },
	];
	const extraItems = [
		{ key: 'team', label: 'Equipo', type: 'image', activeSrc: footballPidge2, inactiveSrc: footballPidge },
		{ key: 'market', label: 'Mercado', type: 'image', activeSrc: cart2, inactiveSrc: cart },
		{ key: 'robot', label: 'Analisis', type: 'image', activeSrc: robot2, inactiveSrc: robot },
	];

	const adminItem = { key: 'admin', label: 'Admin', type: 'emoji', emoji: '🛡️' };

	let navItems = selectedLeague ? [...baseItems.slice(0, 1), ...extraItems, baseItems[1]] : baseItems;
	if (isAdmin) {
		navItems = [...navItems, adminItem];
	}

	useEffect(() => {
		if (!selectedLeague && !['home', 'league', 'admin'].includes(activeKey)) {
			onNavigate && onNavigate('home');
		}
		if (!isAdmin && activeKey === 'admin') {
			onNavigate && onNavigate('home');
		}
		if (selectedLeague && viewUser && activeKey !== 'team') {
			onNavigate && onNavigate('team');
		}
		if (navTarget && activeKey !== navTarget) {
			onNavigate && onNavigate(navTarget);
		}
		if (navTarget) {
			setNavTarget(null);
		}
	}, [selectedLeague, viewUser, navTarget, activeKey, onNavigate, isAdmin, setNavTarget]);

	return (
		<View style={styles.container}>
			<View style={styles.content}>{children}</View>

			<View style={[styles.footerWrap, { paddingBottom: Math.max(insets.bottom + 8, 24) }]} pointerEvents="box-none">
				<View style={styles.footerGlow} />
				<View style={styles.footer}>
					{navItems.map(item => {
						const active = activeKey === item.key;
						return (
							<TouchableOpacity
								key={item.key}
								style={[styles.navItem, active && styles.navItemActive]}
								activeOpacity={0.75}
								onPress={() => onNavigate && onNavigate(item.key)}
								accessibilityRole="button"
								accessibilityState={{ selected: active }}
							>
								<View style={[styles.iconWrap, active && styles.iconWrapActive]}>
									{item.type === 'emoji' ? (
									<Text style={styles.navEmoji}>{item.emoji}</Text>
								) : (
									<Image
										source={active ? item.activeSrc : item.inactiveSrc}
										style={[styles.navImage, !active && styles.navImageInactive]}
									/>
								)}
								</View>
								<Text style={[styles.label, active && styles.labelActive]}>
									{item.label}
								</Text>
								{active && <View style={styles.activeDot} />}
							</TouchableOpacity>
						);
					})}
				</View>
			</View>
		</View>
	);
}


const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: colors.bgApp,
	},
	content: {
		flex: 1,
	},
	footerWrap: {
		paddingHorizontal: spacing.lg,
		paddingTop: spacing.sm,
		backgroundColor: 'transparent',
		position: 'relative',
	},
	footer: {
		height: 78,
		backgroundColor: colors.primaryLight,
		borderRadius: radius.xl,
		flexDirection: 'row',
		alignItems: 'center',
		justifyContent: 'space-around',
		paddingHorizontal: spacing.sm,
		shadowColor: colors.primaryDeep,
		shadowOffset: { width: 0, height: 10 },
		shadowOpacity: 0.18,
		shadowRadius: 18,
		elevation: 16,
		borderWidth: 1,
		borderColor: 'rgba(20,83,45,0.15)',
	},
	footerGlow: {
		position: 'absolute',
		top: 0,
		left: 36,
		right: 36,
		height: 20,
		borderRadius: radius.pill,
		backgroundColor: 'rgba(34,197,94,0.18)',
	},
	navItem: {
		alignItems: 'center',
		justifyContent: 'center',
		paddingHorizontal: spacing.md,
		paddingVertical: spacing.sm / 2,
		minWidth: 64,
		borderRadius: 18,
	},
	navItemActive: {
		backgroundColor: 'rgba(20,83,45,0.12)',
	},
	iconWrap: {
		width: 46,
		height: 46,
		alignItems: 'center',
		justifyContent: 'center',
		borderRadius: radius.md,
	},
	iconWrapActive: {
		backgroundColor: 'rgba(20,83,45,0.14)',
	},
	navImage: {
		width: 32,
		height: 32,
		borderRadius: 6,
	},
	navEmoji: {
		fontSize: fontSize.xl,
	},
	navImageInactive: {
		opacity: 0.65,
	},
	label: {
		color: 'rgba(20,83,45,0.65)',
		fontSize: fontSize.xs,
		marginTop: 3,
		fontWeight: fontWeight.semibold,
		letterSpacing: 0.3,
	},
	labelActive: {
		color: colors.primaryDeep,
		fontWeight: fontWeight.extrabold,
	},
	activeDot: {
		marginTop: spacing.xs,
		width: 6,
		height: 6,
		borderRadius: 3,
		backgroundColor: colors.primaryDeep,
	},
});
