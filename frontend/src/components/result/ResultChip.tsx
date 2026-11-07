import { useTranslation } from 'react-i18next'
import { DEFAULT_RESULT_ICON, RESULT_ICONS, TONE_CLASSES, toneFor } from './result-helpers'

/** Small status chip used in history rows and the review queue. */
export function ResultChip({ code }: { code: string | null }) {
  const { t } = useTranslation()
  const Icon = RESULT_ICONS[code ?? ''] ?? DEFAULT_RESULT_ICON
  const tone = toneFor(code)
  return (
    <span className={`chip ${TONE_CLASSES[tone].chip}`}>
      <Icon className="h-3.5 w-3.5" aria-hidden />
      {code ? t(`result.${code}`) : '…'}
    </span>
  )
}