import AsyncStorage from '@react-native-async-storage/async-storage';
import Constants from 'expo-constants';
import { Platform } from 'react-native';


const API_OVERRIDE_KEY = 'auth.apiBaseOverride';

function deriveHostFromExpo() {
  const hostUri = Constants.expoConfig?.hostUri || Constants.manifest2?.extra?.expoClient?.hostUri;
  if (!hostUri) return null;
  const host = hostUri.split(':')[0];
  if (!host) return null;
  return `http://${host}:8080`;
}

async function getBaseUrl() {
  const override = await AsyncStorage.getItem(API_OVERRIDE_KEY);
  if (override) return override;
  const envBase = Constants.expoConfig?.extra?.EXPO_PUBLIC_API_BASE || process.env.EXPO_PUBLIC_API_BASE;
  if (envBase) return envBase;
  // Web: use current host to avoid 10.0.2.2 issues in browser
  if (Platform.OS === 'web') {
    try {
      const host = typeof window !== 'undefined' ? window.location.hostname : 'localhost';
      return `http://${host}:8080`;
    } catch {
      return 'http://localhost:8080';
    }
  }
  // Try to derive from Expo host (works for real devices on LAN)
  const derived = deriveHostFromExpo();
  if (derived) {
    if (Platform.OS === 'android' && (derived.includes('localhost') || derived.includes('127.0.0.1'))) {
      // Android emulator can't reach host via localhost
      return 'http://10.0.2.2:8080';
    }
    return derived;
  }
  // Fallbacks by platform
  if (Platform.OS === 'android') return 'http://10.0.2.2:8080';
  return 'http://localhost:8080';
}

export async function setApiBaseOverride(url) {
  if (url) await AsyncStorage.setItem(API_OVERRIDE_KEY, url);
}

export async function clearApiBaseOverride() {
  await AsyncStorage.removeItem(API_OVERRIDE_KEY);
}
const ACCESS_TOKEN_KEY = 'auth.accessToken';

class AuthError extends Error {
  constructor(message, code = 'AUTH') {
    super(message);
    this.name = 'AuthError';
    this.code = code;
  }
}

async function saveTokens(token) {
  if (token) await AsyncStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export async function getAccessToken() {
  return AsyncStorage.getItem(ACCESS_TOKEN_KEY);
}

// Decodifica el JWT (sin verificar firma) para extraer claims básicos
export async function decodeAccessToken() {
  const token = await getAccessToken();
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length < 2) return null;
  const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
  try {
    const json = atob ? atob(base64) : Buffer.from(base64, 'base64').toString('utf8');
    return JSON.parse(json);
  } catch {
    return null;
  }
}

export async function getCurrentUser() {
  const payload = await decodeAccessToken();
  if (payload && payload.uid && payload.sub) {
    return {
      id: payload.uid,
      username: payload.sub,
      displayName: payload.displayName,
      email: payload.email,
      source: 'token'
    };
  }
  // Fallback al backend para info fiable
  try {
    const res = await authenticatedFetch('/auth/me', { method: 'GET' });
    if (!res.ok) return null;
    const data = await res.json();
    return { ...data, source: 'api' };
  } catch {
    return null;
  }
}

// No refresh token used

export async function clearTokens() {
  await AsyncStorage.removeItem(ACCESS_TOKEN_KEY);
}

async function handleAuthResponse(res) {
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Error ${res.status}`);
  }
  const data = await res.json();
  // Expecting { token }
  if (data?.token) {
    await saveTokens(data.token);
  }
  return data;
}

export async function register({ username, password, email, displayName }) {
  const base = await getBaseUrl();
  const res = await fetch(`${base}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password, email, displayName })
  });
  return handleAuthResponse(res);
}

export async function login({ username, password }) {
  const base = await getBaseUrl();
  const res = await fetch(`${base}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  return handleAuthResponse(res);
}

// No refresh endpoint; user must re-login when JWT expires

function isExpiredPayload(payload) {
  if (!payload || typeof payload.exp !== 'number') return true; // si no hay exp lo tratamos como expirado para forzar login
  const nowSec = Date.now() / 1000;
  return payload.exp <= nowSec;
}

function isNearExpiry(payload, seconds = 300) { // 5 min
  if (!payload || typeof payload.exp !== 'number') return true;
  const nowSec = Date.now() / 1000;
  return (payload.exp - nowSec) < seconds;
}

export async function tryRefreshOnLaunch() {
  const token = await getAccessToken();
  if (!token) return false;
  const payload = await decodeAccessToken();
  if (isExpiredPayload(payload)) { await clearTokens(); return false; }
  return true;
}

export async function authenticatedFetch(input, init = {}) {
  const base = await getBaseUrl();
  const url = input.startsWith('http') ? input : `${base}${input.startsWith('/') ? '' : '/'}${input}`;
  const token = await getAccessToken();
  const headers = {
    ...(init.headers || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
  const exec = async () => fetch(url, { ...init, headers });
  const res = await exec();
  if (res.status === 401) {
    await clearTokens();
    throw new AuthError('No autorizado. Inicia sesión.', 'UNAUTHORIZED');
  }
  return res;
}

export async function logout() {
  await clearTokens();
}

export default {
  register,
  login,
  authenticatedFetch,
  getAccessToken,
  decodeAccessToken,
  getCurrentUser,
  tryRefreshOnLaunch,
  logout,
  clearTokens,
  setApiBaseOverride,
  clearApiBaseOverride,
};
