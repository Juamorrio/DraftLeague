import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import Layout from './components/layout';
import React from 'react';

export default function App() {
  const [active, setActive] = React.useState('home');

  return (
    <Layout
      activeKey={active}
      onNavigate={(key) => {
        setActive(key);
      }}
    >
      <View style={styles.container}>
          <StatusBar style="auto" />
          <Text></Text>
        </View>
    </Layout>
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
