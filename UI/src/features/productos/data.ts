import { api } from '@/api'
import type { Page } from '@/features/empresa/model'

export type Referencia = { id: number; nombre: string; activo: boolean }
export type Unidad = Referencia & { simbolo: string | null }
export type ReferenciasProducto = { categorias: Referencia[]; unidades: Unidad[] }

export type Producto = {
  id: number
  categoriaId: number
  unidadMedidaId: number
  codigo: string
  nombre: string
  descripcion: string | null
  marca: string | null
  precio: number
  costo: number
  ultimoCosto: number | null
  imagenUrl: string | null
  activo: boolean
}

type ProductoRequest = Omit<Producto, 'id' | 'imagenUrl'>
export type RecursoReferencia = 'categorias' | 'unidades-medida'

async function listarReferencias<T extends Referencia>(recurso: RecursoReferencia, signal: AbortSignal) {
  const referencias: T[] = []
  let totalPages = 1
  for (let pagina = 0; pagina < totalPages; pagina++) {
    signal.throwIfAborted()
    const respuesta = await api<Page<T>>(`${recurso}?page=${pagina}&size=100&sort=nombre,asc&sort=id,asc`, { signal })
    referencias.push(...respuesta.content)
    totalPages = respuesta.page.totalPages
  }
  return referencias
}

export async function cargarReferencias(signal: AbortSignal): Promise<ReferenciasProducto> {
  const [categorias, unidades] = await Promise.all([
    listarReferencias<Referencia>('categorias', signal),
    listarReferencias<Unidad>('unidades-medida', signal),
  ])
  return { categorias, unidades }
}

export function listarProductos(pagina: number, categoriaId: string, signal: AbortSignal) {
  const params = new URLSearchParams({ page: String(pagina), size: '20', sort: 'id,desc' })
  if (categoriaId) params.set('categoriaId', categoriaId)
  return api<Page<Producto>>(`productos?${params}`, { signal })
}

export function guardarProducto(datos: ProductoRequest, imagen?: File, id?: number) {
  const body = new FormData()
  body.set('producto', new Blob([JSON.stringify(datos)], { type: 'application/json' }))
  if (imagen) body.set('imagen', imagen)
  return api<Producto>(id === undefined ? 'productos' : `productos/${id}`, {
    method: id === undefined ? 'POST' : 'PUT',
    body,
  })
}

export function cambiarEstadoProducto(producto: Producto) {
  return api<Producto>(`productos/${producto.id}/estado`, {
    method: 'PATCH',
    body: JSON.stringify({ activo: !producto.activo }),
  })
}

export function crearReferencia(recurso: RecursoReferencia, nombre: string, simbolo: string | null) {
  const datos = recurso === 'categorias' ? { nombre, activo: true } : { nombre, simbolo, activo: true }
  return api<Referencia>(recurso, { method: 'POST', body: JSON.stringify(datos) })
}
