import React, { useState, useEffect } from 'react';
import { View, TouchableOpacity, StyleSheet, Image, Text } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import logo from '../assets/header/Logo.png';
import NotificationBell from './NotificationBell';
import NotificationModal from './NotificationModal';
import { colors, shadow, fontWeight } from '../utils/theme';
import authService from '../services/authService';

const Header = ({ onLogout }) => {
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const [notificationModalVisible, setNotificationModalVisible] = useState(false);
  const [bellSyncCallback, setBellSyncCallback] = useState(null);
  const [username, setUsername] = useState(null);

  useEffect(() => {
    authService.getCurrentUser().then(u => {
      if (u?.username || u?.displayName || u?.email) {
        setUsername(u.displayName || u.username || u.email);
      }
    }).catch(() => {});
  }, []);

  const handleProfilePress = () => {
    navigation.navigate('Profile');
  };

  const handleModalClose = (maxSeenId) => {
    setNotificationModalVisible(false);
    if (bellSyncCallback && typeof bellSyncCallback === 'function') {
      bellSyncCallback(maxSeenId);
      setBellSyncCallback(null);
    }
  };

  return (
    <>
      <LinearGradient
        colors={[colors.gradientStart, colors.gradientEnd]}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 0 }}
        style={[styles.headerContainer, { paddingTop: insets.top + 4 }]}
      >
        <TouchableOpacity style={styles.sideSlot} activeOpacity={0.7} onPress={handleProfilePress}>
          <View style={styles.profileCircle}>
            <Text style={styles.profileIcon}>{username ? username.charAt(0).toUpperCase() : '👤'}</Text>
          </View>
        </TouchableOpacity>

        <View style={styles.logoContainer}>
          <Image source={logo} style={styles.logo} resizeMode="contain" />
        </View>

        <View style={[styles.sideSlot, { justifyContent: 'flex-end' }]}>
          <NotificationBell
            onPress={() => setNotificationModalVisible(true)}
            onRequestSync={(syncFn) => setBellSyncCallback(() => syncFn)}
          />
        </View>
      </LinearGradient>

      <NotificationModal
        visible={notificationModalVisible}
        onClose={handleModalClose}
      />
    </>
  );
};

const styles = StyleSheet.create({
  headerContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 6,
    ...shadow.md,
  },
  sideSlot: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    minWidth: 80,
  },
  profileCircle: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: 'rgba(255,255,255,0.15)',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.25)',
  },
  profileIcon: {
    fontSize: 16,
    fontWeight: fontWeight.bold,
    color: colors.textInverse,
  },
  logoContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  logo: {
    width: 68,
    height: 68,
  },
});

export default Header;
