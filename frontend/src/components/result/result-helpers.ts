import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  CloudOff,
  Copy,
  Gauge,
  ImageOff,
  Link2Off,
  ScanLine,
  SearchX,
  ShieldQuestion,
  ShieldX,
  type LucideIcon,
} from 'lucide-react'

export type Tone = 'good' | 'warn' | 'bad' | 'info'

export function toneFor(code: string | null): Tone {
  switch (code) {
    case 'VERIFIED':
      return 'good'
    case 'AMOUNT_MISMATCH':
    case 'RECEIVER_UNCERTAIN':
    case 'UNREADABLE_QR':
    case 'QUOTA_EXCEEDED':
    case 'INVALID_IMAGE':
    case 'NOT_LINKED':
      return 'warn'
    case 'BANK_PENDING':
    case 'PROVIDER_UNAVAILABLE':
      return 'info'
    default:
      return 'bad' // DUPLICATE_SLIP, WRONG_RECEIVER, SLIP_NOT_FOUND
  }
}

export const RESULT_ICONS: Record<string, LucideIcon> = {
  VERIFIED: CheckCircle2,
  AMOUNT_MISMATCH: AlertTriangle,
  RECEIVER_UNCERTAIN: ShieldQuestion,
  DUPLICATE_SLIP: Copy,
  WRONG_RECEIVER: ShieldX,
  UNREADABLE_QR: ScanLine,
  SLIP_NOT_FOUND: SearchX,
  BANK_PENDING: Clock,
  PROVIDER_UNAVAILABLE: CloudOff,
  QUOTA_EXCEEDED: Gauge,
  INVALID_IMAGE: ImageOff,
  NOT_LINKED: Link2Off,
}

export const DEFAULT_RESULT_ICON: LucideIcon = AlertTriangle

export const TONE_CLASSES: Record<Tone, { chip: string; tile: string; glow: string }> = {
  good: {
    chip: 'bg-emerald-400/15 text-emerald-300 ring-emerald-400/30',
    tile: 'bg-emerald-400/15 ring-emerald-400/30',
    glow: 'shadow-glow',
  },
  warn: {
    chip: 'bg-amber-400/15 text-amber-300 ring-amber-400/30',
    tile: 'bg-amber-400/15 ring-amber-400/30',
    glow: '',
  },
  bad: {
    chip: 'bg-rose-400/15 text-rose-300 ring-rose-400/30',
    tile: 'bg-rose-400/15 ring-rose-400/30',
    glow: '',
  },
  info: {
    chip: 'bg-sky-400/15 text-sky-300 ring-sky-400/30',
    tile: 'bg-sky-400/15 ring-sky-400/30',
    glow: '',
  },
}

export function formatWhen(iso: string | null, language: string): string {
  if (!iso) return '—'
  const locale = language === 'th' ? 'th-TH' : language === 'my' ? 'my-MM' : 'en-GB'
  try {
    return new Intl.DateTimeFormat(locale, {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(iso))
  } catch {
    return iso
  }
}