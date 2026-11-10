import { useEffect, useState, useRef, type ChangeEvent } from 'react'
import { useTranslation } from 'react-i18next'
import {
  ChevronLeft,
  ChevronRight,
  Inbox,
  Filter,
  Calendar,
  ChevronDown,
  Globe,
  Check,
  X,
  RotateCcw,
  SlidersHorizontal,
} from 'lucide-react'
import { slipApi } from '../api'
import type { Page, Submission } from '../types/type'
import { ResultChip } from '../components/result/ResultChip'
import { formatWhen } from '../components/result/result-helpers'

const STATUS_ITEMS = [
  { code: 'VERIFIED', color: 'bg-emerald-400' },
  { code: 'DUPLICATE_SLIP', color: 'bg-rose-400' },
  { code: 'SLIP_NOT_FOUND', color: 'bg-rose-400' },
  { code: 'AMOUNT_MISMATCH', color: 'bg-amber-400' },
  { code: 'RECEIVER_UNCERTAIN', color: 'bg-amber-400' },
  { code: 'WRONG_RECEIVER', color: 'bg-rose-400' },
  { code: 'UNREADABLE_QR', color: 'bg-amber-400' },
  { code: 'BANK_PENDING', color: 'bg-sky-400' },
  { code: 'PROVIDER_UNAVAILABLE', color: 'bg-slate-400' },
  { code: 'QUOTA_EXCEEDED', color: 'bg-slate-400' },
  { code: 'INVALID_IMAGE', color: 'bg-rose-400' },
]

function ChannelLogo({ channel }: { channel: string }) {
  if (channel === 'TELEGRAM') {
    return (
      <svg className="h-6 w-6 shrink-0" viewBox="0 0 24 24" fill="none" role="img" aria-label="Telegram">
        <circle cx="12" cy="12" r="12" fill="#24A1DE" />
        <path
          d="M5.4 11.9L17.1 7.3C17.6 7.1 18.1 7.4 18 7.9L16 16.5C15.9 17 15.4 17.2 14.9 16.9L11.5 14.4L9.8 16C9.6 16.2 9.3 16.1 9.3 15.8V13.8L15.3 9.4C15.5 9.2 15.3 8.9 15.1 9C12.8 10.4 8.5 13.1 8.5 13.1L5.4 11.9Z"
          fill="white"
        />
      </svg>
    )
  }

  if (channel === 'LINE') {
    return (
      <svg className="h-6 w-6 shrink-0" viewBox="0 0 24 24" fill="none" role="img" aria-label="LINE">
        <circle cx="12" cy="12" r="12" fill="#06C755" />
        <path
          d="M17.5 11C17.5 8.2 15 6 12 6C9 6 6.5 8.2 6.5 11C6.5 13.1 7.9 14.9 10 15.6C10.2 15.7 10.3 15.9 10.2 16.1L9.8 17.3C9.7 17.6 10 17.8 10.2 17.6L12.8 16.1C12.9 16 13 16 13.1 16C15.6 15.8 17.5 13.6 17.5 11Z"
          fill="white"
        />
      </svg>
    )
  }

  return (
    <div
      className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-emerald-400/10 text-emerald-400 ring-1 ring-emerald-400/30"
      title="Web Dashboard"
    >
      <Globe className="h-3.5 w-3.5" aria-hidden />
    </div>
  )
}

