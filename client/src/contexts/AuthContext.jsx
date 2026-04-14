import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import api from '../services/api';

const AuthContext = createContext(null);

const STORAGE_KEY = 'minecraft_manager_session';

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
        setUser(session.user);
        setToken(session.token);
      } catch {
        sessionStorage.removeItem(STORAGE_KEY);
      }
    } else {
      setShowLogin(true);
    }
    setLoading(false);
  }, []);

  const saveSession = useCallback((sessionData) => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(sessionData));
    setUser(sessionData.user);
    setToken(sessionData.token);
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

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}

export default AuthContext;