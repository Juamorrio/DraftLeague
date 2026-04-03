/**
 * Tests for MarketPlayerCard
 *
 * The Player sub-component makes authenticated fetch calls, so we mock it.
 * The theme module is pure JS and is imported directly.
 */

import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import MarketPlayerCard from '../Market/MarketPlayerCard';

// Mock the Player component — it makes network calls and is tested separately
jest.mock('../player', () => {
  const { View } = require('react-native');
  return function MockPlayer() {
    return <View testID="mock-player" />;
  };
});

// ─── Test data ────────────────────────────────────────────────────────────────

function buildItem(overrides = {}) {
  return {
    id: 1,
    hasBid: false,
    myBid: 0,
    auctionEndTime: new Date(Date.now() + 3_600_000).toISOString(), // 1h from now
    player: {
      fullName: 'Lionel Messi',
      position: 'DEL',
      avatarUrl: null,
      teamId: 100,
      totalPoints: 45,
      marketValue: 12_000_000,
    },
    ...overrides,
  };
}

const formatNumber = (n) => n.toLocaleString('es-ES');

// ─── Render tests ─────────────────────────────────────────────────────────────

describe('MarketPlayerCard — render', () => {
  test('muestra el nombre del jugador', () => {
    const { getByText } = render(
      <MarketPlayerCard
        item={buildItem()}
        onViewStats={jest.fn()}
        onBid={jest.fn()}
        onCancelBid={jest.fn()}
        formatNumber={formatNumber}
      />
    );
    expect(getByText('Lionel Messi')).toBeTruthy();
  });

  test('muestra el valor de mercado con el texto "valor de mercado"', () => {
    const { getByText } = render(
      <MarketPlayerCard
        item={buildItem()}
        onViewStats={jest.fn()}
        onBid={jest.fn()}
        onCancelBid={jest.fn()}
        formatNumber={formatNumber}
      />
    );
    expect(getByText(/valor de mercado/)).toBeTruthy();
  });

  test('muestra el badge de posición', () => {
    const { getByText } = render(
      <MarketPlayerCard
        item={buildItem()}
        onViewStats={jest.fn()}
        onBid={jest.fn()}
        onCancelBid={jest.fn()}
        formatNumber={formatNumber}
      />
    );
    expect(getByText('DEL')).toBeTruthy();
  });

  test('subasta activa → muestra "Cierra en"', () => {
    const { getByText } = render(
      <MarketPlayerCard
        item={buildItem()}
        onViewStats={jest.fn()}
        onBid={jest.fn()}
        onCancelBid={jest.fn()}
        formatNumber={formatNumber}
      />
    );
    expect(getByText(/Cierra en/)).toBeTruthy();
  });

  test('subasta expirada → muestra "Subasta finalizada"', () => {
    const expired = buildItem({
      auctionEndTime: new Date(Date.now() - 1000).toISOString(),
    });
    const { getByText } = render(
      <MarketPlayerCard
        item={expired}
        onViewStats={jest.fn()}
        onBid={jest.fn()}
        onCancelBid={jest.fn()}
        formatNumber={formatNumber}
      />
    );
    expect(getByText('Subasta finalizada')).toBeTruthy();
  });
});

// ─── Interaction tests ────────────────────────────────────────────────────────

describe('MarketPlayerCard — interacciones', () => {
  test('pulsar la card llama onViewStats con el player', () => {
    const onViewStats = jest.fn();
    const item = buildItem();
    const { getByText } = render(
      <MarketPlayerCard
        item={item}
        onViewStats={onViewStats}
        onBid={jest.fn()}
        onCancelBid={jest.fn()}
        formatNumber={formatNumber}
      />
    );
    fireEvent.press(getByText('Ver stats →'));
    expect(onViewStats).toHaveBeenCalledWith(item.player);
  });

  test('sin puja activa → botón "Hacer puja" visible', () => {
    const { getByText } = render(
      <MarketPlayerCard
        item={buildItem({ hasBid: false })}
        onViewStats={jest.fn()}
        onBid={jest.fn()}
        onCancelBid={jest.fn()}
        formatNumber={formatNumber}
      />
    );
    expect(getByText('Hacer puja')).toBeTruthy();
  });

  test('con puja activa → botón "Cancelar puja" visible', () => {
    const { getByText } = render(
      <MarketPlayerCard
        item={buildItem({ hasBid: true, myBid: 5_000_000 })}
        onViewStats={jest.fn()}
        onBid={jest.fn()}
        onCancelBid={jest.fn()}
        formatNumber={formatNumber}
      />
    );
    expect(getByText('Cancelar puja')).toBeTruthy();
  });

  test('pulsar "Hacer puja" llama onBid con el item completo', () => {
    const onBid = jest.fn();
    const item = buildItem();
    const { getByText } = render(
      <MarketPlayerCard
        item={item}
        onViewStats={jest.fn()}
        onBid={onBid}
        onCancelBid={jest.fn()}
        formatNumber={formatNumber}
      />
    );
    fireEvent.press(getByText('Hacer puja'));
    expect(onBid).toHaveBeenCalledWith(item);
  });

  test('pulsar "Cancelar puja" llama onCancelBid con el id del item', () => {
    const onCancelBid = jest.fn();
    const item = buildItem({ hasBid: true, myBid: 3_000_000 });
    const { getByText } = render(
      <MarketPlayerCard
        item={item}
        onViewStats={jest.fn()}
        onBid={jest.fn()}
        onCancelBid={onCancelBid}
        formatNumber={formatNumber}
      />
    );
    fireEvent.press(getByText('Cancelar puja'));
    expect(onCancelBid).toHaveBeenCalledWith(item.id);
  });
});
