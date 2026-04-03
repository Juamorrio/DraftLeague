module.exports = {
  preset: 'jest-expo',
  setupFilesAfterFramework: ['@testing-library/jest-native/extend-expect'],
  transformIgnorePatterns: [
    'node_modules/(?!(jest-)?react-native' +
      '|@react-native(-community)?' +
      '|expo(nent)?' +
      '|@expo(nent)?/.*' +
      '|@expo-google-fonts/.*' +
      '|react-navigation' +
      '|@react-navigation/.*' +
      '|@react-native-async-storage/.*' +
      '|react-native-chart-kit' +
      '|react-native-svg' +
      '|react-native-linear-gradient' +
      '|expo-linear-gradient' +
      ')',
  ],
  testMatch: [
    '**/__tests__/**/*.{js,jsx}',
    '**/*.{spec,test}.{js,jsx}',
  ],
  collectCoverageFrom: [
    'services/**/*.{js,jsx}',
    'components/**/*.{js,jsx}',
    '!**/node_modules/**',
  ],
  coverageThreshold: {
    global: {
      lines: 50,
    },
  },
};
