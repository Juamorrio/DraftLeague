import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import Layout from './components/layout';
import React from 'react';
import Header from './components/header';
import Register from './pages/auth/register';
import Login from './pages/auth/login';
import authService from './services/authService';
import Leagues from './pages/Leagues/leagues';

export default function App() {
  const [active, setActive] = React.useState('home');
  const [authed, setAuthed] = React.useState(false);
  const [checking, setChecking] = React.useState(true);
  const [authMode, setAuthMode] = React.useState('login'); 
  

  React.useEffect(() => {
    (async () => {
      try {
        const ok = await authService.tryRefreshOnLaunch();
        setAuthed(ok);
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
        {authMode === 'login' ? (
          <Login
            onLoggedIn={() => setAuthed(true)}
            onSwitchToRegister={() => setAuthMode('register')}
          />
        ) : (
          <Register
            onRegistered={() => setAuthed(true)}
            onSwitchToLogin={() => setAuthMode('login')}
          />
        )}
      </View>
    );
  }

  return (
    <>
  <Header onLogout={async () => { await authService.logout(); setAuthed(false); }} />
      <Layout
        activeKey={active}
        onNavigate={(key) => {
          setActive(key);
        }}
      >
        {active === 'league' ? (
          <Leagues />
        ) : (
          <View style={styles.container}>
            <StatusBar style="auto" />
            <Text>Contenido de la app</Text>
          </View>
        )}
      </Layout>
    </>
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
