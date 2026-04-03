/**
 * Tests for the withAuth HOC.
 *
 * Strategy:
 * - Mock tryRefreshOnLaunch from authService so we control the auth outcome
 * - Mock the Login page since it's a complex screen with its own dependencies
 */

import React from 'react';
import { Text, View } from 'react-native';
import { render, waitFor } from '@testing-library/react-native';

jest.mock('../../services/authService', () => ({
  tryRefreshOnLaunch: jest.fn(),
}));

jest.mock('../../pages/auth/login', () => {
  const { View, Text } = require('react-native');
  return function MockLogin({ onLoggedIn }) {
    return (
      <View>
        <Text testID="login-screen">Login</Text>
      </View>
    );
  };
});

import { tryRefreshOnLaunch } from '../../services/authService';
import withAuth from '../withAuth';

const ProtectedScreen = () => (
  <View>
    <Text testID="protected-screen">Contenido protegido</Text>
  </View>
);

describe('withAuth HOC', () => {
  beforeEach(() => jest.clearAllMocks());

  test('muestra null mientras carga (antes de que tryRefreshOnLaunch resuelva)', () => {
    // Never resolves during this test
    tryRefreshOnLaunch.mockReturnValue(new Promise(() => {}));

    const Guard = withAuth(ProtectedScreen);
    const { queryByTestId } = render(<Guard />);

    expect(queryByTestId('protected-screen')).toBeNull();
    expect(queryByTestId('login-screen')).toBeNull();
  });

  test('muestra el componente envuelto cuando el usuario está autenticado', async () => {
    tryRefreshOnLaunch.mockResolvedValue(true);

    const Guard = withAuth(ProtectedScreen);
    const { findByTestId } = render(<Guard />);

    const screen = await findByTestId('protected-screen');
    expect(screen).toBeTruthy();
  });

  test('muestra Login cuando el usuario no está autenticado', async () => {
    tryRefreshOnLaunch.mockResolvedValue(false);

    const Guard = withAuth(ProtectedScreen);
    const { findByTestId } = render(<Guard />);

    const loginScreen = await findByTestId('login-screen');
    expect(loginScreen).toBeTruthy();
  });
});
