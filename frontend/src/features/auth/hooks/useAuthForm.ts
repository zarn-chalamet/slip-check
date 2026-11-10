import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { authApi, errorMessage } from '../../../api'
import { useAuth } from '../../../hook/useAuth'
import { setLanguage, type LanguageCode } from '../../../i18n'
import { validate, type AuthMode, type FieldErrors } from '../validation'

export function useAuthForm() {
  const { t } = useTranslation()
  const { onAuthenticated } = useAuth()
  const navigate = useNavigate()

  const [mode, setMode] = useState<AuthMode>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [language, setLanguageChoice] = useState<LanguageCode>('en')
  const [busy, setBusy] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  function toggleMode() {
    setMode((m) => (m === 'login' ? 'register' : 'login'))
    
    // Clear form fields
    setEmail('')
    setPassword('')
    setDisplayName('')
    
    // Clear errors
    setFormError(null)
    setFieldErrors({})
  }

  function pickLanguage(code: LanguageCode) {
    setLanguageChoice(code)
    setLanguage(code)
  }

  async function submit() {
    const errs = validate(mode, { email, password, displayName }, t)
    setFieldErrors(errs)
    if (Object.keys(errs).length) return

    setBusy(true)
    setFormError(null)
    try {
      const res =
        mode === 'login'
          ? await authApi.login(email.trim(), password)
          : await authApi.register(email.trim(), password, displayName.trim(), language)
      onAuthenticated(res.token, res.seller)
      navigate('/')
    } catch (err) {
      setFormError(errorMessage(err, t('auth.failed')))
    } finally {
      setBusy(false)
    }
  }

  return {
    mode, toggleMode,
    email, setEmail,
    password, setPassword,
    displayName, setDisplayName,
    language, pickLanguage,
    busy, formError, fieldErrors,
    submit,
  }
}