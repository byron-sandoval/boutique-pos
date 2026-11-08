import { legacyPanelUrl } from './paths'

const navigationEvent = 'boutique:navigate'

export function initializeNavigation() {
  // Conserva los enlaces antiguos sin tocar los fragmentos del callback de OAuth.
  const legacyUrl = legacyPanelUrl(window.location)
  if (legacyUrl) window.history.replaceState(window.history.state, '', legacyUrl)
}

export function subscribeNavigation(listener: () => void) {
  window.addEventListener('popstate', listener)
  window.addEventListener(navigationEvent, listener)
  return () => {
    window.removeEventListener('popstate', listener)
    window.removeEventListener(navigationEvent, listener)
  }
}

export function navigate(url: URL) {
  if (url.origin !== window.location.origin) throw new Error('La ruta debe pertenecer al panel.')
  const next = url.pathname + url.search + url.hash
  const current = window.location.pathname + window.location.search + window.location.hash
  if (next !== current) {
    window.history.pushState(null, '', next)
    window.dispatchEvent(new Event(navigationEvent))
  }
  window.scrollTo(0, 0)
  document.getElementById('main-content')?.focus({ preventScroll: true })
}
