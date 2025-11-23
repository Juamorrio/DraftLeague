import React, { createContext, useContext, useState } from 'react';

const LeagueContext = createContext(null);

export function LeagueProvider({ children }) {
  const [selectedLeague, setSelectedLeague] = useState(null);
  return (
    <LeagueContext.Provider value={{ selectedLeague, setSelectedLeague }}>
      {children}
    </LeagueContext.Provider>
  );
}

export function useLeague() {
  const ctx = useContext(LeagueContext);
  if (!ctx) throw new Error('useLeague debe usarse dentro de LeagueProvider');
  return ctx;
}
