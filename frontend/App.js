import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import Layout from './components/layout';

export default function App() {
  return (
    <View style={styles.container}>
      <Layout>
        <Text>hola</Text>
      </Layout>
      <StatusBar style="auto" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#b02c2cff',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
