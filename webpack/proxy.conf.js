function setupProxy({ tls }) {
  const serverResources = ['/api', '/services', '/management', '/v3/api-docs', '/h2-console', '/health'];
  const target = `http${tls ? 's' : ''}://localhost:9080`;

  return serverResources.map(path => ({
    context: path,
    target,
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  }));
}

module.exports = setupProxy;
