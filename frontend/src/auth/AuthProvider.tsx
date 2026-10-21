import { useState, type ReactNode } from 'react'
import { login } from '../api/auth'
import { AuthContext } from './AuthContext'
import { clearToken, getToken, saveToken } from './tokenStorage'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getToken())

  async function signIn(email: string, password: string) {
    const response = await login({ email, password })
    saveToken(response.token)
    setToken(response.token)
  }

  function signOut() {
    clearToken()
    setToken(null)
  }

  return (
    <AuthContext value={{ isAuthenticated: token !== null, signIn, signOut }}>
      {children}
    </AuthContext>
  )
}