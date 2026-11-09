import type { Empresa, EmpresaRequest, Page, Sucursal, SucursalRequest } from './model.ts'

type Request = <T>(path: string, options?: RequestInit) => Promise<T>

export function createEmpresaService(request: Request) {
  return {
    nombre(signal?: AbortSignal) {
      return request<{ nombre: string | null }>('empresas/nombre', { signal })
    },
    async empresa(signal?: AbortSignal): Promise<Empresa | null> {
      const result = await request<Page<Empresa>>('empresas?page=0&size=2&sort=id,asc', { signal })
      if (result.page.totalElements > 1 || result.content.length > 1) {
        throw new Error('Hay más de una empresa registrada. Es necesario revisar los registros existentes antes de continuar.')
      }
      return result.content[0] ?? null
    },
    guardarEmpresa(data: EmpresaRequest, id?: number, adjuntos?: { logo?: File }) {
      let body: string | FormData = JSON.stringify(data)
      if (adjuntos) {
        const form = new FormData()
        form.set('empresa', new Blob([body], { type: 'application/json' }))
        if (adjuntos.logo) form.set('logo', adjuntos.logo)
        body = form
      }
      return request<Empresa>(id === undefined ? 'empresas' : `empresas/${id}`, {
        method: id === undefined ? 'POST' : 'PUT', body,
      })
    },
    async sucursales(empresaId: number, signal?: AbortSignal): Promise<Sucursal[]> {
      const sucursales: Sucursal[] = []
      let totalPages = 1
      for (let page = 0; page < totalPages; page++) {
        signal?.throwIfAborted()
        const params = new URLSearchParams({ page: String(page), size: '100', sort: 'id,asc', empresaId: String(empresaId) })
        const result = await request<Page<Sucursal>>(`sucursales?${params}`, { signal })
        sucursales.push(...result.content)
        totalPages = result.page.totalPages
      }
      return sucursales
    },
    guardarSucursal(data: SucursalRequest, id?: number) {
      return request<Sucursal>(id === undefined ? 'sucursales' : `sucursales/${id}`, {
        method: id === undefined ? 'POST' : 'PUT', body: JSON.stringify(data),
      })
    },
    estadoSucursal(id: number, activa: boolean) {
      return request<Sucursal>(`sucursales/${id}/estado`, { method: 'PATCH', body: JSON.stringify({ activa }) })
    },
  }
}
