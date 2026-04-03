/**
 * Tests for tradeOfferService.js
 *
 * Strategy: mock authenticatedFetch at the module level so every function
 * is tested in isolation without real HTTP calls.
 */

jest.mock('../authService', () => ({
  authenticatedFetch: jest.fn(),
}));

import { authenticatedFetch } from '../authService';
import {
  createOffer,
  acceptOffer,
  rejectOffer,
  cancelOffer,
  getIncomingOffers,
  getOutgoingOffers,
} from '../tradeOfferService';

beforeEach(() => {
  jest.clearAllMocks();
});

// ─── createOffer ──────────────────────────────────────────────────────────────

describe('createOffer', () => {
  test('respuesta ok → devuelve los datos del servidor', async () => {
    const data = { id: 42, status: 'PENDING' };
    authenticatedFetch.mockResolvedValue({
      ok: true,
      json: async () => data,
    });

    const result = await createOffer(2, 'P1', 5_000_000, 1);

    expect(result).toEqual(data);
    expect(authenticatedFetch).toHaveBeenCalledWith(
      '/api/v1/trade-offers',
      expect.objectContaining({ method: 'POST' })
    );
  });

  test('respuesta !ok → lanza Error con mensaje del servidor', async () => {
    authenticatedFetch.mockResolvedValue({
      ok: false,
      json: async () => ({ error: 'Fondos insuficientes' }),
    });

    await expect(createOffer(2, 'P1', 5_000_000, 1)).rejects.toThrow('Fondos insuficientes');
  });
});

// ─── acceptOffer ─────────────────────────────────────────────────────────────

describe('acceptOffer', () => {
  test('respuesta ok → devuelve los datos', async () => {
    const data = { id: 42, status: 'ACCEPTED' };
    authenticatedFetch.mockResolvedValue({ ok: true, json: async () => data });

    const result = await acceptOffer(42);

    expect(result).toEqual(data);
    expect(authenticatedFetch).toHaveBeenCalledWith(
      '/api/v1/trade-offers/42/accept',
      expect.objectContaining({ method: 'PUT' })
    );
  });

  test('respuesta !ok → lanza Error', async () => {
    authenticatedFetch.mockResolvedValue({
      ok: false,
      json: async () => ({ error: 'Oferta no encontrada' }),
    });

    await expect(acceptOffer(99)).rejects.toThrow('Oferta no encontrada');
  });
});

// ─── rejectOffer ─────────────────────────────────────────────────────────────

describe('rejectOffer', () => {
  test('respuesta !ok → lanza Error', async () => {
    authenticatedFetch.mockResolvedValue({
      ok: false,
      json: async () => ({ error: 'Error al rechazar' }),
    });

    await expect(rejectOffer(10)).rejects.toThrow('Error al rechazar');
  });
});

// ─── cancelOffer ─────────────────────────────────────────────────────────────

describe('cancelOffer', () => {
  test('respuesta ok → devuelve los datos', async () => {
    const data = { id: 10, status: 'CANCELLED' };
    authenticatedFetch.mockResolvedValue({ ok: true, json: async () => data });

    const result = await cancelOffer(10);

    expect(result).toEqual(data);
  });
});

// ─── getIncomingOffers ────────────────────────────────────────────────────────

describe('getIncomingOffers', () => {
  test('respuesta ok → devuelve array de ofertas', async () => {
    const offers = [{ id: 1 }, { id: 2 }];
    authenticatedFetch.mockResolvedValue({
      ok: true,
      json: async () => offers,
    });

    const result = await getIncomingOffers(1);

    expect(result).toEqual(offers);
    expect(authenticatedFetch).toHaveBeenCalledWith(
      '/api/v1/trade-offers/league/1/incoming'
    );
  });

  test('respuesta !ok → lanza Error', async () => {
    authenticatedFetch.mockResolvedValue({ ok: false });

    await expect(getIncomingOffers(1)).rejects.toThrow('Error al cargar ofertas recibidas');
  });
});

// ─── getOutgoingOffers ────────────────────────────────────────────────────────

describe('getOutgoingOffers', () => {
  test('respuesta ok → devuelve array de ofertas', async () => {
    const offers = [{ id: 5 }];
    authenticatedFetch.mockResolvedValue({
      ok: true,
      json: async () => offers,
    });

    const result = await getOutgoingOffers(1);

    expect(result).toEqual(offers);
    expect(authenticatedFetch).toHaveBeenCalledWith(
      '/api/v1/trade-offers/league/1/outgoing'
    );
  });
});
