import { LANGUAGES, type LanguageCode } from '../../i18n'

type Props = {
  value: LanguageCode
  onChange: (code: LanguageCode) => void
  label?: string
}

export function LanguagePicker({ value, onChange, label }: Props) {
  return (
    <div>
      {label && <span className="label">{label}</span>}
      <div role="radiogroup" aria-label={label} className="grid grid-cols-3 gap-2">
        {LANGUAGES.map((lang) => {
          const active = value === lang.code
          return (
            <button
              key={lang.code}
              role="radio"
              aria-checked={active}
              type="button"
              onClick={() => onChange(lang.code)}
              className={`rounded-xl px-3 py-2 text-sm font-medium ring-1 transition focus:outline-none focus:ring-2 focus:ring-emerald-400/60 ${
                active
                  ? 'bg-emerald-400 text-ink-950 ring-emerald-400 shadow-glow'
                  : 'bg-ink-900/80 text-slate-300 ring-white/10 hover:bg-white/5'
              }`}
            >
              {lang.label}
            </button>
          )
        })}
      </div>
    </div>
  )
}