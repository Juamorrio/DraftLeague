import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, ActivityIndicator, StyleSheet, Alert } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import authService from '../../../services/authService';

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
    <View style={styles.screenWrap}>
      <LinearGradient
        colors={['#197319', '#013055']}
        start={{ x: 0, y: 0 }}
        end={{ x: 0, y: 1 }}
        style={styles.card}
      >
        <Text style={styles.title}>Iniciar sesión</Text>

        <View style={styles.field}>
          <Text style={styles.label}>Nombre de usuario</Text>
          <TextInput
            value={username}
            onChangeText={setUsername}
            style={[styles.input, errors.username ? styles.inputError : null]}
            autoCapitalize="none"
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
          />
          {errors.password ? <Text style={styles.errorText}>{errors.password}</Text> : null}
        </View>

        {error ? <Text style={styles.error}>{error}</Text> : null}

        <TouchableOpacity style={styles.button} onPress={handleSubmit} disabled={loading}>
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.buttonText}>Entrar</Text>
          )}
        </TouchableOpacity>

        <TouchableOpacity style={styles.linkButton} onPress={onSwitchToRegister}>
          <Text style={styles.linkText}>¿No tienes cuenta? Regístrate</Text>
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
