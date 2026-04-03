/**
 * Tests for authService.js
 *
 * Strategy:
 * - AsyncStorage is auto-mocked via __mocks__/@react-native-async-storage/async-storage.js
 * - expo-constants is auto-mocked via __mocks__/expo-constants.js
 * - global.fetch is replaced per test with jest.fn()
 * - Each test resets all mocks so state doesn't leak between tests
 */

import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  getAccessToken,
  decodeAccessToken,
  tryRefreshOnLaunch,
  authenticatedFetch,
  clearTokens,
} from '../authService';

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** Builds a base64-encoded JWT string with the given payload. */
function buildJwt(payload) {
  const header  = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body    = btoa(JSON.stringify(payload));
  return `${header}.${body}.fakesig`;
}

/** Returns a timestamp (seconds) N seconds from now. */
const inSeconds = (n) => Math.floor(Date.now() / 1000) + n;

beforeEach(() => {
  jest.clearAllMocks();
  // Reset fetch mock
  global.fetch = undefined;
});

// ─── getAccessToken ───────────────────────────────────────────────────────────

describe('getAccessToken', () => {
  test('devuelve el token almacenado en AsyncStorage', async () => {
    AsyncStorage.getItem.mockResolvedValue('my-token');

    const token = await getAccessToken();

    expect(token).toBe('my-token');
    expect(AsyncStorage.getItem).toHaveBeenCalledWith('auth.accessToken');
  });

  test('devuelve null cuando no hay token', async () => {
    AsyncStorage.getItem.mockResolvedValue(null);

    const token = await getAccessToken();

    expect(token).toBeNull();
  });
});

// ─── decodeAccessToken ────────────────────────────────────────────────────────

describe('decodeAccessToken', () => {
  test('JWT válido → devuelve el payload decodificado', async () => {
    const payload = { uid: 1, sub: 'alice', exp: inSeconds(3600) };
    AsyncStorage.getItem.mockResolvedValue(buildJwt(payload));

    const result = await decodeAccessToken();

    expect(result.uid).toBe(1);
    expect(result.sub).toBe('alice');
  });

  test('sin token en storage → devuelve null', async () => {
    AsyncStorage.getItem.mockResolvedValue(null);

    const result = await decodeAccessToken();

    expect(result).toBeNull();
  });

  test('token malformado (solo 1 segmento) → devuelve null', async () => {
    AsyncStorage.getItem.mockResolvedValue('onlyone');

    const result = await decodeAccessToken();

    expect(result).toBeNull();
  });
});

// ─── tryRefreshOnLaunch ───────────────────────────────────────────────────────

describe('tryRefreshOnLaunch', () => {
  test('sin token → devuelve false', async () => {
    AsyncStorage.getItem.mockResolvedValue(null);

    const result = await tryRefreshOnLaunch();

    expect(result).toBe(false);
  });

  test('token expirado → limpia storage y devuelve false', async () => {
    const expired = buildJwt({ uid: 1, sub: 'alice', exp: inSeconds(-100) });
    AsyncStorage.getItem.mockResolvedValue(expired);

    const result = await tryRefreshOnLaunch();

    expect(result).toBe(false);
    expect(AsyncStorage.removeItem).toHaveBeenCalledWith('auth.accessToken');
  });

  test('token válido no expirado → devuelve true', async () => {
    const valid = buildJwt({ uid: 1, sub: 'alice', exp: inSeconds(36000) });
    AsyncStorage.getItem.mockResolvedValue(valid);

    const result = await tryRefreshOnLaunch();

    expect(result).toBe(true);
  });
});

// ─── authenticatedFetch ───────────────────────────────────────────────────────

describe('authenticatedFetch', () => {
  test('adjunta el header Authorization: Bearer <token> en la petición', async () => {
    const token = buildJwt({ uid: 1, sub: 'alice', exp: inSeconds(3600) });
    AsyncStorage.getItem.mockResolvedValue(token);
    global.fetch = jest.fn().mockResolvedValue({ status: 200, ok: true });

    await authenticatedFetch('/api/v1/teams');

    const [, init] = global.fetch.mock.calls[0];
    expect(init.headers.Authorization).toBe(`Bearer ${token}`);
  });

  test('respuesta 401 → limpia el token y lanza AuthError', async () => {
    AsyncStorage.getItem.mockResolvedValue('some-token');
    global.fetch = jest.fn().mockResolvedValue({ status: 401, ok: false });

    await expect(authenticatedFetch('/api/v1/teams')).rejects.toThrow('No autorizado');
    expect(AsyncStorage.removeItem).toHaveBeenCalledWith('auth.accessToken');
  });

  test('respuesta 200 → devuelve el objeto Response', async () => {
    AsyncStorage.getItem.mockResolvedValue(null); // no token
    const fakeResponse = { status: 200, ok: true };
    global.fetch = jest.fn().mockResolvedValue(fakeResponse);

    const result = await authenticatedFetch('/api/v1/teams');

    expect(result).toBe(fakeResponse);
  });
});

// ─── clearTokens ─────────────────────────────────────────────────────────────

describe('clearTokens', () => {
  test('elimina el token de AsyncStorage', async () => {
    await clearTokens();

    expect(AsyncStorage.removeItem).toHaveBeenCalledWith('auth.accessToken');
  });
});
