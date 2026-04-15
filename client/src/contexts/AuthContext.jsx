import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import api from '../services/api';

const AuthContext = createContext(null);

const STORAGE_KEY = 'minecraft_manager_session';

/**
 * Keeps the logged-in session in memory and mirrors it into session storage.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [showLogin, setShowLogin] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        const session = JSON.parse(stored);
        const normalized = normalizeSession(session);
        setUser(normalized.user);
        setToken(normalized.token);
      } catch {
        sessionStorage.removeItem(STORAGE_KEY);
      }
    } else {
      setShowLogin(true);
    }
    setLoading(false);
  }, []);

  const saveSession = useCallback((sessionData) => {
    const normalized = normalizeSession(sessionData);
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(normalized));
    setUser(normalized.user);
    setToken(normalized.token);
    setShowLogin(false);
  }, []);

  const login = useCallback(async (username, password) => {
    const response = await api.login(username, password);
    if (response.status === 200 && response.data?.token) {
      saveSession(response.data);
      return { success: true };
    }
    return { success: false, error: response.data?.message || 'Login failed' };
  }, [saveSession]);

  const register = useCallback(async (username, password) => {
    const response = await api.register(username, password);
    if (response.status === 200 && response.data?.token) {
      saveSession(response.data);
      return { success: true };
    }
    return { success: false, error: response.data?.message || 'Registration failed' };
  }, [saveSession]);

  const logout = useCallback(() => {
    sessionStorage.removeItem(STORAGE_KEY);
    setUser(null);
    setToken(null);
    setShowLogin(true);
  }, []);

  const value = {
    user,
    token,
    isAuthenticated: !!token,
    showLogin,
    setShowLogin,
    login,
    register,
    logout,
    loading,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * Returns the current auth context and enforces provider usage.
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}

export default AuthContext;

/**
 * Normalizes older stored session shapes into the current client format.
 */
function normalizeSession(session) {
  if (session?.user && session?.token) {
    return session;
  }

  return {
    token: session?.token ?? null,
    user: session?.username
      ? {
          id: session.userId ?? null,
          username: session.username,
        }
      : null,
  };
}
