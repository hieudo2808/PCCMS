export function getAccessToken(): string | null {
  return localStorage.getItem('token');
}

export function hasAccessToken(): boolean {
  const token = getAccessToken();
  return Boolean(token && token.length > 10);
}
