export class ApiError extends Error {
  constructor(message, status = 0, payload = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

async function request(url, options = {}) {
  let response;
  try {
    response = await fetch(url, withSecurity(options));
  } catch (error) {
    throw new ApiError('Server neodpovídá.', 0, error);
  }

  const contentType = response.headers.get('content-type') || '';
  const payload = response.status === 204
    ? null
    : contentType.includes('application/json')
      ? await response.json()
      : await response.text();

  if (!response.ok) {
    if (response.status === 401 && !url.startsWith('/api/auth/')) {
      window.dispatchEvent(new CustomEvent('cie:authentication-required'));
    }
    const message = payload && typeof payload === 'object'
      ? payload.message || payload.error
      : payload;
    throw new ApiError(message || `Požadavek selhal (${response.status}).`, response.status, payload);
  }
  return payload;
}

function withSecurity(options) {
  const secured = { ...options, credentials: 'same-origin' };
  const method = String(secured.method || 'GET').toUpperCase();
  const headers = new Headers(secured.headers || {});
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const token = readCookie('XSRF-TOKEN');
    if (token) headers.set('X-XSRF-TOKEN', token);
  }
  secured.headers = headers;
  return secured;
}

function readCookie(name) {
  const prefix = `${name}=`;
  const value = document.cookie.split(';').map(item => item.trim()).find(item => item.startsWith(prefix));
  return value ? decodeURIComponent(value.slice(prefix.length)) : '';
}

function withJson(method, body) {
  return {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  };
}

export const api = {
  get: url => request(url),
  form: (url, fields) => request(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
    body: new URLSearchParams(fields).toString()
  }),
  post: (url, body) => request(url, withJson('POST', body)),
  put: (url, body) => request(url, withJson('PUT', body)),
  patch: (url, body) => request(url, withJson('PATCH', body)),
  delete: url => request(url, { method: 'DELETE' }),
  send: (url, method, body, contentType) => request(url, {
    method,
    headers: { 'Content-Type': contentType },
    body
  })
};
