import { authenticatedFetch } from './authService';

export async function getNextRoundMatches() {
  const res = await authenticatedFetch('/api/ml/next-round');
  if (!res.ok) throw new Error('Error al obtener la próxima jornada');
  return res.json();
}


export async function predictPlayerPoints(playerId, nextMatchId = null) {
  let url = `/api/ml/predict/player/${playerId}`;
  if (nextMatchId) url += `?nextMatchId=${nextMatchId}`;
  
  const res = await authenticatedFetch(url);
  if (!res.ok) throw new Error('Error al predecir puntos del jugador');
  return res.json();
}

export async function predictTeamPoints(teamId, nextMatchId = null) {
  let url = `/api/ml/predict/team/${teamId}`;
  if (nextMatchId) url += `?nextMatchId=${nextMatchId}`;
  
  const res = await authenticatedFetch(url);
  if (!res.ok) throw new Error('Error al predecir puntos del equipo');
  return res.json();
}

export async function getTeamPointsHistory(teamId) {
  const res = await authenticatedFetch(`/api/v1/fantasy-points/teams/${teamId}/history`);
  if (!res.ok) throw new Error('Error al obtener el historial de puntos');
  return res.json();
}

export async function recalculateAllPoints() {
  const res = await authenticatedFetch('/api/ml/recalculate-fantasy-points', {
    method: 'POST'
  });
  if (!res.ok) throw new Error('Error al recalcular puntos');
  return res.json();
}

export default {
  getNextRoundMatches,
  predictPlayerPoints,
  predictTeamPoints,
  getTeamPointsHistory,
  recalculateAllPoints
};
