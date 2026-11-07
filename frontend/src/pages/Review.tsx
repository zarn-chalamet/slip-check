import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Check, ClipboardCheck, X } from 'lucide-react'
import { errorMessage, slipApi } from '../api'
import type { Submission } from '../types/type'
import { ResultChip } from '../components/result/ResultChip'
import { formatWhen } from '../components/result/result-helpers'

/**
 * Review queue (spec F4): AMOUNT_MISMATCH / RECEIVER_UNCERTAIN items. Approve or reject
 * with an optional note — the transaction stays claimed either way and can never be reused.
 */
export default function Review() {
  const { t, i18n } = useTranslation()
  const [items, setItems] = useState<Submission[]>([])
  const [notes, setNotes] = useState<Record<string, string>>({})
  const [busyId, setBusyId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let ignore = false

    slipApi
        .reviewQueue()
        .then((result) => {
        if (!ignore) setItems(result)
        })
        .catch(() => {
        if (!ignore) setItems([])
        })
        .finally(() => {
        if (!ignore) setLoading(false)
        })

    return () => {
        ignore = true
    }
  }, [])

  async function decide(id: string, decision: 'APPROVED' | 'REJECTED') {
    if (busyId) return
    setBusyId(id)
    setError(null)
    try {
      await slipApi.review(id, decision, notes[id]?.trim() || undefined)
      setItems((prev) => prev.filter((item) => item.id !== id))
    } catch (err) {
      setError(errorMessage(err, t('review.failed')))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold tracking-tight text-slate-50">{t('review.title')}</h1>
      <p className="mt-1 text-sm text-slate-400">{t('review.subtitle')}</p>

      {error && (
        <p className="mt-4 rounded-xl bg-rose-500/10 px-4 py-2.5 text-sm text-rose-300 ring-1 ring-rose-400/30">
          {error}
        </p>
      )}

      {loading && <p className="py-10 text-center text-sm text-slate-500">{t('common.loading')}</p>}

      {!loading && items.length === 0 && (
        <div className="card mt-6 flex flex-col items-center gap-3 p-12 text-center">
          <ClipboardCheck className="h-8 w-8 text-emerald-400" aria-hidden />
          <p className="text-sm text-slate-400">{t('review.empty')}</p>
        </div>
      )}

      <div className="mt-5 space-y-4">
        {items.map((item) => (
          <div key={item.id} className="card p-4 sm:p-5">
            <div className="flex flex-wrap items-center gap-3">
              <ResultChip code={item.resultCode} />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-slate-200">
                  {item.label || item.captionRaw || t('result.SLIP_NOT_FOUND')}
                </p>
                <p className="text-xs text-slate-500">
                  {item.channel} · {formatWhen(item.createdAt, i18n.language)}
                </p>
              </div>
            </div>

            <dl className="mt-4 grid grid-cols-2 gap-2.5 sm:grid-cols-4">
              <div className="rounded-xl bg-ink-900/60 p-3 ring-1 ring-white/5">
                <dt className="text-[11px] uppercase tracking-wider text-slate-500">{t('result.amount')}</dt>
                <dd className="mt-0.5 text-sm font-semibold text-slate-100">
                  {item.amountBaht ? `${item.amountBaht} ${t('result.thb')}` : '—'}
                </dd>
              </div>
              <div className="rounded-xl bg-ink-900/60 p-3 ring-1 ring-white/5">
                <dt className="text-[11px] uppercase tracking-wider text-slate-500">{t('result.expected')}</dt>
                <dd className="mt-0.5 text-sm font-semibold text-slate-100">
                  {item.expectedAmountBaht ? `${item.expectedAmountBaht} ${t('result.thb')}` : '—'}
                </dd>
              </div>
              <div className="rounded-xl bg-ink-900/60 p-3 ring-1 ring-white/5">
                <dt className="text-[11px] uppercase tracking-wider text-slate-500">{t('result.receiver')}</dt>
                <dd className="mt-0.5 truncate text-sm font-semibold text-slate-100">
                  {item.receiverAccountMasked || '—'}
                </dd>
              </div>
              <div className="rounded-xl bg-ink-900/60 p-3 ring-1 ring-white/5">
                <dt className="text-[11px] uppercase tracking-wider text-slate-500">{t('result.bank')}</dt>
                <dd className="mt-0.5 text-sm font-semibold text-slate-100">
                  {item.sendingBank || '—'} · {formatWhen(item.transferredAt, i18n.language)}
                </dd>
              </div>
            </dl>

            <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
              <div className="flex-1">
                <label className="label" htmlFor={`note-${item.id}`}>{t('review.note')}</label>
                <input
                  id={`note-${item.id}`}
                  className="input"
                  placeholder={t('review.notePlaceholder')}
                  value={notes[item.id] ?? ''}
                  onChange={(e) => setNotes((prev) => ({ ...prev, [item.id]: e.target.value }))}
                  maxLength={500}
                />
              </div>
              <div className="flex gap-2">
                <button
                  className="btn-primary flex-1 sm:flex-none"
                  disabled={busyId === item.id}
                  onClick={() => decide(item.id, 'APPROVED')}
                >
                  <Check className="h-4 w-4" aria-hidden />
                  {t('review.approve')}
                </button>
                <button
                  className="btn-danger flex-1 sm:flex-none"
                  disabled={busyId === item.id}
                  onClick={() => decide(item.id, 'REJECTED')}
                >
                  <X className="h-4 w-4" aria-hidden />
                  {t('review.reject')}
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
