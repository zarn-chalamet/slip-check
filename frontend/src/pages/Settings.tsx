import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle, Copy, Check, Globe, Trash2, Unlink } from 'lucide-react'
import { errorMessage, sellerApi } from '../api'
import { LANGUAGES, type LanguageCode } from '../i18n'
import { useAuth } from '../hook/useAuth'
import type { ChannelDto, LinkCode } from '../types/type'

const TIME_ZONES = ['Asia/Bangkok', 'Asia/Yangon', 'UTC']

// Centralized Bot Identifiers
const TELEGRAM_BOT_USERNAME = 'checkslipps_bot'
const LINE_BOT_ID = '380cklab'

function getTelegramDeepLink(code: string) {
  return `https://t.me/${TELEGRAM_BOT_USERNAME}?start=${code}`
}

// Mobile QR Code link (pre-fills /link 123456 on phone camera scan)
function getLineQrLink(code: string) {
  return `https://line.me/R/oaMessage/@${LINE_BOT_ID}/?%2Flink%20${code}`
}

// Web Button link (works on Desktop + Mobile browsers without redirecting)
function getLineButtonLink() {
  return `https://line.me/R/ti/p/@${LINE_BOT_ID}`
}

// Inline SVGs for uncompromised dark-theme rendering
function ChannelIcon({ channel, className = "h-5 w-5" }: { channel: string, className?: string }) {
  if (channel === 'TELEGRAM') {
    return (
      <svg className={`shrink-0 ${className}`} viewBox="0 0 24 24" fill="none" role="img" aria-label="Telegram">
        <circle cx="12" cy="12" r="12" fill="#24A1DE" />
        <path d="M5.4 11.9L17.1 7.3C17.6 7.1 18.1 7.4 18 7.9L16 16.5C15.9 17 15.4 17.2 14.9 16.9L11.5 14.4L9.8 16C9.6 16.2 9.3 16.1 9.3 15.8V13.8L15.3 9.4C15.5 9.2 15.3 8.9 15.1 9C12.8 10.4 8.5 13.1 8.5 13.1L5.4 11.9Z" fill="white" />
      </svg>
    )
  }
  if (channel === 'LINE') {
    return (
      <svg className={`shrink-0 ${className}`} viewBox="0 0 24 24" fill="none" role="img" aria-label="LINE">
        <circle cx="12" cy="12" r="12" fill="#06C755" />
        <path d="M17.5 11C17.5 8.2 15 6 12 6C9 6 6.5 8.2 6.5 11C6.5 13.1 7.9 14.9 10 15.6C10.2 15.7 10.3 15.9 10.2 16.1L9.8 17.3C9.7 17.6 10 17.8 10.2 17.6L12.8 16.1C12.9 16 13 16 13.1 16C15.6 15.8 17.5 13.6 17.5 11Z" fill="white" />
      </svg>
    )
  }
  return (
    <div className={`grid shrink-0 place-items-center rounded-full bg-emerald-400/10 text-emerald-400 ring-1 ring-emerald-400/30 ${className}`}>
      <Globe className="h-3.5 w-3.5" aria-hidden />
    </div>
  )
}

