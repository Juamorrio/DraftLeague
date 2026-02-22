import React from 'react';
import { View, Text, StyleSheet, Dimensions, Platform } from 'react-native';
import { LineChart, BarChart } from 'react-native-chart-kit';

const screenWidth = Dimensions.get('window').width;

// Helper para limpiar props que causan warnings en React 19 Web
const webSafeProps = Platform.OS === 'web' ? {
	onStartShouldSetResponder: undefined,
	onResponderGrant: undefined,
	onResponderMove: undefined,
	onResponderRelease: undefined,
	onResponderTerminate: undefined,
	onResponderTerminationRequest: undefined,
} : {};

export function LineChartComponent({ data, title }) {
	const chartConfig = {
		backgroundColor: '#ffffff',
		backgroundGradientFrom: '#ffffff',
		backgroundGradientTo: '#ffffff',
		decimalPlaces: 1,
		color: (opacity = 1) => `rgba(26, 92, 58, ${opacity})`,
		labelColor: (opacity = 1) => `rgba(0, 0, 0, ${opacity})`,
		style: { borderRadius: 16 },
		propsForDots: {
			r: '4',
			strokeWidth: '2',
			stroke: '#1a5c3a'
		},
		propsForLabels: {
			transformOrigin: 'center', // Corregido de transform-origin para React 19
		}
	};

	return (
		<View style={styles.chartContainer}>
			<Text style={styles.chartTitle}>{title}</Text>
			<LineChart
				data={data}
				width={screenWidth - 64}
				height={220}
				chartConfig={chartConfig}
				bezier
				style={styles.chart}
				onDataPointClick={() => {}}
				{...webSafeProps}
			/>
		</View>
	);
}

export function BarChartComponent({ data, title }) {
	const chartConfig = {
		backgroundColor: '#ffffff',
		backgroundGradientFrom: '#ffffff',
		backgroundGradientTo: '#ffffff',
		decimalPlaces: 1,
		color: (opacity = 1) => `rgba(26, 92, 58, ${opacity})`,
		labelColor: (opacity = 1) => `rgba(0, 0, 0, ${opacity})`,
		style: { borderRadius: 16 },
		propsForLabels: {
			transformOrigin: 'center',
		}
	};

	return (
		<View style={styles.chartContainer}>
			<Text style={styles.chartTitle}>{title}</Text>
			<BarChart
				data={data}
				width={screenWidth - 64}
				height={220}
				chartConfig={chartConfig}
				style={styles.chart}
				showValuesOnTopOfBars
				fromZero
				{...webSafeProps}
			/>
		</View>
	);
}

const styles = StyleSheet.create({
	chartContainer: {
		backgroundColor: '#fff',
		borderRadius: 12,
		padding: 16,
		marginBottom: 16,
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 2 },
		shadowOpacity: 0.1,
		shadowRadius: 4,
		elevation: 3
	},
	chartTitle: {
		fontSize: 16,
		fontWeight: '800',
		color: '#1a5c3a',
		marginBottom: 12
	},
	chart: {
		marginVertical: 8,
		borderRadius: 16
	}
});
