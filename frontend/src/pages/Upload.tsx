import { useCallback, useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ImagePlus, Loader2, X, FileImage, ShieldCheck } from 'lucide-react'
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

  function clearFile() {
    setFile(null)
    if (preview) URL.revokeObjectURL(preview)
    setPreview(null)
    setResult(null)
    if (inputRef.current) inputRef.current.value = ''
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
    <div className="pb-12">
      <div className="mb-8">
        <h1 className="text-2xl font-extrabold tracking-tight text-slate-50">{t('upload.title')}</h1>
        <p className="mt-1 text-sm text-slate-400">{t('upload.subtitle')}</p>
      </div>

      <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-[1fr_360px]">
        
        {/* Left Side: Upload Main Area */}
        <section className="space-y-6">
          <div className="card overflow-hidden bg-ink-900/40 p-1">
            <div className="p-5 sm:p-6">
              
              <input
                ref={inputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(e) => acceptFile(e.target.files?.[0])}
              />

              {preview ? (
                <div className="relative overflow-hidden rounded-2xl bg-ink-950 ring-1 ring-white/10">
                  <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/cubes.png')] opacity-20" />
                  <img src={preview} alt="Slip preview" className="relative mx-auto max-h-80 object-contain p-4 drop-shadow-2xl" />
                  
                  <button
                    type="button"
                    onClick={clearFile}
                    className="absolute right-3 top-3 grid h-8 w-8 place-items-center rounded-full bg-black/50 text-white backdrop-blur-md transition hover:bg-rose-500/80 focus:outline-none"
                    title="Remove image"
                  >
                    <X className="h-4 w-4" />
                  </button>
                  
                  <div className="absolute bottom-3 left-3 right-3 rounded-xl bg-black/60 px-4 py-2.5 backdrop-blur-md">
                    <div className="flex items-center gap-3">
                      <FileImage className="h-5 w-5 text-emerald-400" />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-xs font-bold text-slate-200">{file?.name}</p>
                        <p className="text-[10px] text-slate-400">{(file!.size / 1024 / 1024).toFixed(2)} MB</p>
                      </div>
                    </div>
                  </div>
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
                  className={`group relative flex w-full flex-col items-center justify-center gap-4 rounded-3xl border-2 border-dashed px-6 py-16 text-center transition-all ${
                    dragging
                      ? 'border-emerald-400 bg-emerald-400/5'
                      : 'border-white/10 bg-ink-900/20 hover:border-white/20 hover:bg-ink-900/40'
                  }`}
                >
                  <span className={`grid h-14 w-14 place-items-center rounded-2xl ring-1 transition-colors ${dragging ? 'bg-emerald-400/20 ring-emerald-400/50' : 'bg-white/5 ring-white/10 group-hover:bg-white/10'}`}>
                    <ImagePlus className={`h-6 w-6 ${dragging ? 'text-emerald-400' : 'text-slate-400 group-hover:text-slate-200'}`} aria-hidden />
                  </span>
                  <div>
                    <p className="text-sm font-bold text-slate-200">{t('upload.dropHere')}</p>
                    <p className="mt-1 text-xs font-medium text-slate-500">{t('upload.formats')}</p>
                  </div>
                </button>
              )}

              <div className="mt-6">
                <label className="mb-1.5 block text-xs font-semibold text-slate-300" htmlFor="caption">
                  {t('upload.caption')}
                </label>
                <input
                  id="caption"
                  className="w-full rounded-xl bg-ink-900 px-3.5 py-2.5 text-sm text-slate-200 outline-none ring-1 ring-white/10 transition focus:ring-2 focus:ring-emerald-400/50"
                  value={caption}
                  onChange={(e) => setCaption(e.target.value)}
                  placeholder={t('upload.captionPlaceholder')}
                  maxLength={120}
                  autoComplete="off"
                />
                <p className="mt-2 text-[11px] font-medium text-slate-500">{t('upload.captionHint')}</p>
              </div>

              {error && (
                <div className="mt-5 rounded-xl bg-rose-500/10 px-4 py-3 text-sm font-semibold text-rose-400 ring-1 ring-rose-400/30 animate-in fade-in zoom-in-95">
                  {error}
                </div>
              )}
            </div>

            <div className="border-t border-white/5 bg-ink-900/50 p-5 sm:p-6">
              <button 
                type="button" 
                className="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-emerald-400 px-4 text-sm font-bold text-ink-950 transition hover:bg-emerald-300 focus:outline-none focus:ring-2 focus:ring-emerald-400/50 disabled:opacity-50" 
                onClick={submit} 
                disabled={!file || busy}
              >
                {busy && <Loader2 className="h-4 w-4 animate-spin" aria-hidden />}
                {busy ? t('upload.checking') : t('upload.check')}
              </button>
              <div className="mt-4 flex items-center justify-center gap-2 text-[11px] font-medium text-slate-500">
                <ShieldCheck className="h-3.5 w-3.5 text-emerald-400/60" />
                {t('upload.privacy')}
              </div>
            </div>
          </div>

          {result && (
            <div className="animate-in fade-in slide-in-from-bottom-4">
              <VerdictCard result={result} />
            </div>
          )}
        </section>

        {/* Right Side: Sidebar Stats & Tips */}
        <aside className="space-y-6">
          {summary && (
            <div className="card overflow-hidden bg-ink-900/40">
              <div className="border-b border-white/5 bg-ink-900/40 px-5 py-4">
                <h2 className="text-sm font-bold text-slate-100">
                  {t('summary.title')}
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">{summary.date}</p>
              </div>
              <div className="grid grid-cols-2 gap-px bg-white/5 p-4">
                <div className="flex flex-col items-center justify-center rounded-tl-xl rounded-bl-xl bg-ink-900/80 py-4">
                  <dd className="text-2xl font-black text-emerald-400">{summary.verified}</dd>
                  <dt className="mt-1 text-[10px] font-bold uppercase tracking-wider text-slate-500">{t('summary.verified')}</dt>
                </div>
                <div className="flex flex-col items-center justify-center rounded-tr-xl rounded-br-xl bg-ink-900/80 py-4">
                  <dd className="text-2xl font-black text-amber-400">{summary.review}</dd>
                  <dt className="mt-1 text-[10px] font-bold uppercase tracking-wider text-slate-500">{t('summary.review')}</dt>
                </div>
                <div className="flex flex-col items-center justify-center rounded-tl-xl rounded-bl-xl bg-ink-900/80 py-4 mt-px">
                  <dd className="text-2xl font-black text-rose-400">{summary.duplicates}</dd>
                  <dt className="mt-1 text-[10px] font-bold uppercase tracking-wider text-slate-500">{t('summary.duplicates')}</dt>
                </div>
                <div className="flex flex-col items-center justify-center rounded-tr-xl rounded-br-xl bg-ink-900/80 py-4 mt-px">
                  <dd className="text-2xl font-black text-slate-300">{summary.rejected}</dd>
                  <dt className="mt-1 text-[10px] font-bold uppercase tracking-wider text-slate-500">{t('summary.rejected')}</dt>
                </div>
              </div>
            </div>
          )}

          <div className="card overflow-hidden bg-ink-900/40">
            <div className="border-b border-white/5 bg-ink-900/40 px-5 py-4">
              <h2 className="text-sm font-bold text-slate-100">{t('upload.tipsTitle')}</h2>
            </div>
            <div className="p-5">
              <ul className="space-y-3 text-sm text-slate-400">
                <li className="flex items-start gap-2.5">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-400" />
                  <span className="leading-relaxed">{t('upload.tip1')}</span>
                </li>
                <li className="flex items-start gap-2.5">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-400" />
                  <span className="leading-relaxed">{t('upload.tip2')}</span>
                </li>
                <li className="flex items-start gap-2.5">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-400" />
                  <span className="leading-relaxed">{t('upload.tip3')}</span>
                </li>
              </ul>
            </div>
          </div>
        </aside>
      </div>
    </div>
  )
}