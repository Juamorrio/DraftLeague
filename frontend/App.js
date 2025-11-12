import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import Layout from './components/layout';
import React from 'react';
import Header from './components/header';
import Register from './pages/auth/register';
import authService from './services/authService';

export default function App() {
  const [active, setActive] = React.useState('home');
  const [authed, setAuthed] = React.useState(false);
  const [checking, setChecking] = React.useState(true);

  React.useEffect(() => {
    (async () => {
      try {
        const token = await authService.getToken();
        setAuthed(!!token);
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
        <Register onRegistered={() => setAuthed(true)} />
      </View>
    );
  }

  return (
    <>
      <Header />
      <Layout
        activeKey={active}
        onNavigate={(key) => {
          setActive(key);
        }}
      >
        <View style={styles.container}>
          <StatusBar style="auto" />
          <Text>Contenido de la app</Text>
        </View>
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
