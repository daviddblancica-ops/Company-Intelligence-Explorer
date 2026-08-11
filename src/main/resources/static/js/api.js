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
    response = await fetch(url, options);
  } catch (error) {
    throw new ApiError('Server neodpovida.', 0, error);
  }

  const contentType = response.headers.get('content-type') || '';
  const payload = response.status === 204
    ? null
    : contentType.includes('application/json')
      ? await response.json()
      : await response.text();

  if (!response.ok) {
    const message = payload && typeof payload === 'object'
      ? payload.message || payload.error
      : payload;
    throw new ApiError(message || `Pozadavek selhal (${response.status}).`, response.status, payload);
  }
  return payload;
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
