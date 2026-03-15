import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { StyleSheet, View, Text } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { useFonts } from 'expo-font';
import {
  BarlowCondensed_600SemiBold,
  BarlowCondensed_700Bold,
} from '@expo-google-fonts/barlow-condensed';
import {
  Barlow_400Regular,
  Barlow_500Medium,
  Barlow_600SemiBold,
  Barlow_700Bold,
} from '@expo-google-fonts/barlow';

// Contexts
import { LeagueProvider } from './context/LeagueContext';
import { MatchesProvider } from './context/MatchesContext';

// Components
import Header from './components/header';
import RootNavigator from './navigation/RootNavigator';

// Services
import authService from './services/authService';

export default function App() {
  const [fontsLoaded] = useFonts({
    BarlowCondensed_600SemiBold,
    BarlowCondensed_700Bold,
    Barlow_400Regular,
    Barlow_500Medium,
    Barlow_600SemiBold,
    Barlow_700Bold,
  });

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

  const handleLogin = async () => {
    setAuthed(true);
    const currentUser = await authService.getCurrentUser();
    setUser(currentUser);
  };

  const handleLogout = async () => {
    await authService.logout();
    setAuthed(false);
    setUser(null);
  };

  if (checking || !fontsLoaded) {
    return (
      <View style={styles.loadingContainer}>
        <StatusBar style="auto" />
        <Text>Cargando...</Text>
      </View>
    );
  }

  return (
    <SafeAreaProvider>
    <LeagueProvider>
      <MatchesProvider>
        <NavigationContainer>
          <View style={styles.container}>
            <StatusBar style="auto" />
            {authed && <Header onLogout={handleLogout} />}
            <RootNavigator 
              authed={authed} 
              authMode={authMode}
              user={user}
              onLoggedIn={handleLogin}
              onRegistered={handleLogin}
              onSwitchToRegister={() => setAuthMode('register')}
              onSwitchToLogin={() => setAuthMode('login')}
            />
          </View>
        </NavigationContainer>
      </MatchesProvider>
    </LeagueProvider>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  loadingContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#fff',
  }
});
