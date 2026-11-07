// ---------------------------------------------------------------------------
// Types mirroring the backend DTOs
// ---------------------------------------------------------------------------

export type LanguageCode = 'en' | 'th' | 'my'

export interface Seller {
  id: string
  email: string
  displayName: string
  language: LanguageCode
  timeZone: string
}

export interface AuthResponse {
  token: string
  seller: Seller
}

export interface Account {
  id: string
  bankCode: string
  accountLast4: string
  holderName: string
}

export interface ChannelDto {
  id: string
  channel: 'TELEGRAM' | 'LINE' | 'WEB'
  externalUserId: string
  language: string
  summaryPush: boolean
  linkedAt: string
}

export interface LinkCode {
  code: string
  expiresAt: string
}

export interface SlipResultDto {
  submissionId: string
  resultCode: string
  amountBaht: string | null
  transferredAt: string | null
  sendingBank: string | null
  receiverAccountMasked: string | null
  expectedAmountBaht: string | null
  label: string | null
  message: string
  retryScheduled: boolean
}

export interface Submission {
  id: string
  channel: string
  status: string
  resultCode: string | null
  captionRaw: string | null
  label: string | null
  expectedAmountBaht: string | null
  amountBaht: string | null
  transferredAt: string | null
  sendingBank: string | null
  transRef: string | null
  receiverAccountMasked: string | null
  reviewDecision: string | null
  reviewNote: string | null
  createdAt: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface DailySummary {
  date: string
  verified: number
  totalBaht: string
  review: number
  duplicates: number
  rejected: number
  message: string
}

export interface ErrorBody {
  error: string
  message: string
}
