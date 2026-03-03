const IS_PROD = process.env.APP_VARIANT === 'production';
const IS_PREVIEW = process.env.APP_VARIANT === 'preview';

export default {
  name: IS_PROD ? 'DraftLeague' : IS_PREVIEW ? 'DraftLeague (Preview)' : 'DraftLeague (Dev)',
  slug: 'draftleague',
  owner: 'juanmoreno',
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/header/Logo.png',
  userInterfaceStyle: 'light',
  splash: {
    image: './assets/header/Logo.png',
    backgroundColor: '#013055',
  },
  ios: {
    supportsTablet: true,
    bundleIdentifier: 'com.draftleague.app',
  },
  android: {
    adaptiveIcon: {
      foregroundImage: './assets/header/Logo.png',
      backgroundColor: '#013055',
    },
    package: 'com.draftleague.app',
  },
  web: {
    favicon: './assets/header/Logo.png',
  },
  extra: {
    EXPO_PUBLIC_API_BASE: process.env.EXPO_PUBLIC_API_BASE || 'http://localhost:8080',
    eas: {
      projectId: '0624c8da-b38f-4629-895c-b22eee1200cc',
    },
  },
};
