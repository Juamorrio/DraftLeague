import { authenticatedFetch } from './authService';

const BASE = '/api/v1/trade-offers';

/**
 * Propose a trade offer to buy a player from another team.
 * @param {number} toTeamId - ID of the team that owns the player
 * @param {string} playerId - Player ID
 * @param {number} offerPrice - Offered price in €
 * @param {number} leagueId - League context
 */
export async function createOffer(toTeamId, playerId, offerPrice, leagueId) {
	const res = await authenticatedFetch(BASE, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ toTeamId, playerId, offerPrice, leagueId }),
	});
	const data = await res.json();
	if (!res.ok) throw new Error(data?.error || 'Error al enviar oferta');
	return data;
}

/**
 * Accept an incoming trade offer (seller accepts).
 * @param {number} offerId
 */
export async function acceptOffer(offerId) {
	const res = await authenticatedFetch(`${BASE}/${offerId}/accept`, { method: 'PUT' });
	const data = await res.json();
	if (!res.ok) throw new Error(data?.error || 'Error al aceptar oferta');
	return data;
}

/**
 * Reject an incoming trade offer (seller rejects).
 * @param {number} offerId
 */
export async function rejectOffer(offerId) {
	const res = await authenticatedFetch(`${BASE}/${offerId}/reject`, { method: 'PUT' });
	const data = await res.json();
	if (!res.ok) throw new Error(data?.error || 'Error al rechazar oferta');
	return data;
}

/**
 * Cancel an outgoing trade offer (buyer cancels).
 * @param {number} offerId
 */
export async function cancelOffer(offerId) {
	const res = await authenticatedFetch(`${BASE}/${offerId}/cancel`, { method: 'PUT' });
	const data = await res.json();
	if (!res.ok) throw new Error(data?.error || 'Error al cancelar oferta');
	return data;
}

/**
 * Get all incoming offers for the current user in a league (offers on your players).
 * @param {number} leagueId
 */
export async function getIncomingOffers(leagueId) {
	const res = await authenticatedFetch(`${BASE}/league/${leagueId}/incoming`);
	if (!res.ok) throw new Error('Error al cargar ofertas recibidas');
	return res.json();
}

/**
 * Get all outgoing offers for the current user in a league (offers you made).
 * @param {number} leagueId
 */
export async function getOutgoingOffers(leagueId) {
	const res = await authenticatedFetch(`${BASE}/league/${leagueId}/outgoing`);
	if (!res.ok) throw new Error('Error al cargar ofertas enviadas');
	return res.json();
}
