import { useState, useEffect, type ReactNode } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { LogOut, Menu, ShieldCheck, X } from 'lucide-react'
import { useAuth } from '../../hook/useAuth'
import { LanguageSwitcher } from './LanguageSwitcher'
import { NAV_ITEMS } from './nav-items'

export function Layout({ children }: { children: ReactNode }) {
  const { t } = useTranslation()
  const { seller, logout } = useAuth()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  // Prevent background scrolling when mobile menu overlay is open
  useEffect(() => {
    if (mobileMenuOpen) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
    return () => {
      document.body.style.overflow = ''
    }
  }, [mobileMenuOpen])

  const initial = seller?.displayName?.charAt(0).toUpperCase() || 'S'

  return (
    <div className="min-h-screen bg-ink-950">
      {/* --- SAAS HEADER --- */}
      <header className="sticky top-0 z-50 w-full border-b border-white/5 bg-ink-950/70 backdrop-blur-xl supports-[backdrop-filter]:bg-ink-950/60">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          
          <div className="flex items-center gap-8">
            {/* Logo */}
            <Link
              to="/"
              onClick={() => setMobileMenuOpen(false)}
              className="flex items-center gap-2.5 transition-opacity hover:opacity-80"
            >
              <span className="grid h-8 w-8 place-items-center rounded-lg bg-emerald-400">
                <ShieldCheck className="h-5 w-5 text-ink-950" aria-hidden />
              </span>
              <span className="text-base font-bold tracking-tight text-slate-50">SlipCheck</span>
            </Link>

            {/* Desktop Navigation */}
            <nav className="hidden items-center gap-6 md:flex" aria-label="Primary">
              {NAV_ITEMS.map(({ to, key }) => (
                <NavLink
                  key={to}
                  to={to}
                  end={to === '/'}
                  className={({ isActive }) =>
                    `relative py-5 text-sm font-medium transition-colors ${
                      isActive ? 'text-slate-50' : 'text-slate-400 hover:text-slate-200'
                    }`
                  }
                >
                  {({ isActive }) => (
                    <>
                      {t(key)}
                      {/* Active Bottom Border */}
                      {isActive && (
                        <span className="absolute inset-x-0 bottom-0 h-[2px] bg-emerald-400" />
                      )}
                    </>
                  )}
                </NavLink>
              ))}
            </nav>
          </div>

          {/* Right Tools */}
          <div className="flex items-center gap-2 sm:gap-4">
            <LanguageSwitcher />

            <div className="hidden h-5 w-px bg-white/10 sm:block" />

            {/* User Profile */}
            <div className="hidden items-center gap-3 sm:flex">
              <span className="text-sm font-medium text-slate-300">{seller?.displayName}</span>
              <div className="grid h-8 w-8 place-items-center rounded-full bg-gradient-to-br from-emerald-400/20 to-emerald-400/5 text-sm font-bold text-emerald-400 ring-1 ring-emerald-400/20">
                {initial}
              </div>
            </div>

            <button
              onClick={logout}
              className="hidden rounded-lg p-2 text-slate-400 transition-colors hover:bg-rose-500/10 hover:text-rose-400 sm:block"
              title={t('nav.logout')}
            >
              <LogOut className="h-4 w-4" />
            </button>

            {/* Mobile Menu Button */}
            <button
              className="ml-2 rounded-lg p-2 text-slate-400 transition-colors hover:bg-white/5 hover:text-slate-200 md:hidden"
              onClick={() => setMobileMenuOpen((open) => !open)}
              aria-expanded={mobileMenuOpen}
              aria-label="Toggle Menu"
            >
              {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </button>
          </div>
        </div>
      </header>

      {/* --- MOBILE FULL-SCREEN MENU --- */}
      {mobileMenuOpen && (
        <div className="fixed inset-0 top-16 z-40 bg-ink-950/95 backdrop-blur-xl animate-in fade-in duration-200 md:hidden">
          <nav className="flex h-full flex-col px-4 py-6">
            <div className="flex-1 space-y-2">
              {NAV_ITEMS.map(({ to, key, icon: Icon }) => (
                <NavLink
                  key={to}
                  to={to}
                  end={to === '/'}
                  onClick={() => setMobileMenuOpen(false)}
                  className={({ isActive }) =>
                    `flex items-center gap-3 rounded-xl px-4 py-3.5 text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-emerald-400/10 text-emerald-400'
                        : 'text-slate-300 hover:bg-white/5'
                    }`
                  }
                >
                  <Icon className="h-5 w-5" />
                  {t(key)}
                </NavLink>
              ))}
            </div>

            <div className="mt-auto border-t border-white/10 pb-8 pt-6">
              <div className="mb-6 flex items-center gap-3 px-4">
                <div className="grid h-10 w-10 place-items-center rounded-full bg-emerald-400/10 text-base font-bold text-emerald-400 ring-1 ring-emerald-400/20">
                  {initial}
                </div>
                <div>
                  <p className="text-sm font-medium text-slate-200">{seller?.displayName}</p>
                  <p className="text-xs text-slate-500">{seller?.email}</p>
                </div>
              </div>
              <button
                onClick={() => {
                  setMobileMenuOpen(false)
                  logout()
                }}
                className="flex w-full items-center gap-3 rounded-xl px-4 py-3.5 text-sm font-medium text-rose-400 transition-colors hover:bg-rose-500/10"
              >
                <LogOut className="h-5 w-5" />
                {t('nav.logout')}
              </button>
            </div>
          </nav>
        </div>
      )}

      {/* --- MAIN CONTENT --- */}
      <main className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="animate-in fade-in slide-in-from-bottom-2 duration-500 ease-out">
          {children}
        </div>
      </main>
    </div>
  )
}