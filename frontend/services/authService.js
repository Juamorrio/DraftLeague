import AsyncStorage from '@react-native-async-storage/async-storage';

const BASE_URL = 'http://192.168.1.103:8080'; 
const TOKEN_KEY = 'authToken';

async function saveToken(token) {
  if (token) {
    await AsyncStorage.setItem(TOKEN_KEY, token);
  }
}

export async function getToken() {
  return AsyncStorage.getItem(TOKEN_KEY);
}

export async function clearToken() {
  await AsyncStorage.removeItem(TOKEN_KEY);
}

export async function register({ username, password, email, displayName }) {
  const res = await fetch(`${BASE_URL}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password, email, displayName })
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Error ${res.status}`);
  }
  const data = await res.json();
  if (data?.token) await saveToken(data.token);
  return data;
}

export async function login({ username, password }) {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Error ${res.status}`);
  }
  const data = await res.json();
  if (data?.token) await saveToken(data.token);
  return data;
}

export default { register, login, getToken, clearToken };
