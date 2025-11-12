import AsyncStorage from '@react-native-async-storage/async-storage';

const BASE_URL = 'http://192.168.1.103:8080';
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
  const res = await fetch(`${BASE_URL}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password, email, displayName })
  });
  return handleAuthResponse(res);
}

export async function login({ username, password }) {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  return handleAuthResponse(res);
}

export async function refresh() {
  const refreshToken = await getRefreshToken();
  if (!refreshToken) throw new AuthError('No hay refresh token', 'NO_REFRESH');
  const res = await fetch(`${BASE_URL}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });
  try {
    const data = await handleAuthResponse(res);
    return data;
  } catch (e) {
    // If refresh fails, clear tokens
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
  const token = await getAccessToken();
  const headers = {
    ...(init.headers || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
  const exec = async () => fetch(input, { ...init, headers });
  let res = await exec();
  if (res.status === 401) {
    // try refresh once
    try {
      await refresh();
      const token2 = await getAccessToken();
      const headers2 = { ...(init.headers || {}), ...(token2 ? { Authorization: `Bearer ${token2}` } : {}) };
      res = await fetch(input, { ...init, headers: headers2 });
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
};
