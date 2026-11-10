import type { ReactNode } from 'react'
import { AlertCircle } from 'lucide-react'

export function Alert({ children }: { children: ReactNode }) {
  return (
    <div
      role="alert"
      className="flex items-start gap-2 rounded-xl bg-rose-500/10 px-4 py-2.5 text-sm text-rose-200 ring-1 ring-rose-400/30"
    >
      <AlertCircle className="mt-0.5 h-4 w-4 flex-shrink-0" aria-hidden />
      <span>{children}</span>
    </div>
  )
}