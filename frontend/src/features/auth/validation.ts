export type AuthMode = 'login' | 'register'

export type FieldErrors = Partial<Record<'email' | 'password' | 'displayName', string>>

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validate(
  mode: AuthMode,
  v: { email: string; password: string; displayName: string },
  t: (k: string) => string,
): FieldErrors {
  const errors: FieldErrors = {}
  if (!EMAIL_RE.test(v.email.trim())) errors.email = t('auth.errors.email')
  if (v.password.length < (mode === 'register' ? 8 : 1)) {
    errors.password = t('auth.errors.passwordShort')
  }
  if (mode === 'register' && v.displayName.trim().length < 2) {
    errors.displayName = t('auth.errors.displayName')
  }
  return errors
}

/** 0–4 score: length + variety */
export function passwordStrength(pw: string): 0 | 1 | 2 | 3 | 4 {
  if (!pw) return 0
  let score = 0
  if (pw.length >= 8) score++
  if (pw.length >= 12) score++
  if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score++
  if (/\d/.test(pw) && /[^A-Za-z0-9]/.test(pw)) score++
  return Math.min(score, 4) as 0 | 1 | 2 | 3 | 4
}