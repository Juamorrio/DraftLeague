import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import authService from '../../services/authService';
import withAuth from '../../components/withAuth';
import { colors, fontSize, fontWeight, fontFamily, radius, spacing, shadow } from '../../utils/theme';

function Profile({ onLogout }) {
  const [user, setUser] = useState(null);
  const [leagues, setLeagues] = useState([]);
  const [teamsByLeague, setTeamsByLeague] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const currentUser = await authService.getCurrentUser();
        setUser(currentUser);

        if (currentUser?.id) {
          const leaguesRes = await authService.authenticatedFetch(`/api/v1/users/${currentUser.id}/leagues`);
          if (leaguesRes.ok) {
            const leaguesData = await leaguesRes.json();
            const data = Array.isArray(leaguesData) ? leaguesData : [];
            setLeagues(data);

            // Fetch team info for each league in parallel
            const teamEntries = await Promise.all(
              data.map(async (league) => {
                try {
                  const teamRes = await authService.authenticatedFetch(
                    `/api/v1/teams/league/${league.id}/${currentUser.id}`
                  );
                  if (teamRes.ok) {
                    const team = await teamRes.json();
                    return [league.id, team];
                  }
                } catch {}
                return [league.id, null];
              })
            );
            setTeamsByLeague(Object.fromEntries(teamEntries));
          }
        }
      } catch (e) {
        console.error('Error loading profile:', e);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const handleLogout = () => {
    Alert.alert(
      'Cerrar sesión',
      '¿Estás seguro de que quieres cerrar sesión?',
      [
        { text: 'Cancelar', style: 'cancel' },
        { text: 'Cerrar sesión', style: 'destructive', onPress: onLogout },
      ]
    );
  };

  const formatChips = (usedChips) => {
    if (!usedChips) return 'Ninguno usado';
    const chips = usedChips.split(',').map(c => c.trim()).filter(Boolean);
    if (chips.length === 0) return 'Ninguno usado';
    return chips.join(', ');
  };

  const chipsUsedCount = (usedChips) => {
    if (!usedChips) return 0;
    return usedChips.split(',').filter(c => c.trim()).length;
  };

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  const displayName = user?.displayName || user?.username || 'Usuario';
  const initial = displayName.charAt(0).toUpperCase();

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Avatar + Info */}
      <View style={styles.heroCard}>
        <View style={styles.avatarCircle}>
          <Text style={styles.avatarText}>{initial}</Text>
        </View>
        <View style={styles.heroInfo}>
          <Text style={styles.displayName}>{displayName}</Text>
          {user?.username && user?.displayName && (
            <Text style={styles.username}>@{user.username}</Text>
          )}
          {user?.email && (
            <Text style={styles.email}>{user.email}</Text>
          )}
          {user?.role === 'ADMIN' && (
            <View style={styles.adminBadge}>
              <Text style={styles.adminBadgeText}>Admin</Text>
            </View>
          )}
        </View>
      </View>

      {/* Stats rápidas */}
      <View style={styles.statsRow}>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>{leagues.length}</Text>
          <Text style={styles.statLabel}>Ligas</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>
            {Object.values(teamsByLeague).reduce((acc, t) => acc + (t?.totalPoints ?? 0), 0)}
          </Text>
          <Text style={styles.statLabel}>Pts totales</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>
            {Object.values(teamsByLeague).reduce((acc, t) => acc + chipsUsedCount(t?.usedChips), 0)}
          </Text>
          <Text style={styles.statLabel}>Chips usados</Text>
        </View>
      </View>

      {/* Ligas */}
      {leagues.length > 0 && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Mis ligas</Text>
          {leagues.map((league) => {
            const team = teamsByLeague[league.id];
            return (
              <View key={league.id} style={styles.leagueCard}>
                <View style={styles.leagueAccent} />
                <View style={styles.leagueBody}>
                  <Text style={styles.leagueName}>{league.name}</Text>
                  {league.description ? (
                    <Text style={styles.leagueDesc} numberOfLines={1}>{league.description}</Text>
                  ) : null}
                  {team ? (
                    <View style={styles.leagueMeta}>
                      <View style={styles.metaPill}>
                        <Ionicons name="trophy-outline" size={13} color={colors.primary} />
                        <Text style={styles.metaPillText}>{team.totalPoints ?? 0} pts</Text>
                      </View>
                      <View style={styles.metaPill}>
                        <Ionicons name="flash-outline" size={13} color={colors.warning} />
                        <Text style={styles.metaPillText}>
                          {chipsUsedCount(team.usedChips)}/10 chips
                        </Text>
                      </View>
                      {team.activeChip && (
                        <View style={[styles.metaPill, styles.activeChipPill]}>
                          <Text style={styles.activeChipText}>{team.activeChip}</Text>
                        </View>
                      )}
                    </View>
                  ) : (
                    <Text style={styles.noTeamText}>Sin equipo asignado</Text>
                  )}
                </View>
              </View>
            );
          })}
        </View>
      )}

      {leagues.length === 0 && (
        <View style={styles.emptyLeagues}>
          <Ionicons name="trophy-outline" size={40} color={colors.textMuted} />
          <Text style={styles.emptyLeaguesText}>Aún no perteneces a ninguna liga</Text>
        </View>
      )}

      {/* Cerrar sesión */}
      <TouchableOpacity style={styles.logoutBtn} onPress={handleLogout} activeOpacity={0.8}>
        <Ionicons name="log-out-outline" size={18} color={colors.danger} />
        <Text style={styles.logoutText}>Cerrar sesión</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

