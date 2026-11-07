import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { clearToken, getToken, sellerApi, setToken } from '../api'
import type { Seller } from '../types/type.ts'
import { setLanguage, type LanguageCode } from '../i18n.ts'
import { AuthContext, type AuthState } from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [seller, setSeller] = useState<Seller | null>(null)
  // Only "loading" if there is a token to validate
  const [loading, setLoading] = useState(() => getToken() !== null)

  const applySeller = useCallback((s: Seller) => {
    setSeller(s)
    setLanguage(s.language as LanguageCode)
  }, [])

  // Restore session on first load
  useEffect(() => {
    if (!getToken()) return

    sellerApi
      .me()
      .then(applySeller)
      .catch(clearToken)
      .finally(() => setLoading(false))
  }, [applySeller])

  const onAuthenticated = useCallback(
    (token: string, s: Seller) => {
      setToken(token)
      applySeller(s)
    },
    [applySeller],
  )

  const logout = useCallback(() => {
    clearToken()
    setSeller(null)
  }, [])

  const value = useMemo<AuthState>(
    () => ({ seller, loading, onAuthenticated, onSellerUpdated: applySeller, logout }),
    [seller, loading, onAuthenticated, applySeller, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}