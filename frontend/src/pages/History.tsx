import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ChevronLeft, ChevronRight, Inbox } from 'lucide-react'
import { slipApi } from '../api'
import type { Page, Submission } from '../types/type'
import { ResultChip } from '../components/result/ResultChip'
import { formatWhen } from '../components/result/result-helpers'

const RESULT_CODES = [
  'VERIFIED',
  'AMOUNT_MISMATCH',
  'RECEIVER_UNCERTAIN',
  'DUPLICATE_SLIP',
  'WRONG_RECEIVER',
  'UNREADABLE_QR',
  'SLIP_NOT_FOUND',
  'BANK_PENDING',
  'PROVIDER_UNAVAILABLE',
  'QUOTA_EXCEEDED',
  'INVALID_IMAGE',
]

export default function HistoryPage() {
  const { t, i18n } = useTranslation()
  const [resultCode, setResultCode] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState<Page<Submission> | null>(null)
  const [loading, setLoading] = useState(true)

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
    <div>
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-50">{t('history.title')}</h1>
          <p className="mt-1 text-sm text-slate-400">{t('history.subtitle')}</p>
        </div>
        {data && (
          <p className="text-sm text-slate-500">{t('history.total', { count: data.totalElements })}</p>
        )}
      </div>

      <div className="card mt-5 p-4">
        <div className="grid gap-3 sm:grid-cols-3">
          <div>
            <label className="label" htmlFor="filter-result">{t('history.allResults')}</label>
            <select
              id="filter-result"
              className="input"
              value={resultCode}
              onChange={(e) => {
                setResultCode(e.target.value)
                setPage(0)
              }}
            >
              <option value="">{t('history.allResults')}</option>
              {RESULT_CODES.map((code) => (
                <option key={code} value={code}>
                  {t(`result.${code}`)}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="label" htmlFor="filter-from">{t('history.date')}</label>
            <input
              id="filter-from"
              type="date"
              className="input"
              value={from}
              onChange={(e) => {
                setFrom(e.target.value)
                setPage(0)
              }}
            />
          </div>
          <div>
            <label className="label" htmlFor="filter-to">→</label>
            <input
              id="filter-to"
              type="date"
              className="input"
              value={to}
              onChange={(e) => {
                setTo(e.target.value)
                setPage(0)
              }}
            />
          </div>
        </div>
      </div>

      <div className="mt-4 space-y-2">
        {loading && <p className="py-10 text-center text-sm text-slate-500">{t('common.loading')}</p>}

        {!loading && data && data.content.length === 0 && (
          <div className="card flex flex-col items-center gap-3 p-12 text-center">
            <Inbox className="h-8 w-8 text-slate-600" aria-hidden />
            <p className="text-sm text-slate-400">{t('history.empty')}</p>
          </div>
        )}

        {!loading &&
          data?.content.map((sub) => (
            <div key={sub.id} className="card flex flex-wrap items-center gap-3 px-4 py-3">
              <ResultChip code={sub.resultCode} />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-slate-200">
                  {sub.label || sub.captionRaw || sub.transRef || sub.id.slice(0, 8)}
                </p>
                <p className="text-xs text-slate-500">
                  {formatWhen(sub.transferredAt ?? sub.createdAt, i18n.language)}
                  {sub.sendingBank ? ` · ${sub.sendingBank}` : ''}
                </p>
              </div>
              {sub.amountBaht && (
                <p className="text-sm font-semibold text-slate-100">
                  {sub.amountBaht} <span className="text-xs text-slate-500">{t('result.thb')}</span>
                </p>
              )}
              <span className="chip bg-ink-900/80 text-slate-400 ring-white/10">{sub.channel}</span>
              {sub.reviewDecision && (
                <span
                  className={`chip ${
                    sub.reviewDecision === 'APPROVED'
                      ? 'bg-emerald-400/10 text-emerald-300 ring-emerald-400/30'
                      : 'bg-rose-400/10 text-rose-300 ring-rose-400/30'
                  }`}
                >
                  {sub.reviewDecision === 'APPROVED' ? t('review.approved') : t('review.rejected')}
                </span>
              )}
            </div>
          ))}
      </div>

      {data && data.totalPages > 1 && (
        <div className="mt-5 flex items-center justify-center gap-3">
          <button className="btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>
            <ChevronLeft className="h-4 w-4" aria-hidden />
            {t('history.prev')}
          </button>
          <span className="text-sm text-slate-400">
            {t('history.page', { page: data.page + 1, total: data.totalPages })}
          </span>
          <button
            className="btn-ghost"
            disabled={page >= data.totalPages - 1}
            onClick={() => setPage(page + 1)}
          >
            {t('history.next')}
            <ChevronRight className="h-4 w-4" aria-hidden />
          </button>
        </div>
      )}
    </div>
  )
}
