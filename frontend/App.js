import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import Layout from './components/layout';
import { LeagueProvider, useLeague } from './context/LeagueContext';
import { MatchesProvider } from './context/MatchesContext';
import React from 'react';
import Header from './components/header';
import Register from './pages/auth/register';
import Login from './pages/auth/login';
import authService from './services/authService';
import Leagues from './pages/Leagues/leagues';
import Team from './pages/Teams/team';
import Market from './pages/Market/market';
import Admin from './pages/Admin/admin';
import Home from './pages/Home/home';


function RobotPlaceholder() {
  const { selectedLeague } = useLeague();
  return (
    <View style={styles.container}><Text>IA de {selectedLeague?.name}</Text></View>
  );
}

export default function App() {
  const [active, setActive] = React.useState('home');
  const [authed, setAuthed] = React.useState(false);
  const [checking, setChecking] = React.useState(true);
  const [authMode, setAuthMode] = React.useState('login');
  const [user, setUser] = React.useState(null); 
  

  React.useEffect(() => {
    (async () => {
      try {
        const ok = await authService.tryRefreshOnLaunch();
        setAuthed(ok);
        if (ok) {
          const currentUser = await authService.getCurrentUser();
          setUser(currentUser);
        }
        const onlyRegister = await authService.shouldShowRegisterOnly();
        if (onlyRegister) setAuthMode('register');
      } finally {
        setChecking(false);
      }
    })();
  }, []);

  if (checking) {
    return (
      <View style={[styles.container, { justifyContent: 'center', alignItems: 'center' }]}>
        <StatusBar style="auto" />
        <Text>Cargando...</Text>
      </View>
    );
  }

  if (!authed) {
    return (
      <View style={styles.container}>
        <StatusBar style="auto" />
        {authMode === 'register' ? (
          <Register
            onRegistered={async () => {
              setAuthed(true);
              const currentUser = await authService.getCurrentUser();
              setUser(currentUser);
            }}
          />
        ) : (
          <Login
            onLoggedIn={async () => {
              setAuthed(true);
              const currentUser = await authService.getCurrentUser();
              setUser(currentUser);
            }}
            onSwitchToRegister={() => setAuthMode('register')}
          />
        )}
      </View>
    );
  }

  return (
    <LeagueProvider>
      <MatchesProvider>
        <Header onLogout={async () => { await authService.logout(); setAuthed(false); }} />
        <Layout
          activeKey={active}
          onNavigate={(key) => {
            setActive(key);
          }}
          isAdmin={user?.role === 'ADMIN'}
        >
          {active === 'league' && <Leagues />}
          {active === 'home' && <Home />}
          {active === 'team' && <Team />}
          {active === 'market' && <Market />}
          {active === 'robot' && <RobotPlaceholder />}
          {active === 'admin' && <Admin />}
          {!['home','league','team','market','robot','admin'].includes(active) && (
            <View style={styles.container}><Text>Pantalla no definida</Text></View>
          )}
        </Layout>
      </MatchesProvider>
    </LeagueProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#ffffffff',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
