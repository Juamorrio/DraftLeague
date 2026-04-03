/**
 * Tests para predictionService.js
 *
 * Estrategia: mock de authenticatedFetch a nivel de módulo para aislar
 * cada función sin llamadas HTTP reales.
 */

jest.mock('../authService', () => ({
  authenticatedFetch: jest.fn(),
}));

import { authenticatedFetch } from '../authService';
import {
  getNextRoundMatches,
  predictPlayerPoints,
  predictTeamPoints,
  getTeamPointsHistory,
  recalculateAllPoints,
} from '../predictionService';

beforeEach(() => {
  jest.clearAllMocks();
});

// ─── getNextRoundMatches ──────────────────────────────────────────────────────

describe('getNextRoundMatches', () => {
  test('respuesta ok → devuelve datos del servidor', async () => {
    const data = { round: 5, matches: [{ id: 1 }] };
    authenticatedFetch.mockResolvedValue({ ok: true, json: async () => data });

    const result = await getNextRoundMatches();

    expect(result).toEqual(data);
    expect(authenticatedFetch).toHaveBeenCalledWith('/api/ml/next-round');
  });

  test('respuesta !ok → lanza Error', async () => {
    authenticatedFetch.mockResolvedValue({ ok: false });

    await expect(getNextRoundMatches()).rejects.toThrow('Error al obtener la próxima jornada');
  });
});

// ─── predictPlayerPoints ─────────────────────────────────────────────────────

describe('predictPlayerPoints', () => {
  test('respuesta ok → devuelve predicción del jugador', async () => {
    const data = { playerId: 'P1', predictedPoints: 8.5 };
    authenticatedFetch.mockResolvedValue({ ok: true, json: async () => data });

    const result = await predictPlayerPoints('P1');

    expect(result).toEqual(data);
    expect(authenticatedFetch).toHaveBeenCalledWith('/api/ml/predict/player/P1');
  });

  test('con nextMatchId → URL incluye parámetro nextMatchId', async () => {
    authenticatedFetch.mockResolvedValue({ ok: true, json: async () => ({}) });

    await predictPlayerPoints('P2', 42);

    expect(authenticatedFetch).toHaveBeenCalledWith('/api/ml/predict/player/P2?nextMatchId=42');
  });

  test('respuesta !ok → lanza Error', async () => {
    authenticatedFetch.mockResolvedValue({ ok: false });

    await expect(predictPlayerPoints('P1')).rejects.toThrow('Error al predecir puntos del jugador');
  });
});

// ─── predictTeamPoints ───────────────────────────────────────────────────────

describe('predictTeamPoints', () => {
  test('respuesta ok → devuelve predicción del equipo', async () => {
    const data = { teamId: 1, totalPredictedPoints: 65.0 };
    authenticatedFetch.mockResolvedValue({ ok: true, json: async () => data });

    const result = await predictTeamPoints(1);

    expect(result).toEqual(data);
    expect(authenticatedFetch).toHaveBeenCalledWith('/api/ml/predict/team/1');
  });

  test('con nextMatchId → URL incluye parámetro nextMatchId', async () => {
    authenticatedFetch.mockResolvedValue({ ok: true, json: async () => ({}) });

    await predictTeamPoints(1, 10);

    expect(authenticatedFetch).toHaveBeenCalledWith('/api/ml/predict/team/1?nextMatchId=10');
  });

  test('respuesta !ok → lanza Error', async () => {
    authenticatedFetch.mockResolvedValue({ ok: false });

    await expect(predictTeamPoints(1)).rejects.toThrow('Error al predecir puntos del equipo');
  });
});

// ─── getTeamPointsHistory ─────────────────────────────────────────────────────

describe('getTeamPointsHistory', () => {
  test('respuesta ok → devuelve historial de puntos', async () => {
    const data = [{ gameweek: 1, points: 60 }, { gameweek: 2, points: 72 }];
    authenticatedFetch.mockResolvedValue({ ok: true, json: async () => data });

    const result = await getTeamPointsHistory(5);

    expect(result).toEqual(data);
    expect(authenticatedFetch).toHaveBeenCalledWith('/api/v1/fantasy-points/teams/5/history');
  });

  test('respuesta !ok → lanza Error', async () => {
    authenticatedFetch.mockResolvedValue({ ok: false });

    await expect(getTeamPointsHistory(5)).rejects.toThrow('Error al obtener el historial de puntos');
  });
});

// ─── recalculateAllPoints ─────────────────────────────────────────────────────

describe('recalculateAllPoints', () => {
  test('respuesta ok → devuelve resultado del recálculo', async () => {
    const data = { status: 'recalculated', count: 100 };
    authenticatedFetch.mockResolvedValue({ ok: true, json: async () => data });

    const result = await recalculateAllPoints();

    expect(result).toEqual(data);
    expect(authenticatedFetch).toHaveBeenCalledWith(
      '/api/ml/recalculate-fantasy-points',
      expect.objectContaining({ method: 'POST' })
    );
  });

  test('respuesta !ok → lanza Error', async () => {
    authenticatedFetch.mockResolvedValue({ ok: false });

    await expect(recalculateAllPoints()).rejects.toThrow('Error al recalcular puntos');
  });
});
