import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import { jwtDecode } from 'jwt-decode';
import type { UserResponse } from '../../../types';
import {
  setUnauthorizedHandler,
  recordAuthFailure,
  clearLastAuthFailure,
} from '~/shared/auth/authSession';
import { getAccessToken } from '~/shared/auth/tokenStorage';

interface AuthContextType {
  isAuthenticated: boolean;
  isInitializing: boolean;
  user: UserResponse | null;
  login: (token: string, refreshToken: string | null, user: UserResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function clearStoredSession() {
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true);

  const logout = useCallback(() => {
    clearStoredSession();
    setUser(null);
    setIsAuthenticated(false);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, [logout]);

  useEffect(() => {
    const syncFromStorage = () => {
      if (!getAccessToken() && user) {
        logout();
      }
    };
    window.addEventListener('storage', syncFromStorage);
    return () => window.removeEventListener('storage', syncFromStorage);
  }, [user, logout]);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');

    if (!token || !storedUser) {
      clearStoredSession();
      setIsInitializing(false);
      return;
    }

    try {
      const decoded = jwtDecode<{ exp?: number }>(token);
      const isExpired = decoded.exp != null && decoded.exp * 1000 < Date.now();
      if (isExpired) {
        throw new Error('Token expired');
      }
      setUser(JSON.parse(storedUser) as UserResponse);
      setIsAuthenticated(true);
    } catch (error) {
      recordAuthFailure({
        source: 'token-init',
        message:
          error instanceof Error ? error.message : 'Token hoặc user trong localStorage không hợp lệ',
      });
      clearStoredSession();
    } finally {
      setIsInitializing(false);
    }
  }, []);

  const login = (token: string, refreshToken: string | null, userData: UserResponse) => {
    clearLastAuthFailure();
    localStorage.setItem('token', token);
    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken);
    }
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    setIsAuthenticated(true);
  };

  if (isInitializing) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 text-sm text-slate-600">
        Đang khôi phục phiên đăng nhập…
      </div>
    );
  }

  return (
    <AuthContext.Provider value={{ isAuthenticated, isInitializing, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
