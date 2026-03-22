import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

// Screens
import Home from '../pages/Home/home';
import Leagues from '../pages/Leagues/leagues';
import Team from '../pages/Teams/team';
import Market from '../pages/Market/market';
import AIInsights from '../pages/AI/aiInsights';
import Admin from '../pages/Admin/admin';
import PlayerStats from '../pages/Player/playerStats';
import PlayerComparator from '../pages/Player/playerComparator';
import Profile from '../pages/Profile/profile';
import Login from '../pages/auth/login';
import Register from '../pages/auth/register';

// Context
import { useLeague } from '../context/LeagueContext';
import { fontFamily } from '../utils/theme';

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

function CustomTabBar({ state, descriptors, navigation, isAdmin }) {
    const { selectedLeague } = useLeague();
    const insets = useSafeAreaInsets();
    
    const allItems = [
        { key: 'Home',       label: 'Home',    icon: 'home',      iconOut: 'home-outline' },
        { key: 'Leagues',    label: 'Ligas',   icon: 'trophy',    iconOut: 'trophy-outline' },
        { key: 'Team',       label: 'Equipo',  icon: 'football',  iconOut: 'football-outline' },
        { key: 'Market',     label: 'Mercado', icon: 'cart',      iconOut: 'cart-outline' },
        { key: 'AIInsights', label: 'Análisis', icon: 'analytics', iconOut: 'analytics-outline' },
        { key: 'Admin',      label: 'Admin',   icon: 'shield-checkmark', iconOut: 'shield-checkmark-outline' },
    ];

    const displayRoutes = state.routes.filter(route => {
        if (!selectedLeague && ['Team', 'Market', 'AIInsights'].includes(route.name)) return false;
        if (route.name === 'Admin' && !isAdmin) return false;
        if (['PlayerStats', 'PlayerComparator', 'Profile'].includes(route.name)) return false;
        return true;
    });

    return (
        <View style={[styles.footerWrap, { paddingBottom: Math.max(insets.bottom, 12) }]}>
            <View style={styles.footer}>
                {displayRoutes.map((route) => {
                    const isFocused = state.index === state.routes.findIndex(r => r.name === route.name);
                    const item = allItems.find(i => i.key === route.name) || { label: route.name, icon: 'help', iconOut: 'help-outline' };

                    const onPress = () => {
                        const event = navigation.emit({
                            type: 'tabPress',
                            target: route.key,
                            canPreventDefault: true,
                        });
                        if (!isFocused && !event.defaultPrevented) {
                            navigation.navigate(route.name);
                        }
                    };

                    return (
                        <TouchableOpacity
                            key={route.name}
                            style={[styles.navItem, isFocused && styles.navItemActive]}
                            activeOpacity={0.75}
                            onPress={onPress}
                        >
                            <Ionicons
                                name={isFocused ? item.icon : item.iconOut}
                                size={24}
                                color={isFocused ? '#14B8A6' : 'rgba(255,255,255,0.45)'}
                            />
                            <Text style={[styles.label, isFocused && styles.labelActive]}>{item.label}</Text>
                        </TouchableOpacity>
                    );
                })}
            </View>
        </View>
    );
}

function MainTabNavigator({ isAdmin, onLogout }) {
    return (
        <Tab.Navigator
            tabBar={props => <CustomTabBar {...props} isAdmin={isAdmin} />}
            screenOptions={{ headerShown: false }}
            backBehavior="history"
        >
            <Tab.Screen name="Home" component={Home} />
            <Tab.Screen name="Team" component={Team} />
            <Tab.Screen name="Market" component={Market} />
            <Tab.Screen name="AIInsights" component={AIInsights} />
            <Tab.Screen name="Leagues" component={Leagues} />
            <Tab.Screen name="Profile">
                {props => <Profile {...props} onLogout={onLogout} />}
            </Tab.Screen>
            <Tab.Screen name="Admin" component={Admin} />
            <Tab.Screen name="PlayerStats" component={PlayerStats} />
            <Tab.Screen name="PlayerComparator" component={PlayerComparator} />
        </Tab.Navigator>
    );
}

export default function RootNavigator({ authed, authMode, user, onLoggedIn, onRegistered, onLogout, onSwitchToRegister, onSwitchToLogin }) {
    const isAdmin = user?.role === 'ADMIN';
    return (
        <Stack.Navigator screenOptions={{ headerShown: false }}>
            {!authed ? (
                <>
                    {authMode === 'login' ? (
                        <Stack.Screen name="Login">
                            {props => <Login {...props} onLoggedIn={onLoggedIn} onSwitchToRegister={onSwitchToRegister} />}
                        </Stack.Screen>
                    ) : (
                        <Stack.Screen name="Register">
                            {props => <Register {...props} onRegistered={onRegistered} onSwitchToLogin={onSwitchToLogin} />}
                        </Stack.Screen>
                    )}
                </>
            ) : (
                <Stack.Screen name="Main">
                    {props => <MainTabNavigator {...props} isAdmin={isAdmin} onLogout={onLogout} />}
                </Stack.Screen>
            )}
        </Stack.Navigator>
    );
}

const styles = StyleSheet.create({
    footerWrap: {
        paddingHorizontal: 12,
        paddingTop: 8,
        backgroundColor: '#F8FAFC',
    },
    footer: {
        height: 68,
        backgroundColor: '#052E16',
        borderRadius: 20,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-around',
        paddingHorizontal: 8,
        shadowColor: '#052E16',
        shadowOffset: { width: 0, height: 8 },
        shadowOpacity: 0.30,
        shadowRadius: 16,
        elevation: 12,
    },
    navItem: {
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 10,
        paddingVertical: 8,
        minWidth: 52,
        borderRadius: 14,
        gap: 3,
    },
    navItemActive: {
        backgroundColor: 'rgba(20,184,166,0.15)',
    },
    label: {
        color: 'rgba(255,255,255,0.45)',
        fontSize: 10,
        fontFamily: 'Barlow_600SemiBold',
        letterSpacing: 0.3,
    },
    labelActive: {
        color: '#14B8A6',
        fontFamily: 'Barlow_700Bold',
    },
});
