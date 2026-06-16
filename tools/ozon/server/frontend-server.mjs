import fs from 'node:fs';
import path from 'node:path';
import http from 'node:http';
import https from 'node:https';
import { URL } from 'node:url';

const port = Number(process.env.PORT || 8084);
const distDir = path.resolve(process.env.DIST_DIR || path.join(process.cwd(), 'dist'));
const gatewayOrigin = process.env.GATEWAY_ORIGIN || 'http://127.0.0.1:8099';
const ozonOrigin = process.env.OZON_ORIGIN || gatewayOrigin;
const staticOrigin = process.env.STATIC_ORIGIN || '';
const gatewayUrl = new URL(gatewayOrigin);
const ozonUrl = new URL(ozonOrigin);
const staticUrl = staticOrigin ? new URL(staticOrigin) : null;

const proxyPrefixes = [
  '/api',
  '/admin/api',
  '/erp/api',
  '/amazon/api',
  '/amazonadv/api',
  '/ozon/api',
  '/quote/api',
  '/mdata/api',
  '/finance/api',
  '/code/gen',
];

const contentTypes = new Map([
  ['.html', 'text/html; charset=utf-8'],
  ['.js', 'application/javascript; charset=utf-8'],
  ['.mjs', 'application/javascript; charset=utf-8'],
  ['.css', 'text/css; charset=utf-8'],
  ['.json', 'application/json; charset=utf-8'],
  ['.svg', 'image/svg+xml'],
  ['.png', 'image/png'],
  ['.jpg', 'image/jpeg'],
  ['.jpeg', 'image/jpeg'],
  ['.gif', 'image/gif'],
  ['.webp', 'image/webp'],
  ['.ico', 'image/x-icon'],
  ['.woff', 'font/woff'],
  ['.woff2', 'font/woff2'],
  ['.ttf', 'font/ttf'],
  ['.txt', 'text/plain; charset=utf-8'],
]);

function shouldProxy(urlPath) {
  return proxyPrefixes.some((prefix) => urlPath === prefix || urlPath.startsWith(`${prefix}/`));
}

function upstreamForPath(urlPath) {
  if (urlPath === '/ozon/api' || urlPath.startsWith('/ozon/api/')) {
    return ozonUrl;
  }
  return gatewayUrl;
}

function sendFile(res, filePath) {
  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('Not found');
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    res.writeHead(200, {
      'Content-Type': contentTypes.get(ext) || 'application/octet-stream',
      'Cache-Control': ext === '.html' ? 'no-cache' : 'public, max-age=31536000, immutable',
    });
    res.end(data);
  });
}

function proxyToUpstream(req, res, upstream) {
  const transport = upstream.protocol === 'https:' ? https : http;
  const headers = { ...req.headers, host: upstream.host };

  const upstreamReq = transport.request(
    {
      protocol: upstream.protocol,
      hostname: upstream.hostname,
      port: upstream.port || (upstream.protocol === 'https:' ? 443 : 80),
      method: req.method,
      path: req.url,
      headers,
    },
    (upstreamRes) => {
      res.writeHead(upstreamRes.statusCode || 502, upstreamRes.headers);
      upstreamRes.pipe(res);
    }
  );

  upstreamReq.on('error', (error) => {
    res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(
      JSON.stringify({
        code: 502,
        msg: `Gateway proxy failed: ${error.message}`,
      })
    );
  });

  req.pipe(upstreamReq);
}

function serveSpa(req, res, urlPath) {
  const normalized = decodeURIComponent(urlPath.split('?')[0]);
  const requestedPath = normalized === '/' ? '/index.html' : normalized;
  const safePath = path.normalize(requestedPath).replace(/^(\.\.[/\\])+/, '');
  const absolutePath = path.join(distDir, safePath);

  if (absolutePath.startsWith(distDir) && fs.existsSync(absolutePath) && fs.statSync(absolutePath).isFile()) {
    sendFile(res, absolutePath);
    return;
  }

  if (fs.existsSync(path.join(distDir, 'index.html'))) {
    sendFile(res, path.join(distDir, 'index.html'));
    return;
  }

  if (staticUrl) {
    proxyToUpstream(req, res, staticUrl);
    return;
  }

  sendFile(res, path.join(distDir, 'index.html'));
}

function proxyRequest(req, res) {
  const urlPath = (req.url || '/').split('?')[0];
  const upstream = upstreamForPath(urlPath);
  proxyToUpstream(req, res, upstream);
}

const server = http.createServer((req, res) => {
  if (!req.url) {
    res.writeHead(400, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Bad request');
    return;
  }

  const urlPath = req.url.split('?')[0];
  if (shouldProxy(urlPath)) {
    proxyRequest(req, res);
    return;
  }

  serveSpa(req, res, urlPath);
});

server.listen(port, '0.0.0.0', () => {
  console.log(
    `frontend server listening on :${port}, dist=${distDir}, static=${staticOrigin || 'none'}, gateway=${gatewayOrigin}, ozon=${ozonOrigin}`
  );
});
