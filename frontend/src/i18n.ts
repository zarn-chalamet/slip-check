import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import en from './i18n/en.json'
import th from './i18n/th.json'
import my from './i18n/my.json'

export const LANGUAGES = [
  { code: 'en', label: 'English' },
  { code: 'th', label: 'ไทย' },
  { code: 'my', label: 'မြန်မာ' },
] as const

export type LanguageCode = (typeof LANGUAGES)[number]['code']

const stored = localStorage.getItem('slipcheck.lang')

i18n.use(initReactI18next).init({
  resources: {
    en: { translation: en },
    th: { translation: th },
    my: { translation: my },
  },
  lng: stored && ['en', 'th', 'my'].includes(stored) ? stored : 'en',
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
})

export function setLanguage(code: LanguageCode) {
  localStorage.setItem('slipcheck.lang', code)
  void i18n.changeLanguage(code)
}

export default i18n
