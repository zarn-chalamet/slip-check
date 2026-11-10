import { useTranslation } from 'react-i18next'
import { passwordStrength } from '../validation'

const BAR = ['bg-rose-400', 'bg-rose-400', 'bg-amber-400', 'bg-emerald-300', 'bg-emerald-400']
const LABEL = ['tooShort', 'weak', 'fair', 'good', 'strong'] as const

export function PasswordStrength({ password }: { password: string }) {
  const { t } = useTranslation()
  if (!password) return null
  const score = passwordStrength(password)

  return (
    <div className="mt-2" aria-live="polite">
      <div className="flex gap-1">
        {[0, 1, 2, 3].map((i) => (
          <span
            key={i}
            className={`h-1 flex-1 rounded-full transition-colors ${
              i < score ? BAR[score] : 'bg-white/10'
            }`}
          />
        ))}
      </div>
      <p className="mt-1 text-xs text-slate-500">
        {t(`auth.strength.${LABEL[score]}`)}
      </p>
    </div>
  )
}