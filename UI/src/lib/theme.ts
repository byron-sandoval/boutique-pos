type Theme = 'light' | 'dark'

const storageKey = 'boutique-theme'
const listeners = new Set<() => void>()
let theme: Theme = 'light'

function applyTheme(next: Theme) {
  theme = next
  document.documentElement.classList.toggle('dark', next === 'dark')
  document.documentElement.style.colorScheme = next
  listeners.forEach(listener => listener())
}

export function initializeTheme() {
  let saved: string | null = null
  try { saved = window.localStorage.getItem(storageKey) } catch { /* El tema sigue funcionando sin almacenamiento. */ }
  applyTheme(saved === 'dark' || saved === 'light'
    ? saved
    : window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
}

export function getTheme() {
  return theme
}

export function subscribeTheme(listener: () => void) {
  listeners.add(listener)
  return () => { listeners.delete(listener) }
}

export function toggleTheme() {
  const next = theme === 'dark' ? 'light' : 'dark'
  try { window.localStorage.setItem(storageKey, next) } catch { /* La elección se conserva durante esta sesión. */ }
  applyTheme(next)
}
