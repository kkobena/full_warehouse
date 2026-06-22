
const packageVersion = require('../package.json').version;

module.exports = {
  I18N_HASH: 'generated_hash',
  SERVER_API_URL: '',
  __VERSION__: process.env.APP_VERSION || packageVersion || 'DEV',
};
