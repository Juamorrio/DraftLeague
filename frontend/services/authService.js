import AsyncStorage from '@react-native-async-storage/async-storage';
import Constants from 'expo-constants';


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
  const derived = deriveHostFromExpo();
  if (derived) return derived;
  return __DEV__ ? 'http://10.0.2.2:8080' : 'http://localhost:8080';
}

export async function setApiBaseOverride(url) {
  if (url) await AsyncStorage.setItem(API_OVERRIDE_KEY, url);
}

export async function clearApiBaseOverride() {
  await AsyncStorage.removeItem(API_OVERRIDE_KEY);
}
const ACCESS_TOKEN_KEY = 'auth.accessToken';
const REFRESH_TOKEN_KEY = 'auth.refreshToken';

class AuthError extends Error {
  constructor(message, code = 'AUTH') {
    super(message);
    this.name = 'AuthError';
    this.code = code;
  }
}

async function saveTokens(token, refreshToken) {
  if (token) await AsyncStorage.setItem(ACCESS_TOKEN_KEY, token);
  if (refreshToken) await AsyncStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export async function getAccessToken() {
  return AsyncStorage.getItem(ACCESS_TOKEN_KEY);
}

export async function getRefreshToken() {
  return AsyncStorage.getItem(REFRESH_TOKEN_KEY);
}

export async function clearTokens() {
  await AsyncStorage.multiRemove([ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY]);
}

async function handleAuthResponse(res) {
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Error ${res.status}`);
  }
  const data = await res.json();
  // Expecting { token, refreshToken }
  if (data?.token || data?.refreshToken) {
    await saveTokens(data.token, data.refreshToken);
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

export async function refresh() {
  const refreshToken = await getRefreshToken();
  if (!refreshToken) throw new AuthError('No hay refresh token', 'NO_REFRESH');
  const base = await getBaseUrl();
  const res = await fetch(`${base}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });
  try {
    const data = await handleAuthResponse(res);
    return data;
  } catch (e) {
    await clearTokens();
    throw new AuthError(e?.message || 'Refresh failed', 'REFRESH_FAILED');
  }
}

export async function tryRefreshOnLaunch() {
  const token = await getAccessToken();
  if (token) return true;
  const r = await getRefreshToken();
  if (!r) return false;
  try {
    await refresh();
    return true;
  } catch {
    return false;
  }
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
  let res = await exec();
  if (res.status === 401) {
    try {
      await refresh();
      const token2 = await getAccessToken();
      const headers2 = { ...(init.headers || {}), ...(token2 ? { Authorization: `Bearer ${token2}` } : {}) };
      res = await fetch(url, { ...init, headers: headers2 });
    } catch (e) {
      throw new AuthError('Sesión expirada. Inicia sesión de nuevo.', 'UNAUTHORIZED');
    }
  }
  return res;
}

export async function logout() {
  await clearTokens();
}

export default {
  register,
  login,
  refresh,
  authenticatedFetch,
  getAccessToken,
  getRefreshToken,
  tryRefreshOnLaunch,
  logout,
  clearTokens,
  setApiBaseOverride,
  clearApiBaseOverride,
};
