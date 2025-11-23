import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ActivityIndicator, ScrollView, TouchableOpacity } from 'react-native';
import authService from '../../services/authService';

export default function RankingLeague({ league, onBack }) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [rows, setRows] = useState([]);

  useEffect(() => {
    if (!league || !league.id) return;
    const load = async () => {
      setLoading(true); setError('');
      try {
        const res = await authService.authenticatedFetch(`/api/v1/leagues/${league.id}/ranking`);
        if (!res.ok) throw new Error(await res.text());
        const data = await res.json();
        setRows(Array.isArray(data) ? data : []);
      } catch (e) {
        setError(e.message || 'Error');
      } finally { setLoading(false); }
    };
    load();
  }, [league]);

  return (
    <View style={styles.screen}>
      <View style={styles.header}> 
        <Text style={styles.title}>Ranking: {league?.name}</Text>
        <TouchableOpacity onPress={onBack} style={styles.backBtn}><Text style={styles.backTxt}>Volver</Text></TouchableOpacity>
      </View>
      {loading && <ActivityIndicator size="large" color="#1d4ed8" />}
      {error ? <Text style={styles.error}>{error}</Text> : null}
      {!loading && !error && rows.length === 0 && <Text style={styles.empty}>Sin equipos todavía.</Text>}
      <ScrollView style={styles.list} contentContainerStyle={styles.listContent}>
        {rows.map(r => (
          <View key={r.teamId} style={styles.row}> 
            <Text style={styles.pos}>{r.position}</Text>
            <View style={styles.info}> 
              <Text style={styles.name}>{r.userDisplayName || 'Usuario'}</Text>
              <Text style={styles.points}>{r.totalPoints} pts</Text>
            </View>
          </View>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, paddingTop: 22, paddingHorizontal: 16 },
  header: { flexDirection: 'row', alignItems: 'center', marginBottom: 14 },
  title: { flex: 1, fontSize: 20, fontWeight: '700', color: '#111827' },
  backBtn: { backgroundColor: '#1f2937', paddingHorizontal: 12, paddingVertical: 8, borderRadius: 8 },
  backTxt: { color: '#fff', fontWeight: '600' },
  error: { marginTop: 10, color: '#dc2626' },
  empty: { marginTop: 20, color: '#6b7280' },
  list: { flex: 1 },
  listContent: { paddingBottom: 40, gap: 8 },
  row: { flexDirection: 'row', backgroundColor: '#e5e7eb', borderRadius: 12, paddingVertical: 10, paddingHorizontal: 12, alignItems: 'center' },
  pos: { width: 32, textAlign: 'center', fontWeight: '700', color: '#1d4ed8', fontSize: 16 },
  info: { flex: 1 },
  name: { fontSize: 16, fontWeight: '600', color: '#111827' },
  points: { marginTop: 2, fontSize: 12, color: '#374151' },
});
