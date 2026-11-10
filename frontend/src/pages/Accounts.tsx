import { useCallback, useEffect, useState, useRef, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Landmark, Plus, Trash2, ChevronDown, Check, AlertTriangle } from 'lucide-react'
import { accountApi, errorMessage } from '../api'
import type { Account } from '../types/type'

const THAI_BANKS = [
  { code: 'KBANK', name: 'Kasikornbank', color: '#00A950', file: '/banks/kbank.jpg' },
  { code: 'SCB', name: 'Siam Commercial Bank', color: '#4E2E7F', file: '/banks/scb.jpeg' },
  { code: 'BBL', name: 'Bangkok Bank', color: '#1E4598', file: '/banks/bbl.jpg' },
  { code: 'KTB', name: 'Krungthai Bank', color: '#00AEEF', file: '/banks/ktb.png' },
  { code: 'BAY', name: 'Krungsri', color: '#FEC43B', file: '/banks/bay.png' },
  { code: 'TTB', name: 'ttb bank', color: '#0050F0', file: '/banks/ttb.jpeg' },
  { code: 'GSB', name: 'Government Savings Bank', color: '#EB198D', file: '/banks/gsb.png' },
  { code: 'CIMB', name: 'CIMB Thai', color: '#7E2A24', file: '/banks/cimb.png' },
  { code: 'UOB', name: 'UOB Thailand', color: '#00377B', file: '/banks/uob.webp' },
  { code: 'LHBANK', name: 'Land and Houses Bank', color: '#6A6A6A', file: '/banks/lhbank.png' },
]

function BankLogo({ code, className = "h-6 w-6" }: { code: string; className?: string }) {
  const bank = THAI_BANKS.find((b) => b.code === code) || { color: '#6A6A6A', code, file: '' }
  const [failed, setFailed] = useState(false)

  if (failed || !bank.file) {
    return (
      <div
        className={`grid shrink-0 place-items-center rounded-lg font-bold text-[10px] text-white ring-1 ring-white/20 ${className}`}
        style={{ backgroundColor: bank.color }}
      >
        {code.slice(0, 3)}
      </div>
    )
  }

  return (
    <img
      src={bank.file}
      alt={code}
      onError={() => setFailed(true)}
      className={`shrink-0 rounded-md object-contain ${className}`}
    />
  )
}

