import { api } from '@/api'
import { listarPaginas } from '@/api/paginas'
import type { Page } from '@/features/empresa/model'

type SucursalCompra = { id: number; nombre: string; activa: boolean }
type ProveedorCompra = { id: number; nombre: string; activo: boolean }
type LineaCompra = { varianteId: number; cantidad: number; costoUnitario: number }
type CompraRequest = {
  sucursalId: number
  proveedorId: number
  noDocumento: string
  observaciones: string | null
  detalles: LineaCompra[]
}
type CompraResumen = Omit<CompraRequest, 'detalles' | 'proveedorId'> & {
  id: number; proveedorId: number | null; fecha: string; total: number; activo: boolean
}
type DetalleCompra = LineaCompra & {
  id: number; monto: number; producto: string; sku: string; talla: string | null; color: string | null
}
type Compra = CompraResumen & { detalles: DetalleCompra[]; registradoPor: string | null }

export const monedaCompra = new Intl.NumberFormat('es-NI', { style: 'currency', currency: 'NIO', currencyDisplay: 'narrowSymbol' })

export function cargarSucursalCompra(signal: AbortSignal) {
  return api<SucursalCompra>('ingresos/sucursal-actual', { signal })
}

export function obtenerUltimoCosto(varianteId: number, signal: AbortSignal) {
  return api<{ costoUnitario: number | null }>(`ingresos/ultimo-costo?varianteId=${varianteId}`, { signal })
}

export function listarProveedoresHistorial(signal: AbortSignal) {
  return listarPaginas(pagina =>
    api<Page<ProveedorCompra>>(`proveedores?page=${pagina}&size=100&sort=nombre,asc&sort=id,asc`, { signal }), signal)
}

export async function listarProveedoresCompra(signal: AbortSignal) {
  return (await listarProveedoresHistorial(signal)).filter(proveedor => proveedor.activo)
}

export function listarSucursalesHistorial(signal: AbortSignal) {
  return listarPaginas(pagina =>
    api<Page<SucursalCompra>>(`sucursales?page=${pagina}&size=100&sort=nombre,asc&sort=id,asc`, { signal }), signal)
}

export function listarCompras(pagina: number, sucursalId: string, signal: AbortSignal) {
  const params = new URLSearchParams({ page: String(pagina), size: '20', sort: 'id,desc' })
  if (sucursalId) params.set('sucursalId', sucursalId)
  return api<Page<CompraResumen>>(`ingresos?${params}`, { signal })
}

export function obtenerCompra(id: number, signal: AbortSignal) {
  return api<Compra>(`ingresos/${id}`, { signal })
}

export function registrarCompra(datos: CompraRequest) {
  return api<{ noDocumento: string; total: number }>('ingresos', {
    method: 'POST', body: JSON.stringify(datos), readConflictDetail: true,
  })
}
