import { createContext, useContext, useState, useEffect, type ReactNode } from "react";
import { jwtDecode } from "jwt-decode";
import type { UserResponse } from "../../../types";

interface AuthContextType {
    isAuthenticated: boolean;
    user: UserResponse | null;
    login: (token: string, user: UserResponse) => void;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);



export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<UserResponse | null>(null);
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
    const [isInitializing, setIsInitializing] = useState<boolean>(true);

    useEffect(() => {
        const initAuth = () => {
            const token = localStorage.getItem("token");
            const storedUser = localStorage.getItem("user");

            if (token && storedUser) {
                try {
                    const decoded = jwtDecode(token);
                    const isExpired = decoded.exp && decoded.exp * 1000 < Date.now();
                    if (!isExpired) {
                        setUser(JSON.parse(storedUser));
                        setIsAuthenticated(true);
                    } else {
                        localStorage.removeItem("token");
                        localStorage.removeItem("user");
                    }
                } catch (error) {
                    // invalid token
                    localStorage.removeItem("token");
                }
            }
            setIsInitializing(false);
        };

        initAuth();
    }, []);

    const login = (token: string, userData: UserResponse) => {
        localStorage.setItem("token", token);
        localStorage.setItem("user", JSON.stringify(userData));
        setUser(userData);
        setIsAuthenticated(true);
    };

    const logout = () => {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        setUser(null);
        setIsAuthenticated(false);
    };

    if (isInitializing) {
        return null; // Or a loading spinner
    }

    return (
        <AuthContext.Provider value={{ isAuthenticated, user, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
}
