import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Landmark, Plus, Trash2 } from 'lucide-react'
import { accountApi, errorMessage, } from '../api'
import type { Account } from '../types/type'

const THAI_BANKS = [
  { code: 'KBANK', name: 'Kasikornbank' },
  { code: 'SCB', name: 'Siam Commercial Bank' },
  { code: 'BBL', name: 'Bangkok Bank' },
  { code: 'KTB', name: 'Krungthai Bank' },
  { code: 'BAY', name: 'Krungsri' },
  { code: 'TTB', name: 'ttb bank' },
  { code: 'GSB', name: 'Government Savings Bank' },
  { code: 'CIMB', name: 'CIMB Thai' },
  { code: 'UOB', name: 'UOB Thailand' },
  { code: 'LHBANK', name: 'Land and Houses Bank' },
]

export default function Accounts() {
  const { t } = useTranslation()
  const [accounts, setAccounts] = useState<Account[]>([])
  const [bankCode, setBankCode] = useState('KBANK')
  const [accountNo, setAccountNo] = useState('')
  const [holderName, setHolderName] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

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

  async function remove(id: string) {
    if (!window.confirm(t('accounts.confirmRemove'))) return
    setError(null)
    setNotice(null)
    try {
      await accountApi.remove(id)
      setNotice(t('accounts.removed'))
      setAccounts((prev) => prev.filter((a) => a.id !== id))
    } catch (err) {
      setError(errorMessage(err, t('accounts.failed')))
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
      <section>
        <h1 className="text-2xl font-bold tracking-tight text-slate-50">{t('accounts.title')}</h1>
        <p className="mt-1 text-sm text-slate-400">{t('accounts.subtitle')}</p>

        {(error || notice) && (
          <p
            className={`mt-4 rounded-xl px-4 py-2.5 text-sm ring-1 ${
              error
                ? 'bg-rose-500/10 text-rose-300 ring-rose-400/30'
                : 'bg-emerald-500/10 text-emerald-300 ring-emerald-400/30'
            }`}
          >
            {error ?? notice}
          </p>
        )}

        <div className="mt-5 space-y-2">
          {loading && <p className="py-8 text-center text-sm text-slate-500">{t('common.loading')}</p>}

          {!loading && accounts.length === 0 && (
            <div className="card p-8 text-center text-sm text-slate-400">{t('accounts.empty')}</div>
          )}

          {accounts.map((account) => (
            <div key={account.id} className="card flex items-center gap-3 px-4 py-3">
              <span className="grid h-10 w-10 place-items-center rounded-xl bg-emerald-400/10 ring-1 ring-emerald-400/30">
                <Landmark className="h-5 w-5 text-emerald-300" aria-hidden />
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-slate-100">
                  {THAI_BANKS.find((b) => b.code === account.bankCode)?.name ?? account.bankCode}
                </p>
                <p className="text-xs text-slate-500">
                  {account.holderName} · {t('accounts.last4', { last4: account.accountLast4 })}
                </p>
              </div>
              <button
                className="rounded-full p-2 text-slate-500 transition hover:bg-rose-500/10 hover:text-rose-300"
                onClick={() => remove(account.id)}
                aria-label={t('common.remove')}
                title={t('common.remove')}
              >
                <Trash2 className="h-4 w-4" aria-hidden />
              </button>
            </div>
          ))}
        </div>
      </section>

      <aside>
        <form onSubmit={add} className="card space-y-4 p-5">
          <h2 className="text-sm font-bold text-slate-100">{t('accounts.add')}</h2>

          <div>
            <label className="label" htmlFor="bank">{t('accounts.bank')}</label>
            <select
              id="bank"
              className="input"
              value={bankCode}
              onChange={(e) => setBankCode(e.target.value)}
            >
              {THAI_BANKS.map((bank) => (
                <option key={bank.code} value={bank.code}>
                  {bank.name} ({bank.code})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="label" htmlFor="accountNo">{t('accounts.accountNo')}</label>
            <input
              id="accountNo"
              className="input"
              inputMode="numeric"
              value={accountNo}
              onChange={(e) => setAccountNo(e.target.value)}
              placeholder={t('accounts.accountNoPlaceholder')}
              required
            />
          </div>

          <div>
            <label className="label" htmlFor="holderName">{t('accounts.holderName')}</label>
            <input
              id="holderName"
              className="input"
              value={holderName}
              onChange={(e) => setHolderName(e.target.value)}
              placeholder={t('accounts.holderPlaceholder')}
              required
              maxLength={160}
            />
          </div>

          <button type="submit" className="btn-primary w-full" disabled={busy}>
            <Plus className="h-4 w-4" aria-hidden />
            {busy ? t('common.loading') : t('accounts.add')}
          </button>
        </form>
      </aside>
    </div>
  )
}
