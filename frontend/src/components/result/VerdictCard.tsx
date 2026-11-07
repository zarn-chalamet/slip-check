import { useTranslation } from 'react-i18next'
import { Clock } from 'lucide-react'
import type { SlipResultDto } from '../../types/type'
import { DEFAULT_RESULT_ICON, RESULT_ICONS, TONE_CLASSES, formatWhen, toneFor } from './result-helpers'

/** The big verdict card after a web upload: icon + colour + text (never colour alone). */
export function VerdictCard({ result }: { result: SlipResultDto }) {
  const { t, i18n } = useTranslation()
  const tone = toneFor(result.resultCode)
  const Icon = RESULT_ICONS[result.resultCode] ?? DEFAULT_RESULT_ICON

  const details: Array<{ label: string; value: string | null | undefined }> = [
    { label: t('result.amount'), value: result.amountBaht ? `${result.amountBaht} ${t('result.thb')}` : null },
    { label: t('result.expected'), value: result.expectedAmountBaht ? `${result.expectedAmountBaht} ${t('result.thb')}` : null },
    { label: t('result.bank'), value: result.sendingBank },
    { label: t('result.time'), value: formatWhen(result.transferredAt, i18n.language) },
    { label: t('result.receiver'), value: result.receiverAccountMasked },
  ].filter((d) => d.value)

  return (
    <div className={`card p-5 sm:p-6 ${TONE_CLASSES[tone].glow}`} role="status" aria-live="polite">
      <div className="flex items-start gap-4">
        <span className={`grid h-12 w-12 shrink-0 place-items-center rounded-2xl ring-1 ${TONE_CLASSES[tone].tile}`}>
          <Icon className="h-6 w-6" aria-hidden />
        </span>
        <div className="min-w-0">
          <h2 className="text-lg font-bold text-slate-100">{t(`result.${result.resultCode}`)}</h2>
          <p className="mt-1 text-sm leading-relaxed text-slate-300">{result.message}</p>
        </div>
      </div>

      {details.length > 0 && (
        <dl className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3">
          {details.map((d) => (
            <div key={d.label} className="rounded-xl bg-ink-900/60 p-3 ring-1 ring-white/5">
              <dt className="text-[11px] font-medium uppercase tracking-wider text-slate-500">{d.label}</dt>
              <dd className="mt-0.5 truncate text-sm font-semibold text-slate-100">{d.value}</dd>
            </div>
          ))}
        </dl>
      )}

      {result.retryScheduled && (
        <p className="mt-4 flex items-center gap-2 text-xs text-sky-300">
          <Clock className="h-3.5 w-3.5" aria-hidden />
          {t('result.retryScheduled')}
        </p>
      )}
    </div>
  )
}