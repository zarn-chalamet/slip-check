import { createContext } from 'react'
import type { Seller } from '../types/type'

export interface AuthState {
  seller: Seller | null
  loading: boolean
  onAuthenticated: (token: string, seller: Seller) => void
  onSellerUpdated: (seller: Seller) => void
  logout: () => void
}

export const AuthContext = createContext<AuthState | null>(null)