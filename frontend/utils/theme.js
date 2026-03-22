/**
 * DraftLeague Design System
 * Single source of truth for all visual tokens.
 */

// ─── COLOUR PALETTE ────────────────────────────────────────────────────────
export const colors = {
  // Brand greens
  primary:        '#16A34A', // emerald-600 — primary CTA / active states
  primaryDark:    '#15803D', // emerald-700 — pressed states / header
  primaryDeep:    '#052E16', // emerald-950 — nav background / gradients
  primaryLight:   '#DCFCE7', // emerald-100 — subtle tinted backgrounds
  primaryMuted:   '#BBF7D0', // emerald-200 — borders on tinted surfaces

  // Gradient (header / auth screens)
  gradientStart:  '#15803D',
  gradientEnd:    '#052E16',

  // Accent — stays teal for active/selection indicators
  accent:         '#14B8A6', // teal-500
  accentBg:       'rgba(20,184,166,0.10)',

  // Semantic colours
  danger:         '#EF4444', // red-500 — destructive / error
  dangerDark:     '#DC2626', // red-600 — pressed danger
  dangerBg:       '#FEF2F2', // red-50
  warning:        '#F59E0B', // amber-500 — captain badge / warnings
  warningDark:    '#B45309', // amber-700 — active bid label / pressed warning
  warningDeep:    '#92400E', // amber-800 — active bid amount
  warningBg:      '#FFFBEB', // amber-50
  success:        '#16A34A', // same as primary — points / success states
  successBg:      '#F0FDF4', // green-50
  purple:         '#8B5CF6', // violet-500 — trade offers
  purpleBg:       '#F5F3FF', // violet-50

  // Neutrals
  bgApp:          '#F8FAFC', // slate-50 — screen background
  bgCard:         '#FFFFFF', // white — card surfaces
  bgSubtle:       '#F1F5F9', // slate-100 — subtle sections
  bgInput:        '#FFFFFF',

  border:         '#E2E8F0', // slate-200
  borderStrong:   '#CBD5E1', // slate-300

  textPrimary:    '#0F172A', // slate-900
  textSecondary:  '#475569', // slate-600
  textMuted:      '#94A3B8', // slate-400
  textInverse:    '#FFFFFF',
  textOnGreen:    '#FFFFFF',

  // Notification
  notif:          '#EF4444',

  // Overlay
  overlay:        'rgba(0,0,0,0.40)',
  overlayLight:   'rgba(0,0,0,0.20)',
};

// ─── TYPOGRAPHY ────────────────────────────────────────────────────────────
export const fontSize = {
  xs:   11,
  sm:   13,
  md:   15,
  lg:   18,
  xl:   22,
  '2xl': 28,
  '3xl': 34,
};

export const fontWeight = {
  regular:     '400',
  medium:      '500',
  semibold:    '600',
  bold:        '700',
  extrabold:   '800',
  black:       '900',
};

export const lineHeight = {
  tight:  1.2,
  normal: 1.4,
  loose:  1.6,
};

// ─── SPACING (4 pt grid) ──────────────────────────────────────────────────
export const spacing = {
  xs:   4,
  sm:   8,
  md:   12,
  lg:   16,
  xl:   20,
  '2xl': 24,
  '3xl': 32,
  '4xl': 48,
};

// ─── BORDER RADIUS ────────────────────────────────────────────────────────
export const radius = {
  sm:   8,
  md:   12,
  lg:   16,
  xl:   20,
  pill: 999,
};

// ─── SHADOWS ─────────────────────────────────────────────────────────────
export const shadow = {
  sm: {
    shadowColor: '#0F172A',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 3,
    elevation: 2,
  },
  md: {
    shadowColor: '#0F172A',
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.09,
    shadowRadius: 8,
    elevation: 4,
  },
  lg: {
    shadowColor: '#0F172A',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.12,
    shadowRadius: 16,
    elevation: 8,
  },
};

// ─── FONT FAMILIES ──────────────────────────────────────────────────────
// Loaded via useFonts in App.js — make sure fonts are loaded before using.
export const fontFamily = {
  // Barlow Condensed — numbers, headings, scores, market values
  displayBold:    'BarlowCondensed_700Bold',
  displaySemi:    'BarlowCondensed_600SemiBold',
  // Barlow — body text, labels, UI copy
  body:           'Barlow_400Regular',
  bodyMedium:     'Barlow_500Medium',
  bodySemibold:   'Barlow_600SemiBold',
  bodyBold:       'Barlow_700Bold',
};