function SettingsSection({ title, description, children }: { title: string, description: string, children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-1 gap-x-8 gap-y-6 pt-10 md:grid-cols-3">
      <div className="md:col-span-1">
        <h2 className="text-sm font-semibold text-slate-100">{title}</h2>
        <p className="mt-1 flex max-w-xs text-xs text-slate-400 leading-relaxed">{description}</p>
      </div>
      <div className="md:col-span-2">
        {children}
      </div>
    </div>
  )
}

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
  const [copied, setCopied] = useState(false)

  // ---- danger zone ----
  const [confirmText, setConfirmText] = useState('')

  const [unlinkTarget, setUnlinkTarget] = useState<ChannelDto | null>(null)
  const [unlinking, setUnlinking] = useState(false)

  const loadChannels = useCallback(() => {
    sellerApi.channels().then(setChannels).catch(() => setChannels([]))
  }, [])

  useEffect(() => {
    loadChannels()
  }, [loadChannels])

  // Countdown timer for code expiry
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

  // Auto-refresh linked channels while waiting for the bot to connect
  useEffect(() => {
    if (!linkCode) return

    const interval = window.setInterval(() => {
      if (new Date(linkCode.expiresAt).getTime() <= Date.now()) {
        return
      }

      sellerApi.channels().then((fresh) => {
        setChannels((prev) => {
          const hasNewChannel = fresh.some((f) => !prev.some((p) => p.id === f.id))
          if (hasNewChannel) {
            setLinkCode(null)
            setNotice('Chat linked successfully!')
          }
          return fresh
        })
      }).catch(() => {/* ignore transient errors */})
    }, 2000)

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
    setCopied(false)
    try {
      const code = await sellerApi.linkCode(linkChannel)
      setLinkCode(code)
    } catch (err) {
      setError(errorMessage(err, t('settings.failed')))
    }
  }

  function handleCopy() {
    if (!linkCode) return
    navigator.clipboard.writeText(`/link ${linkCode.code}`)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  async function confirmUnlink() {
    if (!unlinkTarget) return
    setUnlinking(true)
    try {
      await sellerApi.unlinkChannel(unlinkTarget.id)
      setChannels((prev) => prev.filter((c) => c.id !== unlinkTarget.id))
      setNotice('Channel unlinked successfully')
      setUnlinkTarget(null)
    } catch (err) {
      setError(errorMessage(err, t('settings.failed')))
    } finally {
      setUnlinking(false)
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
    <div className="pb-12">
      <div className="mb-8">
        <h1 className="text-2xl font-extrabold tracking-tight text-slate-50">{t('settings.title')}</h1>
        <p className="mt-1 text-sm text-slate-400">Manage your account preferences and connected integrations.</p>
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

      <div className="divide-y divide-white/5 space-y-10">
        
        {/* Profile Section */}
        <SettingsSection title={t('settings.profile')} description="Update your display name, timezone, and preferred language for the dashboard.">
          <form onSubmit={saveProfile} className="card overflow-hidden bg-ink-900/40 p-1">
            <div className="space-y-5 p-5 sm:p-6">
              <div>
                <label className="mb-1.5 block text-xs font-semibold text-slate-300" htmlFor="displayName">
                  {t('settings.displayName')}
                </label>
                <input
                  id="displayName"
                  className="w-full rounded-xl bg-ink-900 px-3.5 py-2.5 text-sm text-slate-200 outline-none ring-1 ring-white/10 transition focus:ring-2 focus:ring-emerald-400/50"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  maxLength={120}
                  required
                />
              </div>

              <div className="grid gap-5 sm:grid-cols-2">
                <div>
                  <span className="mb-1.5 block text-xs font-semibold text-slate-300">{t('settings.language')}</span>
                  <div className="flex rounded-xl bg-ink-900 p-1 ring-1 ring-white/10">
                    {LANGUAGES.map((lang) => (
                      <button
                        key={lang.code}
                        type="button"
                        onClick={() => setLanguage(lang.code)}
                        className={`flex-1 rounded-lg py-2 text-xs font-semibold transition ${
                          language === lang.code
                            ? 'bg-white/10 text-slate-100 shadow'
                            : 'text-slate-400 hover:text-slate-200'
                        }`}
                        aria-pressed={language === lang.code}
                      >
                        {lang.label}
                      </button>
                    ))}
                  </div>
                </div>
                <div>
                  <label className="mb-1.5 block text-xs font-semibold text-slate-300" htmlFor="timeZone">
                    {t('settings.timeZone')}
                  </label>
                  <input
                    id="timeZone"
                    className="w-full rounded-xl bg-ink-900 px-3.5 py-2.5 text-sm text-slate-200 outline-none ring-1 ring-white/10 transition focus:ring-2 focus:ring-emerald-400/50"
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
            </div>
            <div className="flex justify-end border-t border-white/5 bg-ink-900/50 px-5 py-3 sm:px-6">
              <button
                type="submit"
                className="inline-flex items-center justify-center rounded-xl bg-emerald-400 px-4 py-2 text-xs font-bold text-ink-950 transition hover:bg-emerald-300 focus:outline-none focus:ring-2 focus:ring-emerald-400/50 focus:ring-offset-2 focus:ring-offset-ink-900 disabled:opacity-50"
                disabled={busy}
              >
                {busy ? t('common.loading') : t('settings.save')}
              </button>
            </div>
          </form>
        </SettingsSection>

        {/* Chat Linking Section */}
        <SettingsSection title={t('settings.linkChat')} description={t('settings.linkChatSubtitle')}>
          <div className="card overflow-hidden bg-ink-900/40 p-5 sm:p-6">
            <div className="grid grid-cols-2 gap-3 sm:gap-4">
              <button
                type="button"
                onClick={() => { setLinkChannel('TELEGRAM'); setLinkCode(null) }}
                className={`flex flex-col items-center justify-center gap-3 rounded-2xl border px-4 py-6 transition ${
                  linkChannel === 'TELEGRAM'
                    ? 'border-emerald-400/50 bg-emerald-400/5 shadow-[0_0_15px_rgba(52,211,153,0.05)]'
                    : 'border-white/5 bg-ink-900 hover:bg-white/[0.02]'
                }`}
              >
                <ChannelIcon channel="TELEGRAM" className="h-8 w-8" />
                <span className={`text-sm font-semibold ${linkChannel === 'TELEGRAM' ? 'text-emerald-300' : 'text-slate-300'}`}>Telegram</span>
              </button>
              
              <button
                type="button"
                onClick={() => { setLinkChannel('LINE'); setLinkCode(null) }}
                className={`flex flex-col items-center justify-center gap-3 rounded-2xl border px-4 py-6 transition ${
                  linkChannel === 'LINE'
                    ? 'border-emerald-400/50 bg-emerald-400/5 shadow-[0_0_15px_rgba(52,211,153,0.05)]'
                    : 'border-white/5 bg-ink-900 hover:bg-white/[0.02]'
                }`}
              >
                <ChannelIcon channel="LINE" className="h-8 w-8" />
                <span className={`text-sm font-semibold ${linkChannel === 'LINE' ? 'text-emerald-300' : 'text-slate-300'}`}>LINE Bot</span>
              </button>
            </div>

            <div className="mt-5 text-center">
              <button
                type="button"
                className="inline-flex w-full items-center justify-center rounded-xl bg-white/10 px-4 py-2.5 text-sm font-semibold text-slate-100 transition hover:bg-white/15 focus:outline-none focus:ring-2 focus:ring-emerald-400/50 sm:w-auto"
                onClick={generateCode}
              >
                {t('settings.generateCode')}
              </button>
            </div>

            {linkCode && (
              <div className="mt-6 overflow-hidden rounded-2xl border border-emerald-400/20 bg-emerald-400/5 animate-in fade-in zoom-in-95">
                <div className="flex flex-col items-center p-5 text-center sm:p-6">
                  {/* Channel-specific instruction */}
                  <p className="text-xs font-medium text-emerald-400/80">
                    {linkChannel === 'TELEGRAM'
                      ? 'Scan QR code or click the button below to open Telegram:'
                      : 'Scan QR code or click the button below to open LINE:'}
                  </p>

                  {/* QR Code Container */}
                  <div className="my-4 flex flex-col items-center justify-center gap-2">
                    <div className="rounded-xl border border-white/10 bg-white p-2.5 shadow-xl">
                      <img
                        src={`https://api.qrserver.com/v1/create-qr-code/?size=140x140&margin=0&data=${encodeURIComponent(
                          linkChannel === 'TELEGRAM'
                            ? getTelegramDeepLink(linkCode.code)
                            : getLineQrLink(linkCode.code)
                        )}`}
                        alt="Link QR Code"
                        className="h-32 w-32 rounded-lg"
                      />
                    </div>
                    <span className="text-[11px] text-slate-400">
                      {linkChannel === 'TELEGRAM'
                        ? 'Scan with phone camera or Telegram scanner'
                        : 'Scan with phone camera or LINE QR scanner'}
                    </span>
                  </div>

                  {/* Action Stack */}
                  <div className="mt-2 flex w-full max-w-xs flex-col items-center gap-3">
                  
                    {/* Open App Button */}
                    {linkChannel === 'TELEGRAM' ? (
                      <a
                        href={getTelegramDeepLink(linkCode.code)}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-[#24A1DE] px-4 py-2.5 text-xs font-bold text-white shadow-md transition hover:bg-[#1f90c1] focus:outline-none focus:ring-2 focus:ring-[#24A1DE]/50"
                      >
                        <ChannelIcon channel="TELEGRAM" className="h-4 w-4" />
                        Open in Telegram
                      </a>
                    ) : (
                      <a
                        href={getLineButtonLink()}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-[#06C755] px-4 py-2.5 text-xs font-bold text-white shadow-md transition hover:bg-[#05b34c] focus:outline-none focus:ring-2 focus:ring-[#06C755]/50"
                      >
                        <ChannelIcon channel="LINE" className="h-4 w-4" />
                        Open in LINE
                      </a>
                    )}

                    {/* Divider */}
                    <div className="flex w-full items-center gap-2 px-1">
                      <div className="h-px flex-1 bg-white/10" />
                      <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Or type manually</span>
                      <div className="h-px flex-1 bg-white/10" />
                    </div>

                    {/* Command Code Box */}
                    <div className="inline-flex w-full items-center justify-between gap-2 rounded-xl bg-ink-950 px-3.5 py-2.5 ring-1 ring-emerald-400/30">
                      <code className="font-mono text-sm font-bold tracking-wider text-emerald-300">
                        /link {linkCode.code}
                      </code>
                      <button
                        type="button"
                        onClick={handleCopy}
                        className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-400/10 px-2.5 py-1 text-xs font-semibold text-emerald-300 transition hover:bg-emerald-400/20 focus:outline-none"
                        title="Copy code"
                      >
                        {copied ? (
                          <>
                            <Check className="h-3.5 w-3.5 text-emerald-400" />
                            <span>Copied</span>
                          </>
                        ) : (
                          <>
                            <Copy className="h-3.5 w-3.5" />
                            <span>Copy</span>
                          </>
                        )}
                      </button>
                    </div>
                  </div>

                  {/* Expiration Timer */}
                  <div className="mt-5 flex items-center justify-center gap-2">
                    <span className="relative flex h-2 w-2">
                      {secondsLeft > 0 && (
                        <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
                      )}
                      <span
                        className={`relative inline-flex h-2 w-2 rounded-full ${
                          secondsLeft > 0 ? 'bg-emerald-400' : 'bg-slate-600'
                        }`}
                      />
                    </span>
                    <p className="text-xs font-medium text-slate-400">
                      {t('settings.codeExpires')}{' '}
                      {secondsLeft > 0 ? (
                        <span className="tabular-nums font-semibold text-slate-300">
                          {minutes}:{seconds}
                        </span>
                      ) : (
                        <span className="font-semibold text-rose-400">Expired</span>
                      )}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>
        </SettingsSection>

        {/* Linked Channels Section */}
        <SettingsSection title={t('settings.channels')} description="Review and manage external messaging apps actively authenticated with your account.">
          <div className="card overflow-hidden bg-ink-900/40">
            <div className="divide-y divide-white/5">
              {channels.length === 0 ? (
                <div className="p-8 text-center">
                  <p className="text-sm font-medium text-slate-500">{t('settings.noChannels')}</p>
                </div>
              ) : (
                channels.map((channel) => (
                  <div key={channel.id} className="flex items-center gap-4 p-4 transition-colors hover:bg-white/[0.02] sm:px-6">
                    <ChannelIcon channel={channel.channel} className="h-10 w-10 sm:h-8 sm:w-8" />
                    
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold text-slate-200">
                        {channel.channel === 'WEB' ? 'Web Dashboard' : channel.externalUserId}
                      </p>
                      <p className="mt-0.5 text-xs text-slate-500">
                        Authenticated · {channel.language.toUpperCase()}
                      </p>
                    </div>

                    {channel.channel !== 'WEB' && (
                      <button
                        type="button"
                        className="inline-flex items-center gap-2 rounded-xl px-3 py-1.5 text-xs font-semibold text-rose-400/80 transition hover:bg-rose-500/10 hover:text-rose-300 focus:outline-none"
                        onClick={() => setUnlinkTarget(channel)}
                      >
                        <Unlink className="h-3.5 w-3.5" aria-hidden />
                        <span className="hidden sm:inline">{t('settings.unlink')}</span>
                      </button>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
        </SettingsSection>

        {/* Danger Zone Section */}
        <SettingsSection title="Danger Zone" description="Irreversible actions. Deleting your account will immediately wipe all associated data and revoke API access.">
          <div className="card overflow-hidden border-rose-500/20 bg-rose-500/[0.02]">
            <div className="p-5 sm:p-6">
              <div className="flex items-center gap-2 text-rose-400">
                <AlertTriangle className="h-5 w-5" aria-hidden />
                <h3 className="text-sm font-bold">{t('settings.danger')}</h3>
              </div>
              <p className="mt-2 text-sm text-slate-400 leading-relaxed">{t('settings.deleteWarning')}</p>
              
              <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-end">
                <div className="flex-1">
                  <label className="mb-1.5 block text-xs font-semibold text-slate-300" htmlFor="confirmDelete">
                    {t('settings.confirmDelete')}
                  </label>
                  <input
                    id="confirmDelete"
                    className="w-full rounded-xl bg-ink-900 px-3.5 py-2.5 font-mono text-sm text-slate-200 outline-none ring-1 ring-white/10 transition focus:ring-2 focus:ring-rose-500/50"
                    value={confirmText}
                    onChange={(e) => setConfirmText(e.target.value)}
                    placeholder="DELETE"
                    autoComplete="off"
                  />
                </div>
                <button
                  type="button"
                  className="inline-flex h-[42px] shrink-0 items-center justify-center gap-2 rounded-xl bg-rose-500/10 px-5 text-sm font-semibold text-rose-400 ring-1 ring-inset ring-rose-500/20 transition hover:bg-rose-500 hover:text-white disabled:pointer-events-none disabled:opacity-50"
                  disabled={confirmText !== 'DELETE'}
                  onClick={deleteAccount}
                >
                  <Trash2 className="h-4 w-4" aria-hidden />
                  {t('settings.deleteAccount')}
                </button>
              </div>
            </div>
          </div>
        </SettingsSection>

        {/* Unlink confirmation modal */}
        {unlinkTarget && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            {/* Backdrop */}
            <div
              className="absolute inset-0 bg-black/60 backdrop-blur-sm"
              onClick={() => !unlinking && setUnlinkTarget(null)}
            />

            {/* Dialog */}
            <div className="relative w-full max-w-sm animate-in fade-in zoom-in-95 rounded-2xl border border-white/10 bg-ink-900 p-6 shadow-2xl">
              <div className="flex items-start gap-4">
                <div className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-rose-500/10 ring-1 ring-rose-400/30">
                  <Unlink className="h-5 w-5 text-rose-400" />
                </div>
                <div className="min-w-0 flex-1">
                  <h3 className="text-sm font-bold text-slate-100">Unlink this chat?</h3>
                  <p className="mt-1 text-xs leading-relaxed text-slate-400">
                    Slips sent from{' '}
                    <span className="font-medium text-slate-300">
                      {unlinkTarget.channel === 'TELEGRAM' ? 'Telegram' : 'LINE'}
                    </span>
                    {unlinkTarget.externalUserId ? (
                      <>
                        {' '}
                        <span className="font-mono text-slate-300">
                          …{unlinkTarget.externalUserId.slice(-4)}
                        </span>
                      </>
                    ) : null}{' '}
                    will no longer reach your account.
                  </p>
                </div>
              </div>

              <div className="mt-6 flex gap-3">
                <button
                  type="button"
                  disabled={unlinking}
                  onClick={() => setUnlinkTarget(null)}
                  className="flex-1 rounded-xl bg-white/5 px-4 py-2.5 text-xs font-semibold text-slate-300 ring-1 ring-white/10 transition hover:bg-white/10 disabled:opacity-50"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  disabled={unlinking}
                  onClick={confirmUnlink}
                  className="flex-1 rounded-xl bg-rose-500 px-4 py-2.5 text-xs font-bold text-white transition hover:bg-rose-400 disabled:opacity-50"
                >
                  {unlinking ? 'Unlinking…' : 'Unlink'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}