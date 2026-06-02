import { createContext, useContext, useState, useEffect } from 'react';
import { authAPI } from '../api/client';

const AuthContext = createContext();

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within AuthProvider');
    }
    return context;
};

export const AuthProvider = ({ children }) => {
    const [currentUser, setCurrentUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const checkSession = async () => {
            try {
                const storedUser = localStorage.getItem('user');
                if (storedUser && storedUser !== 'undefined' && storedUser !== 'null') {
                    let parsedUser;
                    try {
                        parsedUser = JSON.parse(storedUser);
                    } catch {
                        localStorage.removeItem('user');
                        return;
                    }
                    setCurrentUser(parsedUser);

                    try {
                        await authAPI.getCurrentUser();
                    } catch (err) {
                        console.log('Session invalid, logging out...', err);
                        logout();
                    }
                }
            } catch (error) {
                console.error('Auth check error:', error);
                localStorage.removeItem('user');
            } finally {
                setLoading(false);
            }
        };

        checkSession();
    }, []);

    const login = async (email, password) => {
        try {
            const response = await authAPI.login({ email, password });
            const user = response.data.user;
            setCurrentUser(user);
            localStorage.setItem('user', JSON.stringify(user));
            return { success: true };
        } catch (error) {
            return { success: false, error: error.response?.data?.error || 'Login failed' };
        }
    };

    const register = async (email, firstname, lastname, password) => {
        try {
            const response = await authAPI.register({ email, firstname, lastname, password });
            const user = response.data.user;
            setCurrentUser(user);
            localStorage.setItem('user', JSON.stringify(user));
            return { success: true };
        } catch (error) {
            return { success: false, error: error.response?.data?.error || 'Registration failed' };
        }
    };

    const logout = async () => {
        try {
            await authAPI.logout();
            setCurrentUser(null);
            localStorage.removeItem('user');
        } catch (error) {
            console.error('Logout error:', error);
        }
    };

    const refreshUser = async () => {
        try {
            console.log('AuthContext: refreshing user...');
            const response = await authAPI.getCurrentUser();
            const user = response.data;
            console.log('AuthContext: got updated user:', user);
            setCurrentUser(user);
            localStorage.setItem('user', JSON.stringify(user));
        } catch (error) {
            console.error('Failed to refresh user:', error);
        }
    };

    const value = {
        currentUser,
        login,
        register,
        logout,
        refreshUser,
        loading
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthContext;