function StatusPill({
  value,
  onChange,
  t,
}: {
  value: string
  onChange: (val: string) => void
  t: (k: string) => string
}) {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  const selected = STATUS_ITEMS.find((s) => s.code === value)

  return (
    <div className="relative w-full sm:w-auto" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className={`flex h-9 w-full items-center justify-between gap-2 rounded-xl px-3 text-xs font-semibold transition focus:outline-none focus:ring-2 focus:ring-emerald-400/40 sm:w-auto sm:justify-start ${
          value
            ? 'bg-emerald-400/10 text-emerald-300 ring-1 ring-emerald-400/30'
            : 'bg-ink-900 text-slate-300 ring-1 ring-white/10 hover:bg-white/5'
        }`}
      >
        <span className="flex items-center gap-2 truncate">
          <Filter className="h-3.5 w-3.5 shrink-0 text-slate-400" />
          <span className="truncate">{selected ? t(`result.${selected.code}`) : t('history.allResults')}</span>
        </span>
        <ChevronDown className={`h-3.5 w-3.5 shrink-0 text-slate-400 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <div className="absolute left-0 top-full z-50 mt-2 w-full min-w-[14rem] rounded-2xl border border-white/10 bg-ink-900 p-1.5 shadow-2xl sm:w-56">
          <button
            onClick={() => {
              onChange('')
              setOpen(false)
            }}
            className={`flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-sm font-medium transition sm:py-2 sm:text-xs ${
              !value ? 'bg-emerald-400/10 text-emerald-300' : 'text-slate-300 hover:bg-white/5'
            }`}
          >
            <span>{t('history.allResults')}</span>
            {!value && <Check className="h-3.5 w-3.5" />}
          </button>

          <div className="my-1 h-px bg-white/10" />

          <div className="max-h-56 space-y-0.5 overflow-y-auto">
            {STATUS_ITEMS.map(({ code, color }) => {
              const active = value === code
              return (
                <button
                  key={code}
                  onClick={() => {
                    onChange(code)
                    setOpen(false)
                  }}
                  className={`flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-sm font-medium transition sm:py-2 sm:text-xs ${
                    active ? 'bg-emerald-400/10 text-emerald-300' : 'text-slate-300 hover:bg-white/5'
                  }`}
                >
                  <span className="flex items-center gap-2.5 truncate">
                    <span className={`h-2 w-2 shrink-0 rounded-full ${color}`} />
                    <span className="truncate">{t(`result.${code}`)}</span>
                  </span>
                  {active && <Check className="h-3.5 w-3.5 shrink-0" />}
                </button>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}

function DateField({
  label,
  value,
  onChange,
  onClear,
}: {
  label: string
  value: string
  onChange: (e: ChangeEvent<HTMLInputElement>) => void
  onClear: () => void
}) {
  return (
    <div className="flex h-9 min-w-0 flex-1 items-center gap-2 rounded-xl bg-ink-900 px-3 ring-1 ring-white/10 sm:w-auto sm:flex-none">
      <Calendar className="h-3.5 w-3.5 shrink-0 text-slate-400" aria-hidden />

      <div className="relative min-w-0 flex-1 sm:w-[7.5rem]">
        {/* Fake placeholder text */}
        {!value && (
          <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center text-xs text-slate-500">
            {label}
          </span>
        )}

        {/* Real working date input */}
        <input
          type="date"
          value={value}
          onChange={onChange}
          aria-label={label}
          className={`w-full bg-transparent text-xs text-slate-200 outline-none ${
            value ? 'opacity-100' : 'opacity-0'
          } [&::-webkit-calendar-picker-indicator]:absolute [&::-webkit-calendar-picker-indicator]:inset-0 [&::-webkit-calendar-picker-indicator]:h-full [&::-webkit-calendar-picker-indicator]:w-full [&::-webkit-calendar-picker-indicator]:cursor-pointer [&::-webkit-calendar-picker-indicator]:opacity-0`}
        />
      </div>

      {value && (
        <button
          type="button"
          onClick={onClear}
          className="shrink-0 rounded-md p-0.5 text-slate-400 transition hover:bg-white/10 hover:text-slate-200"
          aria-label={`Clear ${label}`}
        >
          <X className="h-3.5 w-3.5" />
        </button>
      )}
    </div>
  )
}

export default function History() {
  const { t, i18n } = useTranslation()
  const [resultCode, setResultCode] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState<Page<Submission> | null>(null)
  const [loading, setLoading] = useState(true)
  const [showFilters, setShowFilters] = useState(false)

  const handleResultChange = (val: string) => {
    setResultCode(val)
    setPage(0)
    setLoading(true)
  }

  const handleFromChange = (e: ChangeEvent<HTMLInputElement>) => {
    setFrom(e.target.value)
    setPage(0)
    setLoading(true)
  }

  const handleToChange = (e: ChangeEvent<HTMLInputElement>) => {
    setTo(e.target.value)
    setPage(0)
    setLoading(true)
  }

  const clearFrom = () => {
    setFrom('')
    setPage(0)
    setLoading(true)
  }

  const clearTo = () => {
    setTo('')
    setPage(0)
    setLoading(true)
  }

  const clearAllFilters = () => {
    setResultCode('')
    setFrom('')
    setTo('')
    setPage(0)
    setLoading(true)
  }

  const hasActiveFilters = Boolean(resultCode || from || to)

  useEffect(() => {
    let ignore = false
    slipApi
      .history({
        resultCode: resultCode || undefined,
        from: from || undefined,
        to: to ? `${to}T23:59:59Z` : undefined,
        page,
        size: 20,
      })
      .then((result) => {
        if (!ignore) setData(result)
      })
      .catch(() => {
        if (!ignore) setData(null)
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [resultCode, from, to, page])

  return (
    <div className="space-y-5 sm:space-y-6">
      {/* HEADER */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <h1 className="text-xl font-extrabold tracking-tight text-slate-50 sm:text-2xl">
            {t('history.title')}
          </h1>
          <p className="mt-1 text-sm text-slate-400">{t('history.subtitle')}</p>
        </div>

        <div className="flex items-center gap-2">
          {data && (
            <div className="flex items-center gap-2 rounded-full bg-ink-900/80 px-3 py-1.5 ring-1 ring-white/10">
              <span className="h-2 w-2 rounded-full bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.8)]" />
              <p className="text-xs font-semibold text-slate-300">
                {t('history.total', { count: data.totalElements })}
              </p>
            </div>
          )}

          {/* Mobile show/hide filters */}
          <button
            type="button"
            onClick={() => setShowFilters((v) => !v)}
            className={`inline-flex items-center gap-2 rounded-xl px-3 py-2 text-xs font-semibold ring-1 transition sm:hidden ${
              showFilters || hasActiveFilters
                ? 'bg-emerald-400/10 text-emerald-300 ring-emerald-400/30'
                : 'bg-ink-900 text-slate-200 ring-white/10'
            }`}
            aria-expanded={showFilters}
          >
            <SlidersHorizontal className="h-4 w-4" />
            Filters
            {hasActiveFilters && !showFilters && (
              <span className="grid h-4 min-w-4 place-items-center rounded-full bg-emerald-400 px-1 text-[10px] font-bold text-ink-950">
                !
              </span>
            )}
          </button>
        </div>
      </div>

      {/* FILTERS — hidden on mobile until toggled */}
      <div className={`relative z-30 ${showFilters ? 'block' : 'hidden'} sm:block`}>
        <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-center sm:gap-2.5">
          <StatusPill value={resultCode} onChange={handleResultChange} t={t} />

          <div className="hidden h-4 w-px bg-white/10 sm:block" />

          {/* Updated Date Layout: single line horizontal on mobile */}
          <div className="flex w-full items-center gap-2 sm:w-auto">
            <DateField label="Start" value={from} onChange={handleFromChange} onClear={clearFrom} />
            <span className="text-xs text-slate-600 shrink-0">→</span>
            <DateField label="End" value={to} onChange={handleToChange} onClear={clearTo} />
          </div>

          {hasActiveFilters && (
            <button
              type="button"
              onClick={clearAllFilters}
              className="inline-flex h-9 w-full items-center justify-center gap-1.5 rounded-xl px-3 text-xs font-medium text-slate-400 transition hover:bg-white/5 hover:text-slate-200 sm:w-auto"
            >
              <RotateCcw className="h-3.5 w-3.5" />
              Reset
            </button>
          )}
        </div>
      </div>

      {/* DATA TABLE */}
      <div className="card overflow-hidden">
        <div className="hidden border-b border-white/5 bg-ink-900/40 px-6 py-3 text-[11px] font-bold uppercase tracking-wider text-slate-400 sm:grid sm:grid-cols-[140px_1fr_120px_50px] sm:gap-4">
          <div>Status</div>
          <div>Transaction Details</div>
          <div className="text-right">Amount</div>
          <div className="text-right">Channel</div>
        </div>

        {loading ? (
          <div className="divide-y divide-white/5">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="flex animate-pulse items-center gap-4 px-4 py-4 sm:px-6">
                <div className="h-6 w-24 rounded-full bg-white/10" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-1/3 rounded bg-white/10" />
                  <div className="h-3 w-1/4 rounded bg-white/5" />
                </div>
                <div className="h-6 w-6 rounded-full bg-white/10" />
              </div>
            ))}
          </div>
        ) : !data || data.content.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-3 px-6 py-16 text-center">
            <div className="grid h-14 w-14 place-items-center rounded-2xl bg-white/5 ring-1 ring-white/10">
              <Inbox className="h-7 w-7 text-slate-500" aria-hidden />
            </div>
            <p className="text-sm font-medium text-slate-300">{t('history.empty')}</p>
          </div>
        ) : (
          <div className="divide-y divide-white/5">
            {data.content.map((sub) => (
              <div
                key={sub.id}
                className="flex flex-col gap-3 px-4 py-4 transition-colors hover:bg-white/[0.02] sm:grid sm:grid-cols-[140px_1fr_120px_50px] sm:items-center sm:gap-4 sm:px-6"
              >
                <div className="flex items-center justify-between sm:block">
                  <ResultChip code={sub.resultCode} />
                  {sub.amountBaht && (
                    <div className="text-right sm:hidden">
                      <span className="text-sm font-semibold text-slate-100">{sub.amountBaht}</span>
                      <span className="ml-1 text-[10px] font-medium text-slate-400">THB</span>
                    </div>
                  )}
                </div>

                <div className="min-w-0 flex-1">
                  <p className="truncate font-mono text-sm font-medium text-slate-200">
                    {sub.label || sub.captionRaw || sub.transRef || sub.id.slice(0, 8)}
                  </p>
                  <div className="mt-0.5 flex items-center justify-between gap-2 sm:justify-start">
                    <p className="truncate text-xs text-slate-400">
                      {formatWhen(sub.transferredAt ?? sub.createdAt, i18n.language)}
                      {sub.sendingBank ? ` · ${sub.sendingBank}` : ''}
                    </p>
                    <div className="sm:hidden">
                      <ChannelLogo channel={sub.channel} />
                    </div>
                  </div>
                </div>

                <div className="hidden text-right sm:block">
                  {sub.amountBaht ? (
                    <>
                      <p className="text-sm font-semibold text-slate-100">{sub.amountBaht}</p>
                      <p className="text-[10px] font-medium text-slate-400">THB</p>
                    </>
                  ) : (
                    <span className="text-xs text-slate-400">—</span>
                  )}
                </div>

                <div className="hidden sm:flex sm:justify-end">
                  <ChannelLogo channel={sub.channel} />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* PAGINATION */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-white/10 pt-4 sm:justify-center sm:gap-6">
          <button
            className="flex items-center gap-1.5 rounded-xl px-3 py-2 text-xs font-semibold text-slate-300 transition hover:bg-white/5 disabled:opacity-40"
            disabled={page === 0}
            onClick={() => {
              setPage(page - 1)
              setLoading(true)
            }}
          >
            <ChevronLeft className="h-4 w-4" />
            {t('history.prev')}
          </button>

          <span className="text-xs font-semibold text-slate-400">
            {t('history.page', { page: data.page + 1, total: data.totalPages })}
          </span>

          <button
            className="flex items-center gap-1.5 rounded-xl px-3 py-2 text-xs font-semibold text-slate-300 transition hover:bg-white/5 disabled:opacity-40"
            disabled={page >= data.totalPages - 1}
            onClick={() => {
              setPage(page + 1)
              setLoading(true)
            }}
          >
            {t('history.next')}
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      )}
    </div>
  )
}