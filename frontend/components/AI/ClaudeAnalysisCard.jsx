import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { colors, fontSize, fontWeight, spacing, radius } from '../../utils/theme';

/**
 * Displays the Claude AI narrative analysis for a player prediction.
 * Returns null when no analysis is available, so it is safe to render unconditionally.
 *
 * Props:
 *  - analysis   {string}  Narrative text returned by ClaudeAnalysisService
 *  - modelSource {string} e.g. "XGBOOST+CLAUDE" — shown as a small badge
 */
export function ClaudeAnalysisCard({ analysis, modelSource }) {
	if (!analysis) return null;

	return (
		<View style={s.card}>
			<Text style={s.label}>ANÁLISIS IA</Text>
			<Text style={s.text}>{analysis}</Text>
			{modelSource ? <Text style={s.badge}>{modelSource}</Text> : null}
		</View>
	);
}

const s = StyleSheet.create({
	card: {
		marginTop: spacing.sm,
		padding: spacing.sm,
		backgroundColor: colors.bgSubtle,
		borderRadius: radius.md,
		borderWidth: 1,
		borderColor: colors.primaryMuted,
	},
	label: {
		fontSize: 9,
		color: colors.primary,
		fontWeight: fontWeight.extrabold,
		letterSpacing: 0.8,
		marginBottom: 4,
	},
	text: {
		fontSize: fontSize.xs,
		color: colors.textSecondary,
		lineHeight: 16,
		fontWeight: fontWeight.medium,
	},
	badge: {
		fontSize: 9,
		color: colors.textMuted,
		marginTop: 4,
		fontStyle: 'italic',
	},
});
