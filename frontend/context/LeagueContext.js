import React, { createContext, useContext, useState } from 'react';

const LeagueContext = createContext(null);

export function LeagueProvider({ children }) {
  const [selectedLeague, setSelectedLeague] = useState(null);
  const [viewUser, setViewUser] = useState(null); 
  const [navTarget, setNavTarget] = useState(null);
  return (
    <LeagueContext.Provider value={{ selectedLeague, setSelectedLeague, viewUser, setViewUser, navTarget, setNavTarget }}>
      {children}
    </LeagueContext.Provider>
  );
}

export function useLeague() {
  const ctx = useContext(LeagueContext);
  if (!ctx) throw new Error('useLeague debe usarse dentro de LeagueProvider');
  return ctx;
}
