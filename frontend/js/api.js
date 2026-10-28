import { API_BASE } from './config.js';
import { getAccessToken, saveSession, logout } from './auth.js';

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function parseError(response) {
  try {
    const body = await response.json();
    return body.message || response.statusText;
  } catch {
    return response.statusText;
  }
}

let refreshing = null;

export async function request(path, options = {}, retried = false) {
  const headers = { ...(options.headers || {}) };
  if (options.body) headers['Content-Type'] = 'application/json';

  const token = getAccessToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const response = await fetch(API_BASE + path, {
    method: options.method || 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  if (response.status === 401 && !path.startsWith('/api/v1/auth/')) {
    if (!retried && await refreshSession()) {
      return request(path, options, true); // повторяем с новым токеном
    }
    logout();
    location.hash = '#/login';
    throw new ApiError(401, 'Сессия истекла, войдите заново');
  }

  if (!response.ok) {
    throw new ApiError(response.status, await parseError(response));
  }

  if (response.status === 204) return null;
  return response.json();
}

async function tryRefresh() {
  const refreshToken = sessionStorage.getItem('df_refresh_token');
  if (!refreshToken) return false;

  try {
    const response = await fetch(API_BASE + '/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!response.ok) return false;
    saveSession(await response.json());
    return true;
  } catch {
    return false;
  }
}

export async function refreshSession() {
  if (!refreshing) refreshing = tryRefresh().finally(() => { refreshing = null; });
  return refreshing;
}
