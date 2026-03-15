import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, ActivityIndicator, StyleSheet, KeyboardAvoidingView, Platform, ScrollView, Dimensions, Image } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import authService from '../../../services/authService';
import { colors, fontSize, fontWeight, radius, spacing, shadow } from '../../../utils/theme';
import logo from '../../../assets/header/Logo.png';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

export default function Login({ onLoggedIn, onSwitchToRegister }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [errors, setErrors] = useState({ username: '', password: '' });

  const handleSubmit = async () => {
    setError('');
    setErrors({ username: '', password: '' });
    const localErrors = {};
    if (!username) localErrors.username = 'El usuario es obligatorio';
    if (!password) localErrors.password = 'La contraseña es obligatoria';
    if (Object.keys(localErrors).length) {
      setErrors(prev => ({ ...prev, ...localErrors }));
      return;
    }
    setLoading(true);
    try {
      await authService.login({ username, password });
      if (typeof onLoggedIn === 'function') onLoggedIn();
      setUsername('');
      setPassword('');
    } catch (e) {
      let msg = (e?.message || 'Error').trim();
      try {
        const parsed = JSON.parse(msg);
        msg = parsed?.message || msg;
      } catch {}
      if (/bad credentials/i.test(msg)) {
        setErrors(prev => ({ ...prev, password: 'Usuario o contraseña incorrectos' }));
      } else {
        setError(msg.replace(/\s+/g, ' '));
      }
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
            <Text style={styles.appTagline}>Tu liga de fantasy fútbol</Text>
          </View>

          {/* Card */}
          <View style={styles.card}>
            <Text style={styles.title}>Iniciar sesión</Text>

            <View style={styles.field}>
              <Text style={styles.label}>Usuario</Text>
              <TextInput
                value={username}
                onChangeText={setUsername}
                style={[styles.input, errors.username ? styles.inputError : null]}
                autoCapitalize="none"
                placeholderTextColor={colors.textMuted}
              />
              {errors.username ? <Text style={styles.errorText}>{errors.username}</Text> : null}
            </View>

            <View style={styles.field}>
              <Text style={styles.label}>Contraseña</Text>
              <TextInput
                value={password}
                onChangeText={setPassword}
                style={[styles.input, errors.password ? styles.inputError : null]}
                secureTextEntry
                placeholderTextColor={colors.textMuted}
              />
              {errors.password ? <Text style={styles.errorText}>{errors.password}</Text> : null}
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
                <Text style={styles.buttonText}>Entrar</Text>
              )}
            </TouchableOpacity>

            <TouchableOpacity style={styles.linkButton} onPress={onSwitchToRegister}>
              <Text style={styles.linkText}>¿No tienes cuenta? <Text style={styles.linkEmphasis}>Regístrate</Text></Text>
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
    marginBottom: 32,
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
    marginBottom: 24,
  },
  field: {
    marginBottom: 16,
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
