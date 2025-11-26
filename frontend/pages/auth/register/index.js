import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, ActivityIndicator, StyleSheet, Alert, Platform } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import authService from '../../../services/authService';
import { validateRegister } from '../../../services/validation/registerValidation';



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
		<View style={styles.screenWrap}>
			<LinearGradient
				colors={['#197319', '#013055']}
				start={{ x: 0, y: 0 }}
				end={{ x: 0, y: 1 }}
				style={styles.card}
			>
				<Text style={styles.title}>Registrar</Text>

				<View style={styles.field}>
					<Text style={styles.label}>Nombre de usuario</Text>
					<TextInput
						placeholder=""
						placeholderTextColor="#94a3b8"
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
						placeholder=""
						placeholderTextColor="#94a3b8"
						value={displayName}
						onChangeText={setDisplayName}
						style={[styles.input, errors.displayName ? styles.inputError : null]}
					/>
					{errors.displayName ? <Text style={styles.errorText}>{errors.displayName}</Text> : null}
				</View>

				<View style={styles.field}>
					<Text style={styles.label}>Contraseña</Text>
					<TextInput
						placeholder=""
						placeholderTextColor="#94a3b8"
						value={password}
						onChangeText={setPassword}
						style={[styles.input, errors.password ? styles.inputError : null]}
						secureTextEntry
					/>
					{errors.password ? <Text style={styles.errorText}>{errors.password}</Text> : null}
				</View>

				<View style={styles.field}>
					<Text style={styles.label}>Repetir Contraseña</Text>
					<TextInput
						placeholder=""
						placeholderTextColor="#94a3b8"
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
						placeholder=""
						placeholderTextColor="#94a3b8"
						value={email}
						onChangeText={setEmail}
						style={[styles.input, errors.email ? styles.inputError : null]}
						autoCapitalize="none"
						keyboardType="email-address"
					/>
					{errors.email ? <Text style={styles.errorText}>{errors.email}</Text> : null}
				</View>

				{error ? <Text style={styles.error}>{error}</Text> : null}

				<TouchableOpacity style={styles.button} onPress={handleSubmit} disabled={loading}>
					{loading ? (
						<ActivityIndicator color="#fff" />
					) : (
						<Text style={styles.buttonText}>Registrar</Text>
					)}
				</TouchableOpacity>

				<TouchableOpacity style={styles.linkButton} onPress={onSwitchToLogin}>
					<Text style={styles.linkText}>¿Ya tienes cuenta? Inicia sesión</Text>
				</TouchableOpacity>
			</LinearGradient>
		</View>
	);
}

const styles = StyleSheet.create({
	screenWrap: {
		flex: 1,
		alignItems: 'center',
		justifyContent: 'center',
		backgroundColor: '#fff',
		padding: 16,
	},
	card: {
		width: '90%',
		maxWidth: 420,
		borderRadius: 18,
		paddingVertical: 20,
		paddingHorizontal: 40,
		shadowColor: '#000',
		shadowOffset: { width: 0, height: 8 },
		shadowOpacity: 0.15,
		shadowRadius: 12,
		elevation: 6,
	},
	title: {
		fontSize: 32,
		fontWeight: '800',
		color: '#fff',
		textAlign: 'center',
		marginBottom: 12,
	},
	field: {
		marginVertical: 6,
	},
	label: {
		color: '#e2e8f0',
		marginBottom: 6,
		fontSize: 14,
		fontWeight: '600',
	},
	input: {
		backgroundColor: '#ffffff',
		borderRadius: 22,
		paddingHorizontal: 40,
		height: 42,
		width: 280,
		fontSize: 16,
		color: '#0f172a',
	},
    inputError: {
      borderWidth: 1,
      borderColor: '#ef4444',
    },
	button: {
		marginTop: 16,
		backgroundColor: '#1d4ed8',
		paddingVertical: 12,
		borderRadius: 22,
		alignItems: 'center',
	},
	buttonText: {
		color: '#fff',
		fontWeight: '700',
		fontSize: 18,
	},
	error: {
		color: '#fecaca',
		backgroundColor: 'rgba(239,68,68,0.25)',
		borderRadius: 10,
		paddingHorizontal: 10,
		paddingVertical: 6,
		marginTop: 8,
		fontWeight: '600',
	},
    errorText: {
      color: '#fecaca',
      marginTop: 6,
      fontSize: 12,
      fontWeight: '600',
    },
	linkButton: {
		marginTop: 12,
		alignItems: 'center',
	},
	linkText: {
		color: '#e2e8f0',
		textDecorationLine: 'underline',
	},
});

