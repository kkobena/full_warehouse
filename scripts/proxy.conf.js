// Proxy dev-server → backend Spring (localhost:9080).
// @angular/build:dev-server (Vite) attend un objet `{ "/context": { target, ... } }`
// — contrairement à l'ancien webpack-dev-server qui acceptait un tableau `{ context: string, ... }`.
// Le dev HTTPS local n'est pas utilisé, donc le flag tls (ancien webpack/proxy.conf.js) est retiré.
const serverResources = ['/api', '/services', '/management', '/v3/api-docs', '/h2-console', '/health'];
const target = 'http://localhost:9080';

module.exports = Object.fromEntries(
  serverResources.map(path => [
    path,
    {
      target,
      secure: false,
      changeOrigin: true,
    },
  ]),
);