export default withAuth(Profile);

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgApp,
  },
  content: {
    padding: spacing.lg,
    paddingBottom: 100,
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: colors.bgApp,
  },
  heroCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.bgCard,
    borderRadius: radius.xl,
    padding: spacing.xl,
    marginBottom: spacing.lg,
    gap: spacing.lg,
    ...shadow.md,
  },
  avatarCircle: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: colors.primary,
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarText: {
    fontFamily: fontFamily.displayBold,
    fontSize: 32,
    color: colors.textInverse,
  },
  heroInfo: {
    flex: 1,
    gap: 4,
  },
  displayName: {
    fontFamily: fontFamily.displayBold,
    fontSize: fontSize.xl,
    color: colors.textPrimary,
  },
  username: {
    fontFamily: fontFamily.body,
    fontSize: fontSize.sm,
    color: colors.textSecondary,
  },
  email: {
    fontFamily: fontFamily.body,
    fontSize: fontSize.xs,
    color: colors.textMuted,
  },
  adminBadge: {
    alignSelf: 'flex-start',
    backgroundColor: colors.warning,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
    marginTop: 4,
  },
  adminBadgeText: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.bold,
    color: colors.textInverse,
  },
  statsRow: {
    flexDirection: 'row',
    gap: spacing.sm,
    marginBottom: spacing.lg,
  },
  statCard: {
    flex: 1,
    backgroundColor: colors.bgCard,
    borderRadius: radius.lg,
    padding: spacing.md,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    ...shadow.sm,
  },
  statValue: {
    fontFamily: fontFamily.displayBold,
    fontSize: fontSize['2xl'],
    color: colors.primary,
  },
  statLabel: {
    fontFamily: fontFamily.body,
    fontSize: fontSize.xs,
    color: colors.textMuted,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginTop: 2,
  },
  section: {
    marginBottom: spacing.lg,
  },
  sectionTitle: {
    fontFamily: fontFamily.displaySemi,
    fontSize: fontSize.lg,
    color: colors.textPrimary,
    marginBottom: spacing.md,
  },
  leagueCard: {
    flexDirection: 'row',
    backgroundColor: colors.bgCard,
    borderRadius: radius.xl,
    marginBottom: spacing.sm,
    borderWidth: 1,
    borderColor: colors.border,
    overflow: 'hidden',
    ...shadow.sm,
  },
  leagueAccent: {
    width: 6,
    backgroundColor: colors.primary,
  },
  leagueBody: {
    flex: 1,
    padding: spacing.md,
    gap: 4,
  },
  leagueName: {
    fontFamily: fontFamily.displaySemi,
    fontSize: fontSize.md,
    color: colors.textPrimary,
  },
  leagueDesc: {
    fontFamily: fontFamily.body,
    fontSize: fontSize.xs,
    color: colors.textMuted,
  },
  leagueMeta: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.xs,
    marginTop: 4,
  },
  metaPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    backgroundColor: colors.bgSubtle,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.sm,
    paddingVertical: 3,
    borderWidth: 1,
    borderColor: colors.border,
  },
  metaPillText: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.semibold,
    color: colors.textSecondary,
  },
  activeChipPill: {
    backgroundColor: colors.warningBg,
    borderColor: '#FDE68A',
  },
  activeChipText: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.bold,
    color: colors.warningDark,
  },
  noTeamText: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
    fontStyle: 'italic',
    marginTop: 4,
  },
  emptyLeagues: {
    alignItems: 'center',
    paddingVertical: spacing['3xl'],
    gap: spacing.md,
  },
  emptyLeaguesText: {
    fontFamily: fontFamily.body,
    fontSize: fontSize.sm,
    color: colors.textMuted,
    textAlign: 'center',
  },
  logoutBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
    backgroundColor: colors.dangerBg,
    borderRadius: radius.lg,
    paddingVertical: spacing.md,
    borderWidth: 1,
    borderColor: '#FECACA',
    marginTop: spacing.sm,
  },
  logoutText: {
    fontFamily: fontFamily.bodyMedium,
    fontSize: fontSize.md,
    color: colors.danger,
    fontWeight: fontWeight.semibold,
  },
});
