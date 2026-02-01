import React, { createContext, useContext, useState, useEffect } from 'react';
import { authenticatedFetch } from '../services/authService';

const MatchesContext = createContext(null);

export function MatchesProvider({ children }) {
  const [playedMatches, setPlayedMatches] = useState({});
  const [upcomingMatches, setUpcomingMatches] = useState({});
  const [teamImages, setTeamImages] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadTeamImage = async (teamId) => {
    if (teamImages[teamId]) return teamImages[teamId];
    
    try {
      const res = await authenticatedFetch(`/api/v1/players/load-image-team-player?teamId=${teamId}`);
      if (res.ok) {
        const data = await res.json();
        if (data.imageBytes) {
          const imageUrl = `data:image/png;base64,${data.imageBytes}`;
          setTeamImages(prev => ({ ...prev, [teamId]: imageUrl }));
          return imageUrl;
        }
      }
    } catch (e) {
      console.error('Error cargando imagen del equipo:', e);
    }
    return null;
  };

  const loadMatches = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const playedRes = await authenticatedFetch('/api/v1/matches/played');
      if (playedRes.ok) {
        const playedData = await playedRes.json();
        setPlayedMatches(playedData);
        
        const allPlayed = Object.values(playedData).flat();
        const teamIds = [...new Set(allPlayed.flatMap(m => [m.homeTeamId, m.awayTeamId]))];
        await Promise.all(teamIds.map(id => loadTeamImage(id)));
      }

      const upcomingRes = await authenticatedFetch('/api/v1/matches/upcoming');
      if (upcomingRes.ok) {
        const upcomingData = await upcomingRes.json();
        setUpcomingMatches(upcomingData);
        
        const allUpcoming = Object.values(upcomingData).flat();
        const teamIds = [...new Set(allUpcoming.flatMap(m => [m.homeTeamId, m.awayTeamId]))];
        await Promise.all(teamIds.map(id => loadTeamImage(id)));
      }
    } catch (error) {
      console.error('Error cargando partidos:', error);
      setError(error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMatches();
  }, []);

  const refetch = () => {
    loadMatches();
  };

  return (
    <MatchesContext.Provider
      value={{
        playedMatches,
        upcomingMatches,
        teamImages,
        loading,
        error,
        refetch,
        loadTeamImage,
        loadMatches
      }}
    >
      {children}
    </MatchesContext.Provider>
  );
}

export function useMatches() {
  const ctx = useContext(MatchesContext);
  if (!ctx) throw new Error('useMatches debe usarse dentro de MatchesProvider');
  return ctx;
}
