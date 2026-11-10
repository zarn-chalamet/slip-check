import { useTranslation } from 'react-i18next'
import { Clock } from 'lucide-react'
import type { SlipResultDto } from '../../types/type'
import { DEFAULT_RESULT_ICON, RESULT_ICONS, TONE_CLASSES, formatWhen, toneFor } from './result-helpers'

export function VerdictCard({ result }: { result: SlipResultDto }) {
  const { t, i18n } = useTranslation()
  const tone = toneFor(result.resultCode)
  const Icon = RESULT_ICONS[result.resultCode] ?? DEFAULT_RESULT_ICON
  const theme = TONE_CLASSES[tone]

  const details: Array<{ label: string; value: string | null | undefined }> = [
    { label: t('result.amount'), value: result.amountBaht ? `${result.amountBaht} THB` : null },
    { label: t('result.expected'), value: result.expectedAmountBaht ? `${result.expectedAmountBaht} THB` : null },
    { label: t('result.bank'), value: result.sendingBank },
    { label: t('result.time'), value: formatWhen(result.transferredAt, i18n.language) },
    { label: t('result.receiver'), value: result.receiverAccountMasked },
  ].filter((d) => d.value)

  return (
    <div 
      className={`card overflow-hidden border animate-in fade-in slide-in-from-bottom-4 duration-500 ${theme.glow}`} 
      role="status" 
      aria-live="polite"
    >
      <div className="flex flex-col gap-5 p-5 sm:flex-row sm:items-start sm:p-6">
        <div className={`grid h-14 w-14 shrink-0 place-items-center rounded-2xl ring-1 ${theme.tile}`}>
          <Icon className="h-7 w-7" aria-hidden />
        </div>
        
        <div className="min-w-0 flex-1">
          <h2 className={`text-xl font-bold tracking-tight ${theme.text}`}>
            {t(`result.${result.resultCode}`)}
          </h2>
          <p className="mt-1.5 text-sm leading-relaxed text-slate-300">
            {result.message}
          </p>
          
          {result.retryScheduled && (
            <p className="mt-3 flex items-center gap-2 text-xs font-semibold text-sky-400">
              <Clock className="h-4 w-4" aria-hidden />
              {t('result.retryScheduled')}
            </p>
          )}
        </div>
      </div>

      {details.length > 0 && (
        <div className="border-t border-white/5 bg-ink-900/50 p-5 sm:p-6">
          <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            {details.map((d) => (
              <div key={d.label}>
                <dt className="text-[11px] font-bold uppercase tracking-wider text-slate-500">{d.label}</dt>
                <dd className="mt-1 truncate text-sm font-semibold text-slate-200">{d.value}</dd>
              </div>
            ))}
          </dl>
        </div>
      )}
    </div>
  )
}