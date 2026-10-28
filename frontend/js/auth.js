const KEYS = {
  user: 'df_user',
  access: 'df_access_token',
  refresh: 'df_refresh_token',
};

export function getSession() {
  const raw = sessionStorage.getItem(KEYS.user);
  try {
    return raw ? JSON.parse(raw) : null;
  } catch {
    logout();
    return null;
  }
}

export function saveSession(auth) {
  const session = {
    userId: auth.userId,
    email: auth.email,
    name: auth.name,
    role: auth.role,
  };
  sessionStorage.setItem(KEYS.user, JSON.stringify(session));
  sessionStorage.setItem(KEYS.access, auth.accessToken);
  sessionStorage.setItem(KEYS.refresh, auth.refreshToken);
  return session;
}

export function getAccessToken() {
  return sessionStorage.getItem(KEYS.access);
}

export function logout() {
  Object.values(KEYS).forEach(key => sessionStorage.removeItem(key));
}

Object.values(KEYS).forEach(key => localStorage.removeItem(key));
