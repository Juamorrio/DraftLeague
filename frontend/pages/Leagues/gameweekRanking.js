import React, { useState, useEffect, useContext } from 'react';
import { View, Text, StyleSheet, FlatList, ActivityIndicator } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { LeagueContext } from '../../context/LeagueContext';
import authService from '../../services/authService';

export default function GameweekRanking() {
  const { selectedLeague } = useContext(LeagueContext);
  const [selectedGameweek, setSelectedGameweek] = useState(1);
  const [ranking, setRanking] = useState([]);
  const [loading, setLoading] = useState(false);
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
      const token = await authService.getToken();
      const response = await fetch(
        `http://localhost:8080/api/v1/fantasy-points/leagues/${selectedLeague.id}/gameweek/${selectedGameweek}/ranking`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );
      const data = await response.json();
      setRanking(data);
    } catch (error) {
      console.error('Error loading ranking:', error);
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
          <ActivityIndicator size="large" color="#1a5c3a" />
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
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  header: {
    backgroundColor: '#1a5c3a',
    padding: 20,
    paddingTop: 40,
  },
  title: {
    fontSize: 24,
    fontWeight: '800',
    color: '#fff',
    marginBottom: 15,
  },
  pickerContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderRadius: 8,
    paddingHorizontal: 12,
  },
  pickerLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#111827',
    marginRight: 10,
  },
  picker: {
    flex: 1,
    height: 40,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  listContainer: {
    padding: 16,
    gap: 8,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
    marginBottom: 8,
  },
  currentUserRow: {
    backgroundColor: '#f0fdf4',
    borderWidth: 2,
    borderColor: '#1a5c3a',
  },
  positionCircle: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#1d4ed8',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  positionText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '800',
  },
  userInfo: {
    flex: 1,
  },
  userName: {
    fontSize: 16,
    fontWeight: '700',
    color: '#111827',
  },
  youTag: {
    color: '#f59e0b',
    fontWeight: '800',
  },
  pointsContainer: {
    alignItems: 'flex-end',
  },
  gameweekPoints: {
    fontSize: 20,
    fontWeight: '900',
    color: '#1a5c3a',
  },
  totalPoints: {
    fontSize: 12,
    color: '#6b7280',
    marginTop: 2,
  },
});
