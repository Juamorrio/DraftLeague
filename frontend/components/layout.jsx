import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons, MaterialIcons, FontAwesome5 } from '@expo/vector-icons';

export default function Layout({ children, onNavigate }) {
	const navItems = [
		{ key: 'home', label: 'Home', icon: <Ionicons name="home" size={24} color="white" /> },
		{ key: 'search', label: 'Buscar', icon: <Ionicons name="search" size={24} color="white" /> },
		{ key: 'new', label: 'Nuevo', icon: <MaterialIcons name="add-circle-outline" size={26} color="white" /> },
		{ key: 'notifications', label: 'Avisos', icon: <Ionicons name="notifications" size={24} color="white" /> },
		{ key: 'settings', label: 'Ajustes', icon: <FontAwesome5 name="cog" size={20} color="white" /> },
	];

	return (
		<View style={styles.container}>
			<View style={styles.content}>{children}</View>

			<View style={styles.footer}>
				{navItems.map(item => (
					<TouchableOpacity
						key={item.key}
						style={styles.navItem}
						activeOpacity={0.7}
						onPress={() => onNavigate && onNavigate(item.key)}
					>
						{item.icon}
						<Text style={styles.label}>{item.label}</Text>
					</TouchableOpacity>
				))}
			</View>
		</View>
	);
}

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: '#fff',
	},
	content: {
		flex: 1,
	},
	footer: {
		height: 70,
		backgroundColor: '#1f2937',
		flexDirection: 'row',
		alignItems: 'center',
		justifyContent: 'space-around',
		paddingBottom: 8,
	},
	navItem: {
		alignItems: 'center',
		justifyContent: 'center',
	},
	label: {
		color: 'white',
		fontSize: 12,
		marginTop: 2,
	},
});

