import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle, Link2, MessageCircle, Send, Trash2, Unlink } from 'lucide-react'
import {
  errorMessage,
  sellerApi
} from '../api'
import { LANGUAGES, type LanguageCode } from '../i18n'
import { useAuth } from '../hook/useAuth'
import type { ChannelDto, LinkCode } from '../types/type'

const TIME_ZONES = ['Asia/Bangkok', 'Asia/Yangon', 'UTC']

export default function SettingsPage() {
  const { t } = useTranslation()
  const { seller, onSellerUpdated, logout } = useAuth()

  // ---- profile ----
  const [displayName, setDisplayName] = useState(seller?.displayName ?? '')
  const [language, setLanguage] = useState<LanguageCode>(seller?.language ?? 'en')
  const [timeZone, setTimeZone] = useState(seller?.timeZone ?? 'Asia/Bangkok')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  // ---- chat linking ----
  const [linkChannel, setLinkChannel] = useState<'TELEGRAM' | 'LINE'>('TELEGRAM')
  const [linkCode, setLinkCode] = useState<LinkCode | null>(null)
  const [secondsLeft, setSecondsLeft] = useState(0)
  const [channels, setChannels] = useState<ChannelDto[]>([])

  // ---- danger zone ----
  const [confirmText, setConfirmText] = useState('')

  const loadChannels = useCallback(() => {
    sellerApi.channels().then(setChannels).catch(() => setChannels([]))
  }, [])

  useEffect(() => {
    loadChannels()
  }, [loadChannels])

  useEffect(() => {
    if (!linkCode) return
    const tick = () => {
      const left = Math.max(0, Math.floor((new Date(linkCode.expiresAt).getTime() - Date.now()) / 1000))
      setSecondsLeft(left)
    }
    tick()
    const interval = window.setInterval(tick, 1000)
    return () => window.clearInterval(interval)
  }, [linkCode])

  async function saveProfile(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      const updated = await sellerApi.updateMe({ displayName: displayName.trim(), language, timeZone })
      onSellerUpdated(updated)
      setNotice(t('settings.saved'))
    } catch (err) {
      setError(errorMessage(err, t('settings.failed')))
    } finally {
      setBusy(false)
    }
  }

  async function generateCode() {
    setError(null)
    try {
      const code = await sellerApi.linkCode(linkChannel)
      setLinkCode(code)
    } catch (err) {
      setError(errorMessage(err, t('settings.failed')))
    }
  }

  async function unlink(channel: ChannelDto) {
    if (!window.confirm(t('settings.confirmUnlink'))) return
    try {
      await sellerApi.unlinkChannel(channel.id)
      setChannels((prev) => prev.filter((c) => c.id !== channel.id))
    } catch (err) {
      setError(errorMessage(err, t('settings.failed')))
    }
  }

  async function deleteAccount() {
    if (confirmText !== 'DELETE') return
    try {
      await sellerApi.deleteMe()
      logout()
    } catch (err) {
      setError(errorMessage(err, t('settings.failed')))
    }
  }

  const minutes = Math.floor(secondsLeft / 60)
  const seconds = String(secondsLeft % 60).padStart(2, '0')

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight text-slate-50">{t('settings.title')}</h1>

      {(error || notice) && (
        <p
          className={`rounded-xl px-4 py-2.5 text-sm ring-1 ${
            error
              ? 'bg-rose-500/10 text-rose-300 ring-rose-400/30'
              : 'bg-emerald-500/10 text-emerald-300 ring-emerald-400/30'
          }`}
        >
          {error ?? notice}
        </p>
      )}

      {/* Profile */}
      <form onSubmit={saveProfile} className="card space-y-4 p-5 sm:p-6">
        <h2 className="text-sm font-bold text-slate-100">{t('settings.profile')}</h2>
        <div className="grid gap-4 sm:grid-cols-3">
          <div>
            <label className="label" htmlFor="displayName">{t('settings.displayName')}</label>
            <input
              id="displayName"
              className="input"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              maxLength={120}
              required
            />
          </div>
          <div>
            <span className="label">{t('settings.language')}</span>
            <div className="grid grid-cols-3 gap-1.5">
              {LANGUAGES.map((lang) => (
                <button
                  key={lang.code}
                  type="button"
                  onClick={() => setLanguage(lang.code)}
                  className={`rounded-xl px-2 py-2 text-xs font-medium ring-1 transition ${
                    language === lang.code
                      ? 'bg-emerald-400 text-ink-950 ring-emerald-400'
                      : 'bg-ink-900/80 text-slate-300 ring-white/10 hover:bg-white/5'
                  }`}
                  aria-pressed={language === lang.code}
                >
                  {lang.label}
                </button>
              ))}
            </div>
          </div>
          <div>
            <label className="label" htmlFor="timeZone">{t('settings.timeZone')}</label>
            <input
              id="timeZone"
              className="input"
              list="timezones"
              value={timeZone}
              onChange={(e) => setTimeZone(e.target.value)}
            />
            <datalist id="timezones">
              {TIME_ZONES.map((zone) => (
                <option key={zone} value={zone} />
              ))}
            </datalist>
          </div>
        </div>
        <button type="submit" className="btn-primary" disabled={busy}>
          {busy ? t('common.loading') : t('settings.save')}
        </button>
      </form>

      {/* Chat linking */}
      <section className="card p-5 sm:p-6">
        <h2 className="text-sm font-bold text-slate-100">{t('settings.linkChat')}</h2>
        <p className="mt-1 text-sm text-slate-400">{t('settings.linkChatSubtitle')}</p>

        <div className="mt-4 flex flex-wrap items-center gap-2">
          <button
            className={`btn-ghost ${linkChannel === 'TELEGRAM' ? 'ring-emerald-400/60 bg-emerald-400/10' : ''}`}
            onClick={() => setLinkChannel('TELEGRAM')}
          >
            <Send className="h-4 w-4" aria-hidden />
            Telegram
          </button>
          <button
            className={`btn-ghost ${linkChannel === 'LINE' ? 'ring-emerald-400/60 bg-emerald-400/10' : ''}`}
            onClick={() => setLinkChannel('LINE')}
          >
            <MessageCircle className="h-4 w-4" aria-hidden />
            LINE
          </button>
          <button className="btn-primary" onClick={generateCode}>
            <Link2 className="h-4 w-4" aria-hidden />
            {t('settings.generateCode')}
          </button>
        </div>

        {linkCode && (
          <div className="mt-4 rounded-2xl bg-ink-900/80 p-5 text-center ring-1 ring-emerald-400/20">
            <p className="text-xs text-slate-500">{t('settings.codeInstructions')}</p>
            <p className="mt-2 font-mono text-2xl font-bold tracking-wider text-emerald-300">
              /link {linkCode.code}
            </p>
            <p className="mt-2 text-xs text-slate-500">
              {t('settings.codeExpires')} {secondsLeft > 0 && `· ${minutes}:${seconds}`}
            </p>
          </div>
        )}
      </section>

      {/* Linked channels */}
      <section className="card p-5 sm:p-6">
        <h2 className="text-sm font-bold text-slate-100">{t('settings.channels')}</h2>
        <div className="mt-3 space-y-2">
          {channels.length === 0 && (
            <p className="py-4 text-center text-sm text-slate-500">{t('settings.noChannels')}</p>
          )}
          {channels.map((channel) => (
            <div key={channel.id} className="flex items-center gap-3 rounded-xl bg-ink-900/60 px-4 py-3 ring-1 ring-white/5">
              <span className="chip bg-emerald-400/10 text-emerald-300 ring-emerald-400/30">
                {channel.channel}
              </span>
              <span className="flex-1 text-sm text-slate-400">
                {channel.channel === 'WEB' ? 'web' : channel.externalUserId} · {channel.language}
              </span>
              {channel.channel !== 'WEB' && (
                <button
                  className="rounded-full p-2 text-slate-500 transition hover:bg-rose-500/10 hover:text-rose-300"
                  onClick={() => unlink(channel)}
                  aria-label={t('settings.unlink')}
                  title={t('settings.unlink')}
                >
                  <Unlink className="h-4 w-4" aria-hidden />
                </button>
              )}
            </div>
          ))}
        </div>
      </section>

      {/* Danger zone */}
      <section className="card border-rose-400/20 p-5 ring-rose-400/20 sm:p-6">
        <h2 className="flex items-center gap-2 text-sm font-bold text-rose-300">
          <AlertTriangle className="h-4 w-4" aria-hidden />
          {t('settings.danger')}
        </h2>
        <p className="mt-1 text-sm text-slate-400">{t('settings.deleteWarning')}</p>
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="flex-1">
            <label className="label" htmlFor="confirmDelete">{t('settings.confirmDelete')}</label>
            <input
              id="confirmDelete"
              className="input"
              value={confirmText}
              onChange={(e) => setConfirmText(e.target.value)}
              placeholder="DELETE"
            />
          </div>
          <button
            className="btn-danger"
            disabled={confirmText !== 'DELETE'}
            onClick={deleteAccount}
          >
            <Trash2 className="h-4 w-4" aria-hidden />
            {t('settings.deleteAccount')}
          </button>
        </div>
      </section>
    </div>
  )
}
