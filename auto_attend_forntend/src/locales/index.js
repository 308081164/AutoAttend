import Vue from 'vue'
import VueI18n from 'vue-i18n'
import zh from './zh'
import en from './en'
import ru from './ru'
import ja from './ja'
import fr from './fr'

Vue.use(VueI18n)

const messages = {
  zh,
  en,
  ru,
  ja,
  fr
}

const savedLocale = typeof localStorage !== 'undefined' ? localStorage.getItem('autoattend_locale') : null
const fallbackLocale = 'zh'

export const i18n = new VueI18n({
  locale: savedLocale && messages[savedLocale] ? savedLocale : fallbackLocale,
  fallbackLocale,
  messages
})

export function setLocale (lang) {
  if (messages[lang]) {
    i18n.locale = lang
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('autoattend_locale', lang)
    }
  }
}

export const localeOptions = [
  { value: 'zh', label: '中文' },
  { value: 'en', label: 'English' },
  { value: 'ru', label: 'Русский' },
  { value: 'ja', label: '日本語' },
  { value: 'fr', label: 'Français' }
]
