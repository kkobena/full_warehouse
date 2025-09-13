const path = require('path');
const fs = require('fs');
const webpack = require('webpack');
const { merge } = require('webpack-merge');
const { hashElement } = require('folder-hash');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const { BundleAnalyzerPlugin } = require('webpack-bundle-analyzer');
const WebpackNotifierPlugin = require('webpack-notifier');
const MergeJsonWebpackPlugin = require('merge-jsons-webpack-plugin');

const environment = require('./environment');
const proxyConfig = require('./proxy.conf');

module.exports = async config => {
  const languagesHash = await hashElement(path.resolve(__dirname, '../src/main/webapp/i18n'), {
    algo: 'md5',
    encoding: 'hex',
    files: { include: ['*.json'] },
  });

  const isProd = config.mode === 'production';

  // --- Notifications de build en dev ---
  if (!isProd) {
    config.plugins.push(
      new WebpackNotifierPlugin({
        title: 'Warehouse',
        contentImage: path.join(__dirname, 'logo.png'),
      }),
    );
  }

  // --- Proxy backend pour devServer ---
  if (config.devServer) {
    const tls = config.devServer?.server?.type === 'https';
    config.devServer.proxy = proxyConfig({ tls });
    config.devServer.hot = true;
    config.devServer.liveReload = true;
  }

  // --- Analyse du bundle (prod uniquement) ---
  if (isProd) {
    config.plugins.push(
      new BundleAnalyzerPlugin({
        analyzerMode: 'static',
        openAnalyzer: false,
        reportFilename: '../../stats.html',
      }),
    );
  }

  // --- Copier les assets statiques ---
  const patterns = [];

  config.plugins.push(
    new MergeJsonWebpackPlugin({
      output: {
        groupBy: [
          {
            pattern: './src/main/webapp/i18n/fr/*.json',
            fileName: './i18n/fr.json',
          },
          {
            pattern: './src/main/webapp/i18n/en/*.json',
            fileName: './i18n/en.json',
          },
          // Ajouter d'autres langues si nécessaire
        ],
      },
    }),
  );

  // images
  /*const imagesPath = path.resolve(__dirname, '../src/main/webapp/content/images');
  if (fs.existsSync(imagesPath)) {
    patterns.push({
      from: imagesPath,
      to: 'images/',
      noErrorOnMissing: true,
    });
  }*/

  if (patterns.length > 0) {
    config.plugins.push(new CopyWebpackPlugin({ patterns }));
  }

  // --- Définir les variables d’environnement globales ---
  config.plugins.push(
    new webpack.DefinePlugin({
      I18N_HASH: JSON.stringify(languagesHash.hash),
      __VERSION__: JSON.stringify(environment.__VERSION__),
      SERVER_API_URL: JSON.stringify(environment.SERVER_API_URL),
    }),
  );

  // --- Merge final, extensions personnalisées possibles ---
  return merge(config, {});
};
