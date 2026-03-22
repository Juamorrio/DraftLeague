import React, { useState, useEffect, useContext } from 'react';
import { View, Text, StyleSheet, FlatList, ActivityIndicator } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { LeagueContext } from '../../context/LeagueContext';
import authService, { authenticatedFetch } from '../../services/authService';
import { colors, fontSize, fontWeight, radius, spacing, shadow } from '../../utils/theme';

export default function GameweekRanking() {
  const { selectedLeague } = useContext(LeagueContext);
  const [selectedGameweek, setSelectedGameweek] = useState(1);
  const [ranking, setRanking] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentUserId, setCurrentUserId] = useState(null);

  useEffect(() => {
    loadCurrentUser();
  }, []);

  useEffect(() => {
    if (selectedLeague) {
      loadRanking();
    }
  }, [selectedLeague, selectedGameweek]);

  const loadCurrentUser = async () => {
    const user = await authService.getCurrentUser();
    if (user) setCurrentUserId(user.id);
  };

  const loadRanking = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await authenticatedFetch(
        `/api/v1/fantasy-points/leagues/${selectedLeague.id}/gameweek/${selectedGameweek}/ranking`
      );
      if (!res.ok) throw new Error(`Error ${res.status}`);
      const data = await res.json();
      setRanking(data);
    } catch (error) {
      console.error('Error loading ranking:', error);
      setError('No se pudo cargar el ranking. Inténtalo de nuevo.');
    } finally {
      setLoading(false);
    }
  };

  const renderItem = ({ item }) => {
    const isCurrentUser = item.userId === currentUserId;

    return (
      <View style={[styles.row, isCurrentUser && styles.currentUserRow]}>
        <View style={styles.positionCircle}>
          <Text style={styles.positionText}>{item.position}</Text>
        </View>

        <View style={styles.userInfo}>
          <Text style={styles.userName}>
            {item.userDisplayName}
            {isCurrentUser && <Text style={styles.youTag}> (Tú)</Text>}
          </Text>
        </View>

        <View style={styles.pointsContainer}>
          <Text style={styles.gameweekPoints}>{item.gameweekPoints} pts</Text>
          <Text style={styles.totalPoints}>Total: {item.totalPoints}</Text>
        </View>
      </View>
    );
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Ranking por Jornada</Text>

        <View style={styles.pickerContainer}>
          <Text style={styles.pickerLabel}>Jornada:</Text>
          <Picker
            selectedValue={selectedGameweek}
            onValueChange={setSelectedGameweek}
            style={styles.picker}
          >
            {Array.from({ length: 38 }, (_, i) => i + 1).map(gw => (
              <Picker.Item key={gw} label={`J${gw}`} value={gw} />
            ))}
          </Picker>
        </View>
      </View>

      {loading ? (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={colors.primaryDeep} />
        </View>
      ) : error ? (
        <View style={styles.loadingContainer}>
          <Text style={styles.errorText}>{error}</Text>
        </View>
      ) : (
        <FlatList
          data={ranking}
          renderItem={renderItem}
          keyExtractor={item => item.teamId.toString()}
          contentContainerStyle={styles.listContainer}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bgApp },
  header: { backgroundColor: colors.primaryDeep, padding: spacing.xl, paddingTop: 40 },
  title: { fontSize: fontSize['2xl'], fontWeight: fontWeight.extrabold, color: colors.textInverse, marginBottom: 15 },
  pickerContainer: {
    flexDirection: 'row', alignItems: 'center', backgroundColor: colors.bgCard,
    borderRadius: radius.md, paddingHorizontal: spacing.md,
  },
  pickerLabel: { fontSize: fontSize.sm, fontWeight: fontWeight.semibold, color: colors.textPrimary, marginRight: 10 },
  picker: { flex: 1, height: 40 },
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  errorText: { fontSize: fontSize.md, color: colors.danger, textAlign: 'center', paddingHorizontal: spacing.xl },
  listContainer: { padding: spacing.lg, gap: spacing.sm },
  row: {
    flexDirection: 'row', alignItems: 'center', backgroundColor: colors.bgCard,
    padding: spacing.lg, borderRadius: radius.lg, ...shadow.sm, marginBottom: spacing.sm,
    borderWidth: 1, borderColor: colors.border,
  },
  currentUserRow: { backgroundColor: colors.successBg, borderWidth: 2, borderColor: colors.primaryDark },
  positionCircle: {
    width: 40, height: 40, borderRadius: 20, backgroundColor: colors.primaryDark,
    justifyContent: 'center', alignItems: 'center', marginRight: spacing.md,
  },
  positionText: { color: colors.textInverse, fontSize: fontSize.lg, fontWeight: fontWeight.extrabold },
  userInfo: { flex: 1 },
  userName: { fontSize: fontSize.md, fontWeight: fontWeight.bold, color: colors.textPrimary },
  youTag: { color: colors.warning, fontWeight: fontWeight.extrabold },
  pointsContainer: { alignItems: 'flex-end' },
  gameweekPoints: { fontSize: fontSize.xl, fontWeight: fontWeight.black, color: colors.primaryDark },
  totalPoints: { fontSize: fontSize.sm, color: colors.textMuted, marginTop: 2 },
});
