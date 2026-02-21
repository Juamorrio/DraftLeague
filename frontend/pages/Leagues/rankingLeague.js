import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ActivityIndicator, ScrollView, TouchableOpacity } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import authService from '../../services/authService';
import { useLeague } from '../../context/LeagueContext';

export default function RankingLeague({ league, onBack }) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [rows, setRows] = useState([]);
  const { setViewUser } = useLeague();
  const [currentUserId, setCurrentUserId] = useState(null);
  const [selectedView, setSelectedView] = useState('total'); // 'total' o número de jornada
  const [maxGameweek] = useState(38); // Número máximo de jornadas

  useEffect(() => {
    if (!league || !league.id) return;
    const load = async () => {
      setLoading(true); setError('');
      try {
        let endpoint;
        if (selectedView === 'total') {
          endpoint = `/api/v1/leagues/${league.id}/ranking`;
        } else {
          endpoint = `/api/v1/fantasy-points/leagues/${league.id}/gameweek/${selectedView}/ranking`;
        }

        const res = await authService.authenticatedFetch(endpoint);
        if (!res.ok) throw new Error(await res.text());
        const data = await res.json();
        setRows(Array.isArray(data) ? data : []);
      } catch (e) {
        setError(e.message || 'Error');
      } finally { setLoading(false); }
    };
    load();
  }, [league, selectedView]); // Agregar selectedView como dependencia

  useEffect(() => {
    (async () => {
      const user = await authService.getCurrentUser();
      setCurrentUserId(user?.id ?? null);
    })();
  }, []);

  return (
    <View style={styles.screen}>
      <View style={styles.header}>
        <Text style={styles.title}>Ranking: {league?.name}</Text>
        <TouchableOpacity onPress={onBack} style={styles.backBtn}><Text style={styles.backTxt}>Volver</Text></TouchableOpacity>
      </View>
      <View style={styles.pickerContainer}>
        <Picker
          selectedValue={selectedView}
          style={styles.picker}
          onValueChange={(value) => setSelectedView(value)}
        >
          <Picker.Item label="Total Acumulado" value="total" />
          {Array.from({ length: maxGameweek }, (_, i) => i + 1).map(gw => (
            <Picker.Item key={gw} label={`Jornada ${gw}`} value={gw} />
          ))}
        </Picker>
      </View>
      {loading && <ActivityIndicator size="large" color="#1d4ed8" />}
      {error ? <Text style={styles.error}>{error}</Text> : null}
      {!loading && !error && rows.length === 0 && <Text style={styles.empty}>Sin equipos todavía.</Text>}
      <ScrollView style={styles.list} contentContainerStyle={styles.listContent}>
        {rows.map(r => {
          const isMe = currentUserId != null && r.userId === currentUserId;
          return (
            <TouchableOpacity
              key={r.teamId}
              style={styles.row}
              activeOpacity={isMe ? 1 : 0.8}
              disabled={isMe}
              onPress={!isMe ? () => setViewUser({ id: r.userId, name: r.userDisplayName || 'Usuario' }) : undefined}
            >
              <Text style={styles.pos}>{r.position}</Text>
              <View style={styles.info}>
                <View style={styles.nameWrap}>
                  <Text style={styles.name}>{r.userDisplayName || 'Usuario'}</Text>
                  {isMe && (
                    <View style={styles.meTag}><Text style={styles.meTagText}>Tú</Text></View>
                  )}
                </View>
                {selectedView === 'total' ? (
                  <Text style={styles.points}>{r.totalPoints} pts</Text>
                ) : (
                  <View>
                    <Text style={styles.points}>{r.gameweekPoints} pts (jornada)</Text>
                    <Text style={styles.totalPointsSecondary}>{r.totalPoints} pts total</Text>
                  </View>
                )}
              </View>
            </TouchableOpacity>
          );
        })}
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
  nameWrap: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  name: { fontSize: 16, fontWeight: '600', color: '#111827' },
  meTag: { backgroundColor: '#f59e0b', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 9999 },
  meTagText: { color: '#fff', fontWeight: '700', fontSize: 10 },
  points: { marginTop: 2, fontSize: 12, color: '#374151' },
  pickerContainer: { marginBottom: 12 },
  picker: { backgroundColor: '#f3f4f6', borderRadius: 8, height: 50 },
  totalPointsSecondary: { fontSize: 10, color: '#6b7280', marginTop: 2 },
});
