import React, { useState } from 'react';
import { View, TouchableOpacity, StyleSheet } from 'react-native';
import { Ionicons, MaterialIcons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { Image } from 'react-native';
import logo from '../assets/header/Logo.png'
import bell from '../assets/header/bell.png'
import settings from '../assets/header/gear.png'
import NotificationBell from './NotificationBell';
import NotificationModal from './NotificationModal';

const Header = ({ userIcon, logoIcon, notificationIcon, settingsIcon, onLogout }) => {
  const [notificationModalVisible, setNotificationModalVisible] = useState(false);

  return (
    <>
      <LinearGradient
        colors={['#197319', '#013055']}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 0 }}
        style={styles.headerContainer}
      >
        <TouchableOpacity style={styles.profileIconContainer}>
          <Ionicons name="person-circle-outline" size={40} color="white" />
        </TouchableOpacity>

        <View style={styles.logoContainer}>
          <Image source={logo} style={{ width: 50, height: 50 }} />
        </View>

        <View style={styles.iconContainer}>
          <NotificationBell onPress={() => setNotificationModalVisible(true)} />
        </View>

        <TouchableOpacity style={styles.iconContainer} onPress={onLogout}>
          <Image source={settings} style={{ width: 30, height: 30 }} />
        </TouchableOpacity>
      </LinearGradient>

      <NotificationModal
        visible={notificationModalVisible}
        onClose={() => setNotificationModalVisible(false)}
      />
    </>
  );
};

const styles = StyleSheet.create({
  headerContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 12,
    position: 'relative',
    height: 90,
  },
  iconContainer: {
    width: 50,
    justifyContent: 'flex-end',
    alignItems: 'flex-end',
  },
  profileIconContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'flex-start',
  },
  logoContainer: {
    position: 'absolute',
    left: 0,
    right: 0,
    top: 30,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
  },
  logoIconTouchable: {
  },
});

export default Header;
