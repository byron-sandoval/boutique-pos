export function panelPath(pathname: string) {
  return pathname.replace(/\/+$/, '') || '/inicio'
}

export function loginRedirectUri(location: Pick<Location, 'origin' | 'pathname'>) {
  // El código de autorización y el estado del callback no se reutilizan en la redirección.
  return location.origin + location.pathname
}

export function legacyPanelUrl(location: Pick<Location, 'origin' | 'hash'>) {
  if (!location.hash.startsWith('#/')) return null
  const url = new URL(location.hash.slice(1), location.origin)
  if (url.origin !== location.origin || url.pathname.startsWith('//')) return null
  return url.pathname + url.search
}
