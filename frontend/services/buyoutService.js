import { authenticatedFetch } from './authService';

const BASE = '/api/v1/teams';

/**
 * Execute a buyout (clausulazo) to buy a player directly from another team
 * by paying the defined release price, without going through a negotiation process.
 *
 * @param {number} leagueId     - League context
 * @param {number} sellerUserId - User ID of the team that currently owns the player
 * @param {string} playerId     - Player ID to buy out
 * @returns {Promise<{teamId: number, budget: number}>} Updated buyer team info
 */
export async function buyoutPlayer(leagueId, sellerUserId, playerId) {
	const res = await authenticatedFetch(`${BASE}/league/${leagueId}/buyout`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ sellerUserId, playerId: String(playerId) }),
	});
	const data = await res.json();
	if (!res.ok) throw new Error(data?.error || 'No se pudo realizar el clausulazo');
	return data;
}
