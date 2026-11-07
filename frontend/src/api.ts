import axios from 'axios'
import type { Account, AuthResponse, ChannelDto, DailySummary, ErrorBody, LanguageCode, LinkCode, Page, Seller, SlipResultDto, Submission } from './types/type'

// ---------------------------------------------------------------------------
// Client
// ---------------------------------------------------------------------------

const TOKEN_KEY = 'slipcheck.token'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (token: string) => localStorage.setItem(TOKEN_KEY, token)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)

export const api = axios.create({ baseURL: '/api/v1' })

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearToken()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

export const errorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as ErrorBody | undefined
    if (body?.message) return body.message
  }
  return fallback
}

// ---------------------------------------------------------------------------
// Endpoints
// ---------------------------------------------------------------------------

export const authApi = {
  login: (email: string, password: string) =>
    api.post<AuthResponse>('/auth/login', { email, password }).then((r) => r.data),
  register: (email: string, password: string, displayName: string, language: LanguageCode) =>
    api
      .post<AuthResponse>('/auth/register', { email, password, displayName, language })
      .then((r) => r.data),
}

export const sellerApi = {
  me: () => api.get<Seller>('/seller/me').then((r) => r.data),
  updateMe: (patch: Partial<Pick<Seller, 'displayName' | 'language' | 'timeZone'>>) =>
    api.patch<Seller>('/seller/me', patch).then((r) => r.data),
  deleteMe: () => api.delete('/seller/me'),
  linkCode: (channel: 'TELEGRAM' | 'LINE') =>
    api.post<LinkCode>('/seller/link-codes', { channel }).then((r) => r.data),
  channels: () => api.get<ChannelDto[]>('/seller/channels').then((r) => r.data),
  unlinkChannel: (id: string) => api.delete(`/seller/channels/${id}`),
}

export const accountApi = {
  list: () => api.get<Account[]>('/seller/accounts').then((r) => r.data),
  add: (bankCode: string, accountNo: string, holderName: string) =>
    api.post<Account>('/seller/accounts', { bankCode, accountNo, holderName }).then((r) => r.data),
  remove: (id: string) => api.delete(`/seller/accounts/${id}`),
}

export const slipApi = {
  upload: (image: File, caption?: string) => {
    const form = new FormData()
    form.append('image', image)
    if (caption?.trim()) form.append('caption', caption.trim())
    return api.post<SlipResultDto>('/slips', form).then((r) => r.data)
  },
  history: (params: {
    resultCode?: string
    from?: string
    to?: string
    page?: number
    size?: number
  }) => api.get<Page<Submission>>('/slips', { params }).then((r) => r.data),
  reviewQueue: () => api.get<Submission[]>('/slips/review').then((r) => r.data),
  review: (id: string, decision: 'APPROVED' | 'REJECTED', note?: string) =>
    api.post<Submission>(`/slips/${id}/review`, { decision, note }).then((r) => r.data),
}

export const summaryApi = {
  daily: (date?: string) =>
    api.get<DailySummary>('/summary/daily', { params: date ? { date } : {} }).then((r) => r.data),
}
