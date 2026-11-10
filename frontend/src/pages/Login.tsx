import { type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Mail, User } from 'lucide-react'
import { AuthBrand } from '../features/auth/components/AuthBrand'
import { AuthModeToggle } from '../features/auth/components/AuthModeToggle'
import { PasswordStrength } from '../features/auth/components/PasswordStrength'
import { useAuthForm } from '../features/auth/hooks/useAuthForm'
import { TextField } from '../components/ui/TextField'
import { PasswordInput } from '../components/ui/PasswordInput'
import { LanguagePicker } from '../components/ui/LanguagePicker'
import { Alert } from '../components/ui/Alert'

export default function Login() {
  const { t } = useTranslation()
  const f = useAuthForm()

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    void f.submit()
  }

  const isRegister = f.mode === 'register'

  return (
    <div className="relative min-h-screen overflow-hidden">
      {/* decorative background */}
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 -z-10 bg-[radial-gradient(1200px_600px_at_20%_-10%,rgba(16,185,129,0.15),transparent),radial-gradient(900px_500px_at_100%_120%,rgba(56,189,248,0.10),transparent)]"
      />
      <div className="grid min-h-screen place-items-center px-4 py-10">
        <div className="w-full max-w-md">
          <AuthBrand />

          <div className="card p-6 sm:p-8">
            <div className="mb-5 flex items-baseline justify-between">
              <h2 className="text-lg font-bold text-slate-100">
                {isRegister ? t('auth.createAccount') : t('auth.welcome')}
              </h2>
              <span className="text-xs text-slate-500">
                {isRegister ? t('auth.step.register') : t('auth.step.login')}
              </span>
            </div>

            <form onSubmit={onSubmit} className="space-y-4" noValidate>
              {isRegister && (
                <TextField
                  label={t('auth.displayName')}
                  name="displayName"
                  value={f.displayName}
                  onChange={(e) => f.setDisplayName(e.target.value)}
                  leftIcon={<User className="h-4 w-4" aria-hidden />}
                  autoComplete="name"
                  maxLength={120}
                  required
                  error={f.fieldErrors.displayName}
                />
              )}

              <TextField
                label={t('auth.email')}
                name="email"
                type="email"
                value={f.email}
                onChange={(e) => f.setEmail(e.target.value)}
                leftIcon={<Mail className="h-4 w-4" aria-hidden />}
                autoComplete="email"
                inputMode="email"
                required
                error={f.fieldErrors.email}
              />

              <div>
                <PasswordInput
                  label={t('auth.password')}
                  name="password"
                  value={f.password}
                  onChange={(e) => f.setPassword(e.target.value)}
                  autoComplete={isRegister ? 'new-password' : 'current-password'}
                  minLength={isRegister ? 8 : 1}
                  required
                  hint={isRegister ? t('auth.passwordHint') : undefined}
                  error={f.fieldErrors.password}
                />
                {isRegister && <PasswordStrength password={f.password} />}
              </div>

              {isRegister && (
                <LanguagePicker
                  value={f.language}
                  onChange={f.pickLanguage}
                  label={t('auth.language')}
                />
              )}

              {f.formError && <Alert>{f.formError}</Alert>}

              <button
                type="submit"
                className="btn-primary w-full"
                disabled={f.busy}
                aria-busy={f.busy}
              >
                {f.busy
                  ? t('common.loading')
                  : isRegister
                  ? t('auth.register')
                  : t('auth.signIn')}
              </button>
            </form>

            <AuthModeToggle mode={f.mode} onToggle={f.toggleMode} />
          </div>

          <p className="mt-6 text-center text-xs text-slate-500">
            {t('auth.footerLegal')}
          </p>
        </div>
      </div>
    </div>
  )
}