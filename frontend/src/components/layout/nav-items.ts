import { ClipboardList, History, Landmark, ScanLine, Settings } from 'lucide-react'

export const NAV_ITEMS = [
  { to: '/', key: 'nav.checkSlip', icon: ScanLine },
  { to: '/history', key: 'nav.history', icon: History },
  { to: '/review', key: 'nav.review', icon: ClipboardList },
  { to: '/accounts', key: 'nav.accounts', icon: Landmark },
  { to: '/settings', key: 'nav.settings', icon: Settings },
] as const