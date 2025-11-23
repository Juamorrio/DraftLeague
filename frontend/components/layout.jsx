import React, { useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons, MaterialIcons, FontAwesome5 } from '@expo/vector-icons';
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
import { Image } from 'react-native';
import { useLeague } from '../context/LeagueContext';
export default function Layout({ children, onNavigate, activeKey }) {
	const { selectedLeague } = useLeague();
	const baseItems = [
		{ key: 'home', label: 'Home', type: 'image', activeSrc: home2, inactiveSrc: home },
		{ key: 'league', label: 'Ligas', type: 'image', activeSrc: trophy2, inactiveSrc: trophy },
	];
	const extraItems = [
		{ key: 'team', label: 'Equipo', type: 'image', activeSrc: footballPidge2, inactiveSrc: footballPidge },
		{ key: 'market', label: 'Mercado', type: 'image', activeSrc: cart2, inactiveSrc: cart },
		{ key: 'robot', label: 'IA', type: 'image', activeSrc: robot2, inactiveSrc: robot },
	];
	const navItems = selectedLeague ? [...baseItems.slice(0,1), ...extraItems, baseItems[1]] : baseItems; 

	useEffect(() => {
		if (!selectedLeague && !['home', 'league'].includes(activeKey)) {
			onNavigate && onNavigate('home');
		}
	}, [selectedLeague, activeKey, onNavigate]);

	return (
		<View style={styles.container}>
			<View style={styles.content}>{children}</View>

			<View style={styles.footerWrap} pointerEvents="box-none">
				<View style={styles.footer}>
					{navItems.map(item => {
						const active = activeKey === item.key;
						return (
							<TouchableOpacity
								key={item.key}
								style={[styles.navItem, active && styles.navItemActive]}
								activeOpacity={0.8}
								onPress={() => onNavigate && onNavigate(item.key)}
								accessibilityRole="button"
								accessibilityState={{ selected: active }}
							>
								{item.type === 'vector' ? (
									(() => {
										const Icon = item.lib;
										const color = item.key === 'home' ? '#000' : (active ? '#0ea5a4' : '#cbd5e1');
										return <Icon name={item.name} size={item.size} color={color} />;
									})()
								) : (
									<Image
										source={active ? item.activeSrc : item.inactiveSrc}
										style={{ width: 40, height: 40, borderRadius: 4 }}
									/>
								)}
								<Text style={[styles.label, active && styles.labelActive]}>{item.label}</Text>
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
		marginBottom: 20,
		flex: 1,
		backgroundColor: '#fff',
	},
	content: {
		flex: 1,
	},
	footerWrap: {
		padding: 12,
		backgroundColor: 'transparent',
	},
	footer: {
		height: 72,
		backgroundColor: '#156215ff',
		borderRadius: 18,
		flexDirection: 'row',
		alignItems: 'center',
		justifyContent: 'space-around',
		paddingHorizontal: 12,
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 6 },
		shadowOpacity: 0.12,
		shadowRadius: 12,
		elevation: 8,
	},
	navItem: {
		alignItems: 'center',
		justifyContent: 'center',
		paddingHorizontal: 8,
		paddingVertical: 6,
		minWidth: 56,
		borderRadius: 12,
	},
	navItemActive: {
		backgroundColor: 'rgba(14,165,164,0.08)',
	},
	label: {
		color: '#94a3b8',
		fontSize: 11,
		marginTop: 4,
		opacity: 0.9,
	},
	labelActive: {
		color: '#ffffffff',
		fontWeight: '600',
	},
});

