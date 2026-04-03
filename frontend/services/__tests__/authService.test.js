import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  getAccessToken,
  decodeAccessToken,
  tryRefreshOnLaunch,
  authenticatedFetch,
  clearTokens,
} from '../authService';

function buildJwt(payload) {
  const header  = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body    = btoa(JSON.stringify(payload));
  return `${header}.${body}.fakesig`;
}

const inSeconds = (n) => Math.floor(Date.now() / 1000) + n;

beforeEach(() => {
  jest.clearAllMocks();
  global.fetch = undefined;
});

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

describe('authenticatedFetch', () => {
  test('adjunta el header Authorization con el token JWT en la petición', async () => {
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
    AsyncStorage.getItem.mockResolvedValue(null);
    const fakeResponse = { status: 200, ok: true };
    global.fetch = jest.fn().mockResolvedValue(fakeResponse);

    const result = await authenticatedFetch('/api/v1/teams');

    expect(result).toBe(fakeResponse);
  });
});

describe('clearTokens', () => {
  test('elimina el token de AsyncStorage', async () => {
    await clearTokens();

    expect(AsyncStorage.removeItem).toHaveBeenCalledWith('auth.accessToken');
  });
});
