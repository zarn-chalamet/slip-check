import type { ReactNode } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { LogOut, ShieldCheck } from 'lucide-react'
import { useAuth } from '../../hook/useAuth'
import { LanguageSwitcher } from './LanguageSwitcher'
import { NAV_ITEMS } from './nav-items'

export function Layout({ children }: { children: ReactNode }) {
  const { t } = useTranslation()
  const { seller, logout } = useAuth()
  const { pathname } = useLocation()

  return (
    <div className="min-h-screen pb-24 sm:pb-0">
      <header className="sticky top-0 z-40 border-b border-white/5 bg-ink-950/80 backdrop-blur-xl">
        <div className="mx-auto flex h-14 max-w-5xl items-center gap-3 px-4">
          <Link to="/" className="flex items-center gap-2">
            <span className="grid h-8 w-8 place-items-center rounded-xl bg-emerald-400 shadow-glow">
              <ShieldCheck className="h-5 w-5 text-ink-950" aria-hidden />
            </span>
            <span className="text-base font-bold tracking-tight text-slate-100">SlipCheck</span>
          </Link>

          <nav className="ml-4 hidden items-center gap-1 sm:flex" aria-label="Primary">
            {NAV_ITEMS.map(({ to, key, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                end={to === '/'}
                className={({ isActive }) =>
                  `flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm transition ${
                    isActive ? 'bg-white/10 text-slate-100' : 'text-slate-400 hover:text-slate-200'
                  }`
                }
              >
                <Icon className="h-4 w-4" aria-hidden />
                {t(key)}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto flex items-center gap-3">
            <LanguageSwitcher />
            <span className="hidden text-sm text-slate-400 md:inline">{seller?.displayName}</span>
            <button
              onClick={logout}
              className="rounded-full p-2 text-slate-400 transition hover:bg-white/5 hover:text-slate-200"
              aria-label={t('nav.logout')}
              title={t('nav.logout')}
            >
              <LogOut className="h-4 w-4" aria-hidden />
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-5xl px-4 py-6">{children}</main>

      {/* Mobile bottom navigation: sellers mostly work from phones */}
      <nav
        className="fixed inset-x-0 bottom-0 z-40 flex border-t border-white/5 bg-ink-950/90 backdrop-blur-xl sm:hidden"
        aria-label="Primary mobile"
      >
        {NAV_ITEMS.map(({ to, key, icon: Icon }) => {
          const active = to === '/' ? pathname === '/' : pathname.startsWith(to)
          return (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={`flex flex-1 flex-col items-center gap-0.5 py-2.5 text-[10px] font-medium transition ${
                active ? 'text-emerald-300' : 'text-slate-500'
              }`}
            >
              <Icon className="h-5 w-5" aria-hidden />
              {t(key)}
            </NavLink>
          )
        })}
      </nav>
    </div>
  )
}