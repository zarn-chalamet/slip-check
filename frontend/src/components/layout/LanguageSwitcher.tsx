import { Languages } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { LANGUAGES, setLanguage } from '../../i18n.ts'

export function LanguageSwitcher() {
  const { i18n } = useTranslation()
  return (
    <div className="flex items-center gap-1 rounded-full bg-ink-900/80 ring-1 ring-white/10 p-1">
      <Languages className="ml-1.5 h-3.5 w-3.5 text-slate-500" aria-hidden />
      {LANGUAGES.map((lang) => (
        <button
          key={lang.code}
          onClick={() => setLanguage(lang.code)}
          className={`rounded-full px-2.5 py-1 text-xs font-medium transition ${
            i18n.language === lang.code
              ? 'bg-emerald-400 text-ink-950'
              : 'text-slate-400 hover:text-slate-200'
          }`}
          aria-pressed={i18n.language === lang.code}
        >
          {lang.label}
        </button>
      ))}
    </div>
  )
}