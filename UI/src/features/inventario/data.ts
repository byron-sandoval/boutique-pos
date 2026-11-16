import { api } from '@/api'
import type { Page } from '@/features/empresa/model'

export type Existencia = {
  id: number
  varianteId: number
  sucursalId: number
  producto: string
  sku: string
  talla: string | null
  color: string | null
  sucursal: string
  stock: number
  stockMinimo: number
  stockMaximo: number | null
}

export function listarExistencias(pagina: number, signal: AbortSignal, soloAlertas = false) {
  const ruta = soloAlertas ? 'inventarios/alertas' : 'inventarios'
  const orden = soloAlertas ? '' : '&sort=id,desc'
  return api<Page<Existencia>>(`${ruta}?page=${pagina}&size=20${orden}`, { signal })
}

export function guardarLimites(id: number, limites: Pick<Existencia, 'stockMinimo' | 'stockMaximo'>) {
  return api<Existencia>(`inventarios/${id}/limites`, {
    method: 'PUT',
    body: JSON.stringify(limites),
  })
}
