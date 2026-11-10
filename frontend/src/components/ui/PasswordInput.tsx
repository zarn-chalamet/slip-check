import { forwardRef, useState, type InputHTMLAttributes } from 'react'
import { Eye, EyeOff, Lock } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { TextField } from './TextField'

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> & {
  label: string
  hint?: string
  error?: string
}

export const PasswordInput = forwardRef<HTMLInputElement, Props>(function PasswordInput(
  props,
  ref,
) {
  const { t } = useTranslation()
  const [visible, setVisible] = useState(false)

  return (
    <TextField
      {...props}
      ref={ref}
      type={visible ? 'text' : 'password'}
      leftIcon={<Lock className="h-4 w-4" aria-hidden />}
      rightSlot={
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? t('auth.hidePassword') : t('auth.showPassword')}
          aria-pressed={visible}
          className="grid h-8 w-8 place-items-center rounded-lg text-slate-400 transition hover:bg-white/5 hover:text-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-400/60"
          tabIndex={-1}
        >
          {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      }
    />
  )
})