import Keycloak from 'keycloak-js'
import { config } from '../config'
import { loginRedirectUri } from '../routes/paths'

export type Role = 'ADMIN' | 'CAJERO'
export type Session = {
  status: 'loading' | 'anonymous' | 'authenticated' | 'expired' | 'error'
  name: string
  roles: Role[]
}

const keycloak = new Keycloak({
  url: config.keycloakUrl,
  realm: config.keycloakRealm,
  clientId: config.keycloakClientId,
})
let session: Session = { status: 'loading', name: '', roles: [] }
const listeners = new Set<() => void>()
let initialization: Promise<void> | undefined

function publish(status: Session['status']) {
  const claims = keycloak.tokenParsed
  session = {
    status,
    name: status === 'authenticated' ? claims?.name || claims?.preferred_username || 'Usuario' : '',
    roles: status === 'authenticated'
      ? (['ADMIN', 'CAJERO'] as const).filter(role => keycloak.hasRealmRole(role))
      : [],
  }
  listeners.forEach(listener => listener())
}

export function subscribeSession(listener: () => void) {
  listeners.add(listener)
  return () => { listeners.delete(listener) }
}

export function getSession() {
  return session
}

export function expireSession() {
  if (session.status === 'expired') return
  publish('expired')
  keycloak.redirectUri = loginRedirectUri(window.location)
  void keycloak.clearToken().catch(() => publish('error'))
}

export function initializeSession() {
  // Una sola inicialización, también bajo React StrictMode.
  initialization ??= (async () => {
    keycloak.onAuthLogout = () => publish(session.status === 'expired' ? 'expired' : 'anonymous')
    keycloak.onAuthRefreshSuccess = () => {
      if (session.status === 'authenticated') publish('authenticated')
    }
    keycloak.onAuthRefreshError = expireSession
    keycloak.onTokenExpired = () => { void getAccessToken().catch(() => {}) }
    try {
      const authenticated = await keycloak.init({
        onLoad: 'login-required',
        pkceMethod: 'S256',
        flow: 'standard',
        scope: 'openid profile email',
        redirectUri: loginRedirectUri(window.location),
        checkLoginIframe: false,
      })
      publish(authenticated ? 'authenticated' : 'anonymous')
    } catch {
      await keycloak.clearToken()
      publish('error')
    }
  })()
  return initialization
}

export async function getAccessToken() {
  if (session.status !== 'authenticated' || !keycloak.authenticated) throw new Error('Debes iniciar sesión.')
  try {
    await keycloak.updateToken(30)
    if (session.status !== 'authenticated' || !keycloak.token) throw new Error('No se recibió un token.')
    return keycloak.token
  } catch {
    expireSession()
    throw new Error('Tu sesión terminó. Vuelve a iniciar sesión.')
  }
}

export async function logout() {
  // clearToken() activaría otro login con login-required; salir mediante el adaptador.
  await keycloak.logout({ redirectUri: window.location.origin + '/' })
}
