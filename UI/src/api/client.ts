export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

type ClientOptions = {
  baseUrl: string
  getToken: () => Promise<string>
  onUnauthorized: () => void
  fetcher?: typeof fetch
}

export function createApiClient({ baseUrl, getToken, onUnauthorized, fetcher = fetch }: ClientOptions) {
  const base = new URL(baseUrl.replace(/\/+$/, '') + '/')

  return async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
    // Solo rutas dentro de esta API; evita enviar tokens a otras URLs.
    const url = new URL(path, base)
    if (!path || url.origin !== base.origin || !url.pathname.startsWith(base.pathname)) {
      throw new Error('La ruta debe pertenecer a la API configurada.')
    }
    const token = await getToken()
    const headers = new Headers(options.headers)
    headers.set('Authorization', `Bearer ${token}`)
    headers.set('Accept', 'application/json')
    if (options.body && typeof options.body === 'string' && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json')
    }
    let response: Response
    try {
      response = await fetcher(url, { ...options, headers, credentials: 'omit', redirect: 'error' })
    } catch (error) {
      if (options.signal?.aborted) throw error
      throw new ApiError(0, 'No se pudo conectar con el servidor. Comprueba que el backend esté disponible.')
    }
    if (response.status === 401) {
      // No se repiten escrituras automáticamente.
      onUnauthorized()
      throw new ApiError(401, 'El servidor rechazó la sesión. Vuelve a iniciar sesión.')
    }
    if (response.status === 403) {
      throw new ApiError(403, 'No tienes permiso para esta operación. Tu usuario y su sucursal también deben estar activos en el sistema.')
    }
    if (!response.ok) {
      throw new ApiError(response.status, `No se pudo completar la solicitud (${response.status}).`)
    }
    if (response.status === 204) return undefined as T
    return response.json() as Promise<T>
  }
}
