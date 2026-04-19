/**
 * Tests for buyoutService.js
 *
 * Strategy: mock authenticatedFetch at the module level so every function
 * is tested in isolation without real HTTP calls.
 */

jest.mock('../authService', () => ({
  authenticatedFetch: jest.fn(),
}));

import { authenticatedFetch } from '../authService';
import { buyoutPlayer } from '../buyoutService';

beforeEach(() => {
  jest.clearAllMocks();
});

// ─── buyoutPlayer ─────────────────────────────────────────────────────────────

describe('buyoutPlayer', () => {
  test('respuesta ok → devuelve teamId y budget actualizados', async () => {
    const data = { teamId: 1, budget: 7_000_000 };
    authenticatedFetch.mockResolvedValue({
      ok: true,
      json: async () => data,
    });

    const result = await buyoutPlayer(1, 2, 'P1');

    expect(result).toEqual(data);
    expect(authenticatedFetch).toHaveBeenCalledWith(
      '/api/v1/teams/league/1/buyout',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ sellerUserId: 2, playerId: 'P1' }),
      })
    );
  });

  test('respuesta !ok → lanza Error con mensaje del servidor', async () => {
    authenticatedFetch.mockResolvedValue({
      ok: false,
      json: async () => ({ error: 'Presupuesto insuficiente para el clausulazo' }),
    });

    await expect(buyoutPlayer(1, 2, 'P1')).rejects.toThrow(
      'Presupuesto insuficiente para el clausulazo'
    );
  });

  test('respuesta !ok sin mensaje → lanza Error genérico', async () => {
    authenticatedFetch.mockResolvedValue({
      ok: false,
      json: async () => ({}),
    });

    await expect(buyoutPlayer(1, 2, 'P1')).rejects.toThrow(
      'No se pudo realizar el clausulazo'
    );
  });

  test('playerId numérico → lo convierte a string en el cuerpo', async () => {
    authenticatedFetch.mockResolvedValue({
      ok: true,
      json: async () => ({ teamId: 1, budget: 5_000_000 }),
    });

    await buyoutPlayer(1, 2, 42);

    expect(authenticatedFetch).toHaveBeenCalledWith(
      '/api/v1/teams/league/1/buyout',
      expect.objectContaining({
        body: JSON.stringify({ sellerUserId: 2, playerId: '42' }),
      })
    );
  });
});
