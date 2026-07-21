const {pathsToModuleNameMapper} = require('ts-jest');

const {
  compilerOptions: {paths = {}, baseUrl = './'},
} = require('./tsconfig.json');

module.exports = {
  transformIgnorePatterns: ['node_modules/(?!.*\\.mjs$|dayjs/esm)'],
  globals: {
    __VERSION__: 'test',
  },
  roots: ['<rootDir>', `<rootDir>/${baseUrl}`],
  modulePaths: [`<rootDir>/${baseUrl}`],
  // `@angular/localize/init` : certains composants @ng-bootstrap (ngb-pagination…)
  // appellent `$localize` pour leurs libellés ARIA. Le build Angular inline ces appels,
  // pas Jest — sans ce polyfill, leur seule instanciation lève `$localize is not defined`.
  setupFiles: ['jest-date-mock', '@angular/localize/init'],
  cacheDirectory: '<rootDir>/target/jest-cache',
  coverageDirectory: '<rootDir>/target/test-results/',
  moduleNameMapper: pathsToModuleNameMapper(paths,
    {prefix: `<rootDir>/${baseUrl}/`}),
  reporters: [
    'default',
    ['jest-junit', {
      outputDirectory: '<rootDir>/target/test-results/',
      outputName: 'TESTS-results-jest.xml'
    }],
  ],
  testMatch: ['<rootDir>/pharmaSmart-app/src/main/webapp/app/**/@(*.)@(spec.ts)'],
  testEnvironmentOptions: {},
};
