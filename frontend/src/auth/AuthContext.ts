import { createContext, useContext } from 'react'

export interface AuthContextValue {
    isAuthenticated: boolean
    signIn: (email: string, password: string) => Promise<void>
    signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
    const context = useContext(AuthContext)
    if (!context) {
        throw new Error('useAuth deve ser usado dentro de um AuthProvider')
    }
    return context
}