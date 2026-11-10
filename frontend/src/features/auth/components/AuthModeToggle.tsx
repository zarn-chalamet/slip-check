import { useTranslation } from 'react-i18next'
import type { AuthMode } from '../validation'

export function AuthModeToggle({ mode, onToggle }: { mode: AuthMode; onToggle: () => void }) {
  const { t } = useTranslation()
  return (
    <p className="mt-6 text-center text-sm text-slate-400">
      {mode === 'login' ? t('auth.needAccount') : t('auth.haveAccount')}{' '}
      <button
        type="button"
        onClick={onToggle}
        className="font-semibold text-emerald-300 underline-offset-4 transition hover:text-emerald-200 hover:underline focus:outline-none focus:ring-2 focus:ring-emerald-400/60 rounded"
      >
        {mode === 'login' ? t('auth.register') : t('auth.signIn')}
      </button>
    </p>
  )
}