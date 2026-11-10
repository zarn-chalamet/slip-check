import { ShieldCheck } from 'lucide-react'
import { useTranslation } from 'react-i18next'

export function AuthBrand() {
  const { t } = useTranslation()
  return (
    <div className="mb-8 text-center">
      <span className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-emerald-400 shadow-glow ring-4 ring-emerald-400/20">
        <ShieldCheck className="h-8 w-8 text-ink-950" aria-hidden />
      </span>
      <h1 className="mt-4 text-3xl font-extrabold tracking-tight text-slate-50">SlipCheck</h1>
      <p className="mt-2 text-sm text-slate-400">{t('auth.subtitle')}</p>
    </div>
  )
}