function BankDropdown({
  value,
  onChange,
}: {
  value: string
  onChange: (val: string) => void
}) {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  const selected = THAI_BANKS.find((b) => b.code === value) || THAI_BANKS[0]

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="flex w-full items-center justify-between rounded-xl bg-ink-900 px-3.5 py-2.5 text-sm text-slate-200 outline-none ring-1 ring-white/10 transition focus:ring-2 focus:ring-emerald-400/50"
      >
        <span className="flex items-center gap-2.5 truncate">
          <BankLogo code={selected.code} className="h-5 w-5" />
          <span className="truncate">{selected.name} ({selected.code})</span>
        </span>
        <ChevronDown className={`h-4 w-4 shrink-0 text-slate-400 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <div className="absolute left-0 top-full z-50 mt-2 max-h-60 w-full overflow-y-auto rounded-xl border border-white/10 bg-ink-900 p-1.5 shadow-2xl">
          {THAI_BANKS.map((bank) => (
            <button
              key={bank.code}
              type="button"
              onClick={() => {
                onChange(bank.code)
                setOpen(false)
              }}
              className={`flex w-full items-center justify-between rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                value === bank.code ? 'bg-emerald-400/10 text-emerald-300' : 'text-slate-300 hover:bg-white/5'
              }`}
            >
              <span className="flex items-center gap-2.5 truncate">
                <BankLogo code={bank.code} className="h-5 w-5" />
                <span className="truncate">{bank.name}</span>
              </span>
              {value === bank.code && <Check className="h-4 w-4 shrink-0" />}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

export default function Accounts() {
  const { t } = useTranslation()
  const [accounts, setAccounts] = useState<Account[]>([])
  
  // Form State
  const [bankCode, setBankCode] = useState('KBANK')
  const [accountNo, setAccountNo] = useState('')
  const [holderName, setHolderName] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  // Modal State
  const [deleteTarget, setDeleteTarget] = useState<Account | null>(null)
  const [deleting, setDeleting] = useState(false)

  const load = useCallback(() => {
    accountApi
      .list()
      .then(setAccounts)
      .catch(() => setAccounts([]))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  // Auto-clear alerts
  useEffect(() => {
    let timeout: number
    if (notice || error) {
      timeout = window.setTimeout(() => {
        setNotice(null)
        setError(null)
      }, 5000)
    }
    return () => window.clearTimeout(timeout)
  }, [notice, error])

  async function add(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      await accountApi.add(bankCode, accountNo, holderName)
      setAccountNo('')
      setHolderName('')
      setNotice(t('accounts.added'))
      load()
    } catch (err) {
      setError(errorMessage(err, t('accounts.failed')))
    } finally {
      setBusy(false)
    }
  }

  async function confirmRemove() {
    if (!deleteTarget) return
    setDeleting(true)
    setError(null)
    setNotice(null)
    try {
      await accountApi.remove(deleteTarget.id)
      setNotice(t('accounts.removed'))
      setAccounts((prev) => prev.filter((a) => a.id !== deleteTarget.id))
      setDeleteTarget(null)
    } catch (err) {
      setError(errorMessage(err, t('accounts.failed')))
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="pb-12">
      <div className="mb-8">
        <h1 className="text-2xl font-extrabold tracking-tight text-slate-50">{t('accounts.title')}</h1>
        <p className="mt-1 text-sm text-slate-400">{t('accounts.subtitle')}</p>
      </div>

      {(error || notice) && (
        <div
          className={`mb-6 flex animate-in fade-in slide-in-from-top-2 items-center rounded-xl px-4 py-3 text-sm font-medium ring-1 ${
            error
              ? 'bg-rose-500/10 text-rose-300 ring-rose-400/30'
              : 'bg-emerald-500/10 text-emerald-300 ring-emerald-400/30'
          }`}
        >
          {error ?? notice}
        </div>
      )}

      <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-[1fr_360px]">
        
        {/* Left Side: Account List */}
        <section className="card overflow-hidden bg-ink-900/40">
          <div className="border-b border-white/5 bg-ink-900/40 px-5 py-4">
            <h2 className="text-sm font-bold text-slate-100">Registered Accounts</h2>
          </div>
          
          <div className="divide-y divide-white/5">
            {loading ? (
              <div className="flex animate-pulse items-center gap-4 p-5">
                <div className="h-10 w-10 rounded-xl bg-white/10" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-1/3 rounded bg-white/10" />
                  <div className="h-3 w-1/4 rounded bg-white/5" />
                </div>
              </div>
            ) : accounts.length === 0 ? (
              <div className="flex flex-col items-center justify-center p-12 text-center">
                <div className="grid h-12 w-12 place-items-center rounded-full bg-white/5 ring-1 ring-white/10">
                  <Landmark className="h-5 w-5 text-slate-500" aria-hidden />
                </div>
                <p className="mt-4 text-sm font-medium text-slate-400">{t('accounts.empty')}</p>
              </div>
            ) : (
              accounts.map((account) => {
                const bank = THAI_BANKS.find((b) => b.code === account.bankCode)
                return (
                  <div key={account.id} className="flex items-center gap-4 p-5 transition-colors hover:bg-white/[0.02]">
                    <div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-ink-900 p-1.5 ring-1 ring-white/10 shadow-sm">
                      <BankLogo code={account.bankCode} className="h-7 w-7" />
                    </div>
                    
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-bold text-slate-200">
                        {bank?.name ?? account.bankCode}
                      </p>
                      <p className="mt-0.5 truncate text-xs font-medium text-slate-500">
                        {account.holderName} <span className="mx-1 text-slate-600">•</span> <span className="font-mono text-slate-400">*{account.accountLast4}</span>
                      </p>
                    </div>

                    <button
                      type="button"
                      className="shrink-0 rounded-lg p-2 text-slate-500 transition hover:bg-rose-500/10 hover:text-rose-400 focus:outline-none"
                      onClick={() => setDeleteTarget(account)}
                      title={t('common.remove')}
                    >
                      <Trash2 className="h-4 w-4" aria-hidden />
                    </button>
                  </div>
                )
              })
            )}
          </div>
        </section>

        {/* Right Side: Add Form */}
        <aside>
          <form onSubmit={add} className="card sticky top-24 overflow-hidden bg-ink-900/40">
            <div className="border-b border-white/5 bg-ink-900/40 px-5 py-4">
              <h2 className="text-sm font-bold text-slate-100">{t('accounts.add')}</h2>
            </div>
            
            <div className="space-y-4 p-5">
              <div>
                <label className="mb-1.5 block text-xs font-semibold text-slate-300">
                  {t('accounts.bank')}
                </label>
                <BankDropdown value={bankCode} onChange={setBankCode} />
              </div>

              <div>
                <label className="mb-1.5 block text-xs font-semibold text-slate-300" htmlFor="accountNo">
                  {t('accounts.accountNo')}
                </label>
                <input
                  id="accountNo"
                  className="w-full rounded-xl bg-ink-900 px-3.5 py-2.5 font-mono text-sm text-slate-200 outline-none ring-1 ring-white/10 transition focus:ring-2 focus:ring-emerald-400/50"
                  inputMode="numeric"
                  value={accountNo}
                  onChange={(e) => setAccountNo(e.target.value)}
                  placeholder={t('accounts.accountNoPlaceholder')}
                  required
                  autoComplete="off"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-xs font-semibold text-slate-300" htmlFor="holderName">
                  {t('accounts.holderName')}
                </label>
                <input
                  id="holderName"
                  className="w-full rounded-xl bg-ink-900 px-3.5 py-2.5 text-sm text-slate-200 outline-none ring-1 ring-white/10 transition focus:ring-2 focus:ring-emerald-400/50"
                  value={holderName}
                  onChange={(e) => setHolderName(e.target.value)}
                  placeholder={t('accounts.holderPlaceholder')}
                  required
                  maxLength={160}
                  autoComplete="off"
                />
              </div>
            </div>

            <div className="border-t border-white/5 bg-ink-900/50 p-5">
              <button 
                type="submit" 
                className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-xl bg-emerald-400 px-4 text-sm font-bold text-ink-950 transition hover:bg-emerald-300 focus:outline-none focus:ring-2 focus:ring-emerald-400/50 disabled:opacity-50" 
                disabled={busy}
              >
                <Plus className="h-4 w-4" aria-hidden />
                {busy ? t('common.loading') : t('accounts.add')}
              </button>
            </div>
          </form>
        </aside>
      </div>

      {/* Delete Confirmation Modal */}
      {deleteTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => !deleting && setDeleteTarget(null)} />
          <div className="relative w-full max-w-sm animate-in fade-in zoom-in-95 rounded-2xl border border-white/10 bg-ink-900 p-6 shadow-2xl">
            <div className="flex items-start gap-4">
              <div className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-rose-500/10 ring-1 ring-rose-400/30">
                <AlertTriangle className="h-5 w-5 text-rose-400" />
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="text-sm font-bold text-slate-100">Remove Account?</h3>
                <p className="mt-1 text-xs leading-relaxed text-slate-400">
                  Are you sure you want to remove <span className="font-semibold text-slate-200">*{deleteTarget.accountLast4}</span>? Bank slips paid to this account will no longer be verified.
                </p>
              </div>
            </div>
            <div className="mt-6 flex gap-3">
              <button
                type="button"
                disabled={deleting}
                onClick={() => setDeleteTarget(null)}
                className="flex-1 rounded-xl bg-white/5 px-4 py-2.5 text-xs font-semibold text-slate-300 ring-1 ring-white/10 transition hover:bg-white/10 disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deleting}
                onClick={confirmRemove}
                className="flex-1 rounded-xl bg-rose-500 px-4 py-2.5 text-xs font-bold text-white transition hover:bg-rose-400 disabled:opacity-50"
              >
                {deleting ? 'Removing…' : 'Remove'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}