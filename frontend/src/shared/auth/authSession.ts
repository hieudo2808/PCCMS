export type AuthFailureRecord = {
  at: string;
  url?: string;
  method?: string;
  status?: number;
  responseBody?: unknown;
  source: 'api-401' | 'auth-guard' | 'token-init';
  message?: string;
};

type UnauthorizedHandler = () => void;

let unauthorizedHandler: UnauthorizedHandler | null = null;

const STORAGE_KEY = 'pccms:lastAuthFailure';

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null) {
  unauthorizedHandler = handler;
}

export function recordAuthFailure(
  partial: Omit<AuthFailureRecord, 'at'> & { at?: string }
) {
  const record: AuthFailureRecord = {
    at: partial.at ?? new Date().toISOString(),
    ...partial,
  };
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(record));
  console.error('[PCCMS Auth]', record);
}

export function getLastAuthFailure(): AuthFailureRecord | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthFailureRecord) : null;
  } catch {
    return null;
  }
}

export function clearLastAuthFailure() {
  sessionStorage.removeItem(STORAGE_KEY);
}

function clearStoredSession() {
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}

/** Gọi khi API 401 — clear session qua AuthContext, không reload trang. */
export function notifyUnauthorized() {
  if (unauthorizedHandler) {
    unauthorizedHandler();
  } else {
    clearStoredSession();
  }
}
