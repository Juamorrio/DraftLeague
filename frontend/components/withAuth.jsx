import React, { useEffect, useState } from 'react';
import { tryRefreshOnLaunch } from '../services/authService';
import Login from '../pages/auth/login';

export default function withAuth(Wrapped) {
  return function Guard(props) {
    const [ready, setReady] = useState(false);
    const [needsAuth, setNeedsAuth] = useState(false);

    useEffect(() => {
      (async () => {
        const ok = await tryRefreshOnLaunch();
        if (!ok) {
          setNeedsAuth(true);
        } else {
          setReady(true);
        }
      })();
    }, []);

    if (needsAuth) {
      return (
        <Login
          onLoggedIn={() => { setNeedsAuth(false); setReady(true); }}
        />
      );
    }

    if (!ready) return null;
    return <Wrapped {...props} />;
  };
}
