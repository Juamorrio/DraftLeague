import React, { createContext, useContext, useState, useMemo } from 'react';

const LeagueContext = createContext(null);

export function LeagueProvider({ children }) {
  const [selectedLeague, setSelectedLeague] = useState(null);
  const [viewUser, setViewUser] = useState(null);
  const [navTarget, setNavTarget] = useState(null);
  const [selectedPlayer, setSelectedPlayer] = useState(null);
  const [comparePlayer, setComparePlayer] = useState(null);

  // Memoize so consumers only re-render when actual values change,
  // not on every LeagueProvider render.
  const value = useMemo(() => ({
    selectedLeague,
    setSelectedLeague,
    viewUser,
    setViewUser,
    navTarget,
    setNavTarget,
    selectedPlayer,
    setSelectedPlayer,
    comparePlayer,
    setComparePlayer,
  }), [selectedLeague, viewUser, navTarget, selectedPlayer, comparePlayer]);

  return (
    <LeagueContext.Provider value={value}>
      {children}
    </LeagueContext.Provider>
  );
}

export function useLeague() {
  const ctx = useContext(LeagueContext);
  if (!ctx) throw new Error('useLeague debe usarse dentro de LeagueProvider');
  return ctx;
}
