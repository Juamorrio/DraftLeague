import React, { useWindowDimensions } from 'react';
import { View, Text, StyleSheet, Platform } from 'react-native';
import { LineChart, BarChart } from 'react-native-chart-kit';
import { colors, fontSize, fontWeight, radius, shadow, spacing } from '../utils/theme';

// react-native-chart-kit requires opacity-based rgba callbacks, not hex values.
// These RGB triplets are extracted from theme tokens and must stay in sync if the palette changes.
const PRIMARY_RGB = '22, 163, 74';    // colors.primary  #16A34A
const TEXT_SEC_RGB = '71, 85, 105';   // colors.textSecondary  #475569

const webSafeProps = Platform.OS === 'web' ? {
	onStartShouldSetResponder: undefined,
	onResponderGrant: undefined,
	onResponderMove: undefined,
	onResponderRelease: undefined,
	onResponderTerminate: undefined,
	onResponderTerminationRequest: undefined,
} : {};

export function LineChartComponent({ data, title }) {
	const { width: screenWidth } = useWindowDimensions();
	const chartConfig = {
		backgroundColor: colors.bgCard,
		backgroundGradientFrom: colors.bgCard,
		backgroundGradientTo: colors.bgCard,
		decimalPlaces: 1,
		color: (opacity = 1) => `rgba(${PRIMARY_RGB}, ${opacity})`,
		labelColor: (opacity = 1) => `rgba(${TEXT_SEC_RGB}, ${opacity})`,
		style: { borderRadius: radius.lg },
		propsForDots: {
			r: '4',
			strokeWidth: '2',
			stroke: colors.primaryDark,
		},
		propsForLabels: {
			transformOrigin: 'center',
		},
	};

	return (
		<View style={styles.chartContainer}>
			<Text style={styles.chartTitle}>{title}</Text>
			<LineChart
				data={data}
				width={screenWidth - 64}
				height={220}
				chartConfig={chartConfig}
				style={styles.chart}
				onDataPointClick={() => {}}
				{...webSafeProps}
			/>
		</View>
	);
}

export function BarChartComponent({ data, title }) {
	const { width: screenWidth } = useWindowDimensions();
	const chartConfig = {
		backgroundColor: colors.bgCard,
		backgroundGradientFrom: colors.bgCard,
		backgroundGradientTo: colors.bgCard,
		decimalPlaces: 1,
		color: (opacity = 1) => `rgba(${PRIMARY_RGB}, ${opacity})`,
		labelColor: (opacity = 1) => `rgba(${TEXT_SEC_RGB}, ${opacity})`,
		style: { borderRadius: radius.lg },
		propsForLabels: {
			transformOrigin: 'center',
		},
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
		backgroundColor: colors.bgCard,
		borderRadius: radius.lg,
		padding: spacing.lg,
		marginBottom: spacing.lg,
		...shadow.sm,
	},
	chartTitle: {
		fontSize: fontSize.md,
		fontWeight: fontWeight.bold,
		color: colors.primaryDark,
		marginBottom: spacing.md,
	},
	chart: {
		marginVertical: spacing.sm,
		borderRadius: radius.lg,
	},
});