// ─── COMPONENT PRESETS ───────────────────────────────────────────────────
export const card = {
  backgroundColor: colors.bgCard,
  borderRadius: radius.lg,
  padding: spacing.lg,
  ...shadow.sm,
};

export const input = {
  backgroundColor: colors.bgInput,
  borderWidth: 1,
  borderColor: colors.border,
  borderRadius: radius.md,
  height: 48,
  paddingHorizontal: spacing.lg,
  fontSize: fontSize.md,
  color: colors.textPrimary,
};

export const buttonPrimary = {
  backgroundColor: colors.primary,
  borderRadius: radius.pill,
  height: 48,
  alignItems: 'center',
  justifyContent: 'center',
  paddingHorizontal: spacing['2xl'],
};

export const buttonPrimaryText = {
  color: colors.textInverse,
  fontSize: fontSize.md,
  fontWeight: fontWeight.bold,
  letterSpacing: 0.3,
};

export const buttonSecondary = {
  backgroundColor: 'transparent',
  borderRadius: radius.pill,
  height: 44,
  alignItems: 'center',
  justifyContent: 'center',
  paddingHorizontal: spacing.xl,
  borderWidth: 1.5,
  borderColor: colors.border,
};

export const buttonSecondaryText = {
  color: colors.textSecondary,
  fontSize: fontSize.sm,
  fontWeight: fontWeight.semibold,
};

export const sectionTitle = {
  fontSize: fontSize.lg,
  fontWeight: fontWeight.bold,
  color: colors.textPrimary,
};

export const label = {
  fontSize: fontSize.sm,
  fontWeight: fontWeight.semibold,
  color: colors.textSecondary,
  letterSpacing: 0.5,
  textTransform: 'uppercase',
};

// ─── POSITION COLOURS (bar / dot accent) ─────────────────────────────────
export const positionColors = {
  POR: '#F59E0B', // amber
  GK:  '#F59E0B',
  DEF: '#3B82F6', // blue
  MID: '#16A34A', // green
  DEL: '#EF4444', // red
  FWD: '#EF4444',
};

// ─── POSITION BADGE COLOURS (full badge: bg, text, border, bar) ──────────
// Shared by playerStats.js, admin.js, aiInsights.js, market.js, etc.
// Usage: const badge = positionBadgeColors[player.position] ?? positionBadgeColors.MID;
export const positionBadgeColors = {
  POR:   { bar: '#F59E0B', bg: '#FEF3C7', text: '#92400E', border: '#FDE68A' },
  GK:    { bar: '#F59E0B', bg: '#FEF3C7', text: '#92400E', border: '#FDE68A' },
  DEF:   { bar: '#3B82F6', bg: '#DBEAFE', text: '#1E40AF', border: '#BFDBFE' },
  MID:   { bar: '#16A34A', bg: '#DCFCE7', text: '#14532D', border: '#BBF7D0' },
  DEL:   { bar: '#EF4444', bg: '#FEE2E2', text: '#991B1B', border: '#FECACA' },
  FWD:   { bar: '#EF4444', bg: '#FEE2E2', text: '#991B1B', border: '#FECACA' },
  COACH: { bar: '#94A3B8', bg: '#F1F5F9', text: '#475569', border: '#E2E8F0' },
};

// ─── MARKET / AUCTION POSITION COLOURS ───────────────────────────────────
// Violet for MID to visually differentiate from the team/stats green.
// Used exclusively by market components (MarketPlayerCard, BidModal).
export const marketPositionColors = {
  POR:   { bg: '#FEF3C7', text: '#B45309', border: '#FCD34D' },
  GK:    { bg: '#FEF3C7', text: '#B45309', border: '#FCD34D' },
  DEF:   { bg: '#DBEAFE', text: '#1D4ED8', border: '#93C5FD' },
  MID:   { bg: '#EDE9FE', text: '#6D28D9', border: '#C4B5FD' },
  DEL:   { bg: '#FEE2E2', text: '#DC2626', border: '#FCA5A5' },
  FWD:   { bg: '#FEE2E2', text: '#DC2626', border: '#FCA5A5' },
  COACH: { bg: '#F1F5F9', text: '#475569', border: '#E2E8F0' },
};

// ─── HELPERS ────────────────────────────────────────────────────────────
export const withOpacity = (hex, opacity) => {
  const num = Math.round(opacity * 255).toString(16).padStart(2, '0');
  return `${hex}${num}`;
};
