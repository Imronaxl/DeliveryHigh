// сессия пользователя живёт в localStorage, чтобы страница переживала перезагрузку.
// известный tradeoff: JWT в localStorage уязвим к XSS — для продакшена взял бы
// HttpOnly cookie, но для демо так проще

const KEYS = {
  user: 'df_user',
  access: 'df_access_token',
  refresh: 'df_refresh_token',
};

export function getSession() {
  const raw = localStorage.getItem(KEYS.user);
  try {
    return raw ? JSON.parse(raw) : null;
  } catch {
    logout();
    return null;
  }
}

// сохраняем токены и инфу о пользователе из AuthResponse бэкенда
export function saveSession(auth) {
  const session = {
    userId: auth.userId,
    email: auth.email,
    name: auth.name,
    role: auth.role,
  };
  localStorage.setItem(KEYS.user, JSON.stringify(session));
  localStorage.setItem(KEYS.access, auth.accessToken);
  localStorage.setItem(KEYS.refresh, auth.refreshToken);
  return session;
}

export function getAccessToken() {
  return localStorage.getItem(KEYS.access);
}

export function logout() {
  Object.values(KEYS).forEach(key => localStorage.removeItem(key));
}
