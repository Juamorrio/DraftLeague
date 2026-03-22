import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, ActivityIndicator, StyleSheet, Alert, Platform, KeyboardAvoidingView, ScrollView, Dimensions, Image } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import authService from '../../../services/authService';
import { validateRegister } from '../../../services/validation/registerValidation';
import { colors, fontSize, fontWeight, radius, spacing, shadow } from '../../../utils/theme';
import logo from '../../../assets/header/Logo.png';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

export default function Register({ onRegistered, onSwitchToLogin }) {
	const [username, setUsername] = useState('');
	const [displayName, setDisplayName] = useState('');
	const [email, setEmail] = useState('');
	const [password, setPassword] = useState('');
	const [confirmPassword, setConfirmPassword] = useState('');
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState('');
	const [errors, setErrors] = useState({ username: '', displayName: '', email: '', password: '', confirmPassword: '' });

	const handleSubmit = async () => {
		setError('');
		setErrors({ username: '', displayName: '', email: '', password: '', confirmPassword: '' });
		const errs = validateRegister({ username, displayName, email, password, confirmPassword });
		if (Object.keys(errs).length) {
			setErrors(prev => ({ ...prev, ...errs }));
			return;
		}
		setLoading(true);
		try {
			const data = await authService.register({ username, password, email, displayName });
			Alert.alert('Registro correcto', 'Tu cuenta ha sido creada.');
			if (typeof onRegistered === 'function') {
				onRegistered();
			}
			setUsername('');
			setDisplayName('');
			setEmail('');
			setPassword('');
			setConfirmPassword('');
		} catch (e) {
			let msg = (e?.message || 'Error').trim();
			try {
				const parsed = JSON.parse(msg);
				msg = parsed?.message || msg;
			} catch {}
			if (/user(name)?\s*exists|duplic/i.test(msg)) setErrors(prev => ({ ...prev, username: 'El nombre de usuario ya existe' }));
			else if (/email\s*exists|duplic/i.test(msg)) setErrors(prev => ({ ...prev, email: 'El email ya está registrado' }));
			else setError(msg.replace(/\s+/g, ' '));
		} finally {
			setLoading(false);
		}
	};

	return (
		<LinearGradient
			colors={[colors.gradientStart, colors.gradientEnd]}
			start={{ x: 0, y: 0 }}
			end={{ x: 0, y: 1 }}
			style={styles.screen}
		>
			<KeyboardAvoidingView
				style={{ flex: 1 }}
				behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
			>
				<ScrollView
					contentContainerStyle={styles.scrollContent}
					keyboardShouldPersistTaps="handled"
					showsVerticalScrollIndicator={false}
				>
					{/* Logo */}
					<View style={styles.logoWrap}>
						<View style={styles.logoCircle}>
							<Image source={logo} style={styles.logoImage} resizeMode="contain" />
						</View>
						<Text style={styles.appName}>DraftLeague</Text>
						<Text style={styles.appTagline}>Crea tu cuenta gratis</Text>
					</View>

					{/* Card */}
					<View style={styles.card}>
						<Text style={styles.title}>Registrarse</Text>

						<View style={styles.field}>
							<Text style={styles.label}>Usuario</Text>
							<TextInput
								placeholderTextColor={colors.textMuted}
								value={username}
								onChangeText={setUsername}
								style={[styles.input, errors.username ? styles.inputError : null]}
								autoCapitalize="none"
							/>
							{errors.username ? <Text style={styles.errorText}>{errors.username}</Text> : null}
						</View>

						<View style={styles.field}>
							<Text style={styles.label}>Nombre a mostrar</Text>
							<TextInput
								placeholderTextColor={colors.textMuted}
								value={displayName}
								onChangeText={setDisplayName}
								style={[styles.input, errors.displayName ? styles.inputError : null]}
							/>
							{errors.displayName ? <Text style={styles.errorText}>{errors.displayName}</Text> : null}
						</View>

						<View style={styles.field}>
							<Text style={styles.label}>Contraseña</Text>
							<TextInput
								placeholderTextColor={colors.textMuted}
								value={password}
								onChangeText={setPassword}
								style={[styles.input, errors.password ? styles.inputError : null]}
								secureTextEntry
							/>
							{errors.password ? <Text style={styles.errorText}>{errors.password}</Text> : null}
						</View>

						<View style={styles.field}>
							<Text style={styles.label}>Repetir contraseña</Text>
							<TextInput
								placeholderTextColor={colors.textMuted}
								value={confirmPassword}
								onChangeText={setConfirmPassword}
								style={[styles.input, errors.confirmPassword ? styles.inputError : null]}
								secureTextEntry
							/>
							{errors.confirmPassword ? <Text style={styles.errorText}>{errors.confirmPassword}</Text> : null}
						</View>

						<View style={styles.field}>
							<Text style={styles.label}>Email</Text>
							<TextInput
								placeholderTextColor={colors.textMuted}
								value={email}
								onChangeText={setEmail}
								style={[styles.input, errors.email ? styles.inputError : null]}
								autoCapitalize="none"
								keyboardType="email-address"
							/>
							{errors.email ? <Text style={styles.errorText}>{errors.email}</Text> : null}
						</View>

						{error ? (
							<View style={styles.errorBanner}>
								<Text style={styles.errorBannerText}>{error}</Text>
							</View>
						) : null}

						<TouchableOpacity style={styles.button} onPress={handleSubmit} disabled={loading} activeOpacity={0.85}>
							{loading ? (
								<ActivityIndicator color={colors.textInverse} />
							) : (
								<Text style={styles.buttonText}>Crear cuenta</Text>
							)}
						</TouchableOpacity>

						<TouchableOpacity style={styles.linkButton} onPress={onSwitchToLogin}>
							<Text style={styles.linkText}>¿Ya tienes cuenta? <Text style={styles.linkEmphasis}>Inicia sesión</Text></Text>
						</TouchableOpacity>
					</View>
				</ScrollView>
			</KeyboardAvoidingView>
		</LinearGradient>
	);
}

