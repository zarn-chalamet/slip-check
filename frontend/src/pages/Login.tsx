import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ShieldCheck } from 'lucide-react'
import { authApi, errorMessage } from '../api'
import { LANGUAGES, setLanguage, type LanguageCode } from '../i18n'
import { useAuth } from '../hook/useAuth'

export default function Login() {
  const { t } = useTranslation()
  const { onAuthenticated } = useAuth()
  const navigate = useNavigate()

  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [language, setLanguageChoice] = useState<LanguageCode>('en')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const res =
        mode === 'login'
          ? await authApi.login(email.trim(), password)
          : await authApi.register(email.trim(), password, displayName.trim(), language)
      onAuthenticated(res.token, res.seller)
      navigate('/')
    } catch (err) {
      setError(errorMessage(err, t('auth.failed')))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid min-h-screen place-items-center px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <span className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-emerald-400 shadow-glow">
            <ShieldCheck className="h-8 w-8 text-ink-950" aria-hidden />
          </span>
          <h1 className="mt-4 text-3xl font-extrabold tracking-tight text-slate-50">SlipCheck</h1>
          <p className="mt-2 text-sm text-slate-400">{t('auth.subtitle')}</p>
        </div>

        <div className="card p-6 sm:p-8">
          <h2 className="text-lg font-bold text-slate-100">
            {mode === 'login' ? t('auth.welcome') : t('auth.createAccount')}
          </h2>

          <form onSubmit={submit} className="mt-5 space-y-4">
            {mode === 'register' && (
              <div>
                <label className="label" htmlFor="displayName">{t('auth.displayName')}</label>
                <input
                  id="displayName"
                  className="input"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  required
                  maxLength={120}
                />
              </div>
            )}

            <div>
              <label className="label" htmlFor="email">{t('auth.email')}</label>
              <input
                id="email"
                type="email"
                className="input"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
              />
            </div>

            <div>
              <label className="label" htmlFor="password">{t('auth.password')}</label>
              <input
                id="password"
                type="password"
                className="input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={mode === 'register' ? 8 : 1}
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              />
              {mode === 'register' && (
                <p className="mt-1 text-xs text-slate-500">{t('auth.passwordHint')}</p>
              )}
            </div>

            {mode === 'register' && (
              <div>
                <span className="label">{t('auth.language')}</span>
                <div className="grid grid-cols-3 gap-2">
                  {LANGUAGES.map((lang) => (
                    <button
                      key={lang.code}
                      type="button"
                      onClick={() => {
                        setLanguageChoice(lang.code)
                        setLanguage(lang.code)
                      }}
                      className={`rounded-xl px-3 py-2 text-sm font-medium ring-1 transition ${
                        language === lang.code
                          ? 'bg-emerald-400 text-ink-950 ring-emerald-400'
                          : 'bg-ink-900/80 text-slate-300 ring-white/10 hover:bg-white/5'
                      }`}
                      aria-pressed={language === lang.code}
                    >
                      {lang.label}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {error && (
              <p className="rounded-xl bg-rose-500/10 px-4 py-2.5 text-sm text-rose-300 ring-1 ring-rose-400/30">
                {error}
              </p>
            )}

            <button type="submit" className="btn-primary w-full" disabled={busy}>
              {busy ? t('common.loading') : mode === 'login' ? t('auth.signIn') : t('auth.register')}
            </button>
          </form>

          <button
            className="mt-4 w-full text-center text-sm text-emerald-300 hover:text-emerald-200"
            onClick={() => setMode(mode === 'login' ? 'register' : 'login')}
          >
            {mode === 'login' ? t('auth.needAccount') : t('auth.haveAccount')}
          </button>
        </div>
      </div>
    </div>
  )
}
