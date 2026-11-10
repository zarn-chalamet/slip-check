import { forwardRef, type InputHTMLAttributes, type ReactNode } from 'react'

type Props = InputHTMLAttributes<HTMLInputElement> & {
  label: string
  hint?: string
  error?: string
  leftIcon?: ReactNode
  rightSlot?: ReactNode
}

export const TextField = forwardRef<HTMLInputElement, Props>(function TextField(
  { label, hint, error, leftIcon, rightSlot, id, className = '', ...rest },
  ref,
) {
  const inputId = id ?? rest.name
  return (
    <div>
      <label htmlFor={inputId} className="label">{label}</label>
      <div className="relative">
        {leftIcon && (
          <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-500">
            {leftIcon}
          </span>
        )}
        <input
          ref={ref}
          id={inputId}
          aria-invalid={!!error}
          aria-describedby={error ? `${inputId}-err` : hint ? `${inputId}-hint` : undefined}
          className={`input ${leftIcon ? 'pl-10' : ''} ${rightSlot ? 'pr-11' : ''} ${
            error ? 'ring-rose-400/60 focus:ring-rose-400' : ''
          } ${className}`}
          {...rest}
        />
        {rightSlot && (
          <div className="absolute right-2 top-1/2 -translate-y-1/2">{rightSlot}</div>
        )}
      </div>
      {error ? (
        <p id={`${inputId}-err`} className="mt-1 text-xs text-rose-300">{error}</p>
      ) : hint ? (
        <p id={`${inputId}-hint`} className="mt-1 text-xs text-slate-500">{hint}</p>
      ) : null}
    </div>
  )
})