const styles = StyleSheet.create({
	screen: {
		flex: 1,
	},
	scrollContent: {
		flexGrow: 1,
		alignItems: 'center',
		justifyContent: 'center',
		paddingHorizontal: 24,
		paddingVertical: 48,
	},
	logoWrap: {
		alignItems: 'center',
		marginBottom: 28,
	},
	logoCircle: {
		width: 80,
		height: 80,
		borderRadius: 40,
		backgroundColor: 'rgba(255,255,255,0.15)',
		justifyContent: 'center',
		alignItems: 'center',
		borderWidth: 2,
		borderColor: 'rgba(255,255,255,0.3)',
		marginBottom: 12,
	},
	logoImage: {
		width: 52,
		height: 52,
	},
	appName: {
		fontSize: fontSize['2xl'],
		fontWeight: fontWeight.black,
		color: colors.textInverse,
		letterSpacing: 0.5,
	},
	appTagline: {
		fontSize: fontSize.sm,
		color: 'rgba(255,255,255,0.65)',
		marginTop: 4,
		fontWeight: fontWeight.medium,
	},
	card: {
		width: '100%',
		maxWidth: 420,
		backgroundColor: colors.bgCard,
		borderRadius: radius.xl,
		paddingVertical: 32,
		paddingHorizontal: SCREEN_WIDTH < 380 ? 20 : 28,
		...shadow.lg,
	},
	title: {
		fontSize: fontSize.xl,
		fontWeight: fontWeight.bold,
		color: colors.textPrimary,
		marginBottom: 20,
	},
	field: {
		marginBottom: 14,
	},
	label: {
		fontSize: fontSize.sm,
		fontWeight: fontWeight.semibold,
		color: colors.textSecondary,
		marginBottom: 6,
	},
	input: {
		backgroundColor: colors.bgSubtle,
		borderWidth: 1.5,
		borderColor: colors.border,
		borderRadius: radius.md,
		paddingHorizontal: spacing.lg,
		height: 48,
		fontSize: fontSize.md,
		color: colors.textPrimary,
	},
	inputError: {
		borderColor: colors.danger,
		backgroundColor: colors.dangerBg,
	},
	button: {
		marginTop: 8,
		backgroundColor: colors.primary,
		height: 50,
		borderRadius: radius.pill,
		alignItems: 'center',
		justifyContent: 'center',
		...shadow.sm,
	},
	buttonText: {
		color: colors.textInverse,
		fontWeight: fontWeight.bold,
		fontSize: fontSize.md,
		letterSpacing: 0.4,
	},
	errorBanner: {
		backgroundColor: colors.dangerBg,
		borderRadius: radius.sm,
		paddingHorizontal: spacing.md,
		paddingVertical: spacing.sm,
		marginBottom: 12,
		borderLeftWidth: 3,
		borderLeftColor: colors.danger,
	},
	errorBannerText: {
		color: colors.dangerDark,
		fontWeight: fontWeight.semibold,
		fontSize: fontSize.sm,
	},
	errorText: {
		color: colors.danger,
		marginTop: 4,
		fontSize: fontSize.xs,
		fontWeight: fontWeight.semibold,
	},
	linkButton: {
		marginTop: 20,
		alignItems: 'center',
	},
	linkText: {
		color: colors.textMuted,
		fontSize: fontSize.sm,
	},
	linkEmphasis: {
		color: colors.primary,
		fontWeight: fontWeight.semibold,
	},
});
