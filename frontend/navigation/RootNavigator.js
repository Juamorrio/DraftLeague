import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { View, Text, StyleSheet, TouchableOpacity, Image } from 'react-native';

// Screens
import Home from '../pages/Home/home';
import Leagues from '../pages/Leagues/leagues';
import Team from '../pages/Teams/team';
import Market from '../pages/Market/market';
import AIInsights from '../pages/AI/aiInsights';
import Admin from '../pages/Admin/admin';
import PlayerStats from '../pages/Player/playerStats';
import Login from '../pages/auth/login';
import Register from '../pages/auth/register';

// Context
import { useLeague } from '../context/LeagueContext';

// Assets (imported as in Layout.jsx)
import footballPidge from '../assets/layout/football-field.png'
import trophy from '../assets/layout/trophy.png'
import cart from '../assets/layout/cart.png'
import robot from '../assets/layout/robot.png'
import footballPidge2 from '../assets/layout/football-field white.png'
import trophy2 from '../assets/layout/trophy white.png'
import cart2 from '../assets/layout/cart white.png'
import robot2 from '../assets/layout/robot white.png'
import home from '../assets/layout/home.png'
import home2 from '../assets/layout/home white.png'
import { Ionicons } from '@expo/vector-icons';

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

function CustomTabBar({ state, descriptors, navigation, isAdmin }) {
    const { selectedLeague } = useLeague();
    
    // Logic from Layout.jsx for dynamic items
    const baseItems = [
        { key: 'Home', label: 'Home', type: 'image', activeSrc: home2, inactiveSrc: home },
        { key: 'Leagues', label: 'Ligas', type: 'image', activeSrc: trophy2, inactiveSrc: trophy },
    ];
    const extraItems = [
        { key: 'Team', label: 'Equipo', type: 'image', activeSrc: footballPidge2, inactiveSrc: footballPidge },
        { key: 'Market', label: 'Mercado', type: 'image', activeSrc: cart2, inactiveSrc: cart },
        { key: 'AIInsights', label: 'IA', type: 'image', activeSrc: robot2, inactiveSrc: robot },
    ];
    const adminItem = { key: 'Admin', label: 'Admin', type: 'icon', icon: 'shield-checkmark' };
    
    // Filter routes based on league selection and admin status
    let displayRoutes = state.routes.filter(route => {
        if (!selectedLeague && ['Team', 'Market', 'AIInsights'].includes(route.name)) return false;
        if (route.name === 'Admin' && !isAdmin) return false;
        if (route.name === 'PlayerStats') return false; // Hidden from tab bar
        return true;
    });

    return (
        <View style={styles.footerWrap}>
            <View style={styles.footer}>
                {displayRoutes.map((route, index) => {
                    const isFocused = state.index === state.routes.findIndex(r => r.name === route.name);
                    
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

                    // Find item config
                    let item = [...baseItems, ...extraItems].find(i => i.key === route.name);
                    if (!item && route.name === 'Admin') item = adminItem;
                    if (!item) item = { label: route.name, type: 'icon', icon: 'help' };

                    return (
                        <TouchableOpacity
                            key={route.name}
                            style={[styles.navItem, isFocused && styles.navItemActive]}
                            activeOpacity={0.8}
                            onPress={onPress}
                        >
                            {item.type === 'icon' ? (
                                <Ionicons 
                                    name={item.icon} 
                                    size={28} 
                                    color={isFocused ? '#ffffff' : '#cbd5e1'} 
                                />
                            ) : (
                                <Image
                                    source={isFocused ? item.activeSrc : item.inactiveSrc}
                                    style={{ width: 40, height: 40, borderRadius: 4 }}
                                />
                            )}
                            <Text style={[styles.label, isFocused && styles.labelActive]}>{item.label}</Text>
                        </TouchableOpacity>
                    );
                })}
            </View>
        </View>
    );
}

import { useNavigation } from '@react-navigation/native';

function MainTabNavigator({ isAdmin }) {
    const { viewUser, setViewUser } = useLeague();
    const navigation = useNavigation();

    React.useEffect(() => {
        if (viewUser) {
            navigation.navigate('Team');
        }
    }, [viewUser]);

    return (
        <Tab.Navigator 
            tabBar={props => <CustomTabBar {...props} isAdmin={isAdmin} />}
            screenOptions={{ headerShown: false }}
        >
            <Tab.Screen name="Home" component={Home} />
            <Tab.Screen name="Team" component={Team} />
            <Tab.Screen name="Market" component={Market} />
            <Tab.Screen name="AIInsights" component={AIInsights} />
            <Tab.Screen name="Leagues" component={Leagues} />
            <Tab.Screen name="Admin" component={Admin} />
            <Tab.Screen name="PlayerStats" component={PlayerStats} />
        </Tab.Navigator>
    );
}

export default function RootNavigator({ authed, authMode, user, onLoggedIn, onRegistered, onSwitchToRegister, onSwitchToLogin }) {
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
                    {props => <MainTabNavigator {...props} isAdmin={isAdmin} />}
                </Stack.Screen>
            )}
        </Stack.Navigator>
    );
}

const styles = StyleSheet.create({
    footerWrap: {
        padding: 12,
        backgroundColor: '#fff',
    },
    footer: {
        height: 72,
        backgroundColor: '#156215',
        borderRadius: 18,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-around',
        paddingHorizontal: 12,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 6 },
        shadowOpacity: 0.12,
        shadowRadius: 12,
        elevation: 8,
    },
    navItem: {
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 8,
        paddingVertical: 6,
        minWidth: 56,
        borderRadius: 12,
    },
    navItemActive: {
        backgroundColor: 'rgba(14,165,164,0.08)',
    },
    label: {
        color: '#94a3b8',
        fontSize: 11,
        marginTop: 4,
        opacity: 0.9,
    },
    labelActive: {
        color: '#ffffff',
        fontWeight: '600',
    },
});
