import { useCallback, useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ImagePlus, Loader2, RefreshCw } from 'lucide-react'
import { errorMessage, slipApi, summaryApi } from '../api'
import type { DailySummary, SlipResultDto } from '../types/type'
import { VerdictCard } from '../components/result/VerdictCard'

const MAX_BYTES = 4 * 1024 * 1024

export default function Upload() {
  const { t } = useTranslation()
  const inputRef = useRef<HTMLInputElement>(null)

  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<string | null>(null)
  const [caption, setCaption] = useState('')
  const [dragging, setDragging] = useState(false)
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState<SlipResultDto | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [summary, setSummary] = useState<DailySummary | null>(null)

  const refreshSummary = useCallback(() => {
    summaryApi.daily().then(setSummary).catch(() => setSummary(null))
  }, [])

  useEffect(() => {
    refreshSummary()
  }, [refreshSummary])

  useEffect(() => () => {
    if (preview) URL.revokeObjectURL(preview)
  }, [preview])

  function acceptFile(candidate: File | undefined | null) {
    if (!candidate) return
    if (!candidate.type.startsWith('image/')) {
      setError(t('result.INVALID_IMAGE'))
      return
    }
    if (candidate.size > MAX_BYTES) {
      setError(t('upload.formats'))
      return
    }
    setError(null)
    setResult(null)
    if (preview) URL.revokeObjectURL(preview)
    setFile(candidate)
    setPreview(URL.createObjectURL(candidate))
  }

  async function submit() {
    if (!file || busy) return
    setBusy(true)
    setError(null)
    try {
      const res = await slipApi.upload(file, caption)
      setResult(res)
      refreshSummary()
    } catch (err) {
      setError(errorMessage(err, t('upload.failed')))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <section>
        <h1 className="text-2xl font-bold tracking-tight text-slate-50">{t('upload.title')}</h1>
        <p className="mt-1 text-sm text-slate-400">{t('upload.subtitle')}</p>

        <div className="card mt-5 p-5 sm:p-6">
          <input
            ref={inputRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(e) => acceptFile(e.target.files?.[0])}
          />

          {preview ? (
            <div>
              <div className="relative overflow-hidden rounded-xl ring-1 ring-white/10">
                <img src={preview} alt="Slip preview" className="mx-auto max-h-72 object-contain" />
              </div>
              <button
                type="button"
                className="btn-ghost mt-3 w-full"
                onClick={() => inputRef.current?.click()}
              >
                <RefreshCw className="h-4 w-4" aria-hidden />
                {t('upload.change')}
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              onDragOver={(e) => {
                e.preventDefault()
                setDragging(true)
              }}
              onDragLeave={() => setDragging(false)}
              onDrop={(e) => {
                e.preventDefault()
                setDragging(false)
                acceptFile(e.dataTransfer.files?.[0])
              }}
              className={`flex w-full flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed px-6 py-12 text-center transition ${
                dragging
                  ? 'border-emerald-400 bg-emerald-400/5'
                  : 'border-white/15 hover:border-white/30'
              }`}
            >
              <span className="grid h-12 w-12 place-items-center rounded-2xl bg-emerald-400/10 ring-1 ring-emerald-400/30">
                <ImagePlus className="h-6 w-6 text-emerald-300" aria-hidden />
              </span>
              <span className="text-sm font-medium text-slate-200">{t('upload.dropHere')}</span>
              <span className="text-xs text-slate-500">{t('upload.formats')}</span>
            </button>
          )}

          <div className="mt-4">
            <label className="label" htmlFor="caption">{t('upload.caption')}</label>
            <input
              id="caption"
              className="input"
              value={caption}
              onChange={(e) => setCaption(e.target.value)}
              placeholder={t('upload.captionPlaceholder')}
              maxLength={120}
            />
            <p className="mt-1.5 text-xs text-slate-500">{t('upload.captionHint')}</p>
          </div>

          {error && (
            <p className="mt-4 rounded-xl bg-rose-500/10 px-4 py-2.5 text-sm text-rose-300 ring-1 ring-rose-400/30">
              {error}
            </p>
          )}

          <button type="button" className="btn-primary mt-5 w-full" onClick={submit} disabled={!file || busy}>
            {busy && <Loader2 className="h-4 w-4 animate-spin" aria-hidden />}
            {busy ? t('upload.checking') : t('upload.check')}
          </button>

          <p className="mt-3 text-center text-xs text-slate-500">{t('upload.privacy')}</p>
        </div>

        {result && (
          <div className="mt-6">
            <VerdictCard result={result} />
          </div>
        )}
      </section>

      <aside className="space-y-6">
        {summary && (
          <div className="card p-5">
            <h2 className="text-sm font-bold text-slate-100">
              {t('summary.title')} · {summary.date}
            </h2>
            <dl className="mt-3 grid grid-cols-2 gap-2.5 text-center">
              <div className="rounded-xl bg-emerald-400/10 p-3 ring-1 ring-emerald-400/20">
                <dd className="text-xl font-bold text-emerald-300">{summary.verified}</dd>
                <dt className="text-[11px] text-slate-400">{t('summary.verified')}</dt>
              </div>
              <div className="rounded-xl bg-amber-400/10 p-3 ring-1 ring-amber-400/20">
                <dd className="text-xl font-bold text-amber-300">{summary.review}</dd>
                <dt className="text-[11px] text-slate-400">{t('summary.review')}</dt>
              </div>
              <div className="rounded-xl bg-rose-400/10 p-3 ring-1 ring-rose-400/20">
                <dd className="text-xl font-bold text-rose-300">{summary.duplicates}</dd>
                <dt className="text-[11px] text-slate-400">{t('summary.duplicates')}</dt>
              </div>
              <div className="rounded-xl bg-ink-900/80 p-3 ring-1 ring-white/10">
                <dd className="text-xl font-bold text-slate-200">{summary.rejected}</dd>
                <dt className="text-[11px] text-slate-400">{t('summary.rejected')}</dt>
              </div>
            </dl>
          </div>
        )}

        <div className="card p-5">
          <h2 className="text-sm font-bold text-slate-100">{t('upload.tipsTitle')}</h2>
          <ul className="mt-3 space-y-2 text-sm text-slate-400">
            <li className="flex gap-2"><span className="text-emerald-400">•</span>{t('upload.tip1')}</li>
            <li className="flex gap-2"><span className="text-emerald-400">•</span>{t('upload.tip2')}</li>
            <li className="flex gap-2"><span className="text-emerald-400">•</span>{t('upload.tip3')}</li>
          </ul>
        </div>
      </aside>
    </div>
  )
}
