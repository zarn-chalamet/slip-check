import { useTranslation } from 'react-i18next'
import { DEFAULT_RESULT_ICON, RESULT_ICONS, TONE_CLASSES, toneFor } from './result-helpers'

export function ResultChip({ code }: { code: string | null }) {
  const { t } = useTranslation()
  const Icon = RESULT_ICONS[code ?? ''] ?? DEFAULT_RESULT_ICON
  const tone = toneFor(code)
  
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-[11px] font-bold uppercase tracking-wider ring-1 ring-inset ${TONE_CLASSES[tone].chip}`}>
      <Icon className="h-3.5 w-3.5" aria-hidden />
      {code ? t(`result.${code}`) : '…'}
    </span>
  )
}