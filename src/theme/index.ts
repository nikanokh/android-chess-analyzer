/**
 * Mobile Design System & Theme Tokens (Android / React Native + Responsive Web)
 * Follows dark-first chess aesthetics, 60-30-10 color rule, and 44-48dp minimum touch targets.
 */

export const CHESS_THEME = {
  dark: {
    bgCanvas: '#161512',
    bgPrimary: '#262421',
    bgSecondary: '#1E1C1A',
    bgElevated: '#302E2A',
    textPrimary: '#F1F1F1',
    textSecondary: '#A8A49E',
    textMuted: '#78746E',
    border: '#363431',
    borderSubtle: '#2C2A27',
    accentGreen: '#779954',
    accentGreenPressed: '#62934B',
    accentBlue: '#36A9E1',
    accentOrange: '#F39C36',
    danger: '#E05252',
    gold: '#A88865',
  },
  light: {
    bgCanvas: '#ECEEDF',
    bgPrimary: '#FFFFFF',
    bgSecondary: '#F5F5F5',
    bgElevated: '#EAECEF',
    textPrimary: '#333333',
    textSecondary: '#6E6E6E',
    textMuted: '#999999',
    border: '#E2E4E8',
    borderSubtle: '#EBEEF2',
    accentGreen: '#779954',
    accentGreenPressed: '#62934B',
    accentBlue: '#36A9E1',
    accentOrange: '#F39C36',
    danger: '#E05252',
    gold: '#A88865',
  },
  touch: {
    minTargetDp: 44,
    primaryCtaHeightDp: 48,
    bottomTabHeightDp: 60,
    topBarHeightDp: 52,
    cardRadiusDp: 14,
    sheetRadiusDp: 20,
  },
} as const;

export type MainTabId = 'home' | 'games' | 'analyzer' | 'repertoire' | 'settings';
