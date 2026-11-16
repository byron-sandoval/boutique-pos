import { api } from '@/api'
import { listarPaginas } from '@/api/paginas'
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
export type ProductoReferencia = Pick<Producto, 'id' | 'nombre' | 'codigo' | 'activo'>
export type VarianteProducto = {
  id: number
  productoId: number
  sku: string
  talla: string | null
  color: string | null
  codigoBarras: string | null
  activo: boolean
}
type VarianteRequest = Omit<VarianteProducto, 'id'>
export type FilaVarianteLote = Omit<VarianteRequest, 'productoId' | 'activo'>
export type RecursoReferencia = 'categorias' | 'unidades-medida'

function listarReferencias<T extends Referencia>(recurso: RecursoReferencia | 'productos', signal: AbortSignal) {
  return listarPaginas(pagina => api<Page<T>>(`${recurso}?page=${pagina}&size=100&sort=nombre,asc&sort=id,asc`, { signal }), signal)
}

export async function cargarReferencias(signal: AbortSignal): Promise<ReferenciasProducto> {
  const [categorias, unidades] = await Promise.all([
    listarReferencias<Referencia>('categorias', signal),
    listarUnidades(signal),
  ])
  return { categorias, unidades }
}

export function listarUnidades(signal: AbortSignal) {
  return listarReferencias<Unidad>('unidades-medida', signal)
}

export function guardarUnidad(datos: Omit<Unidad, 'id'>, id?: number) {
  return api<Unidad>(id === undefined ? 'unidades-medida' : `unidades-medida/${id}`, {
    method: id === undefined ? 'POST' : 'PUT',
    body: JSON.stringify(datos),
    readConflictDetail: true,
  })
}

export function cambiarEstadoUnidad(unidad: Unidad) {
  return api<Unidad>(`unidades-medida/${unidad.id}/estado`, {
    method: 'PATCH',
    body: JSON.stringify({ activo: !unidad.activo }),
    readConflictDetail: true,
  })
}

export type FiltroProductos = { categoriaId: string; busqueda: string }

export function listarProductos({ categoriaId, busqueda }: FiltroProductos, signal: AbortSignal) {
  return listarPaginas(pagina => {
    const params = new URLSearchParams({ page: String(pagina), size: '100', sort: 'activo,desc' })
    params.append('sort', 'id,desc')
    if (categoriaId) params.set('categoriaId', categoriaId)
    if (busqueda) params.set('busqueda', busqueda)
    return api<Page<Producto>>(`productos?${params}`, { signal })
  }, signal)
}
export function listarProductosReferencia(signal: AbortSignal) {
  return listarReferencias<ProductoReferencia>('productos', signal)
}

export function listarVariantes(productoId: string, signal: AbortSignal) {
  return listarPaginas(pagina => {
    const params = new URLSearchParams({ page: String(pagina), size: '100', sort: 'id,desc' })
    if (productoId) params.set('productoId', productoId)
    return api<Page<VarianteProducto>>(`variantes-producto?${params}`, { signal })
  }, signal)
}

export function guardarVariante(datos: VarianteRequest, id?: number) {
  return api<VarianteProducto>(id === undefined ? 'variantes-producto' : `variantes-producto/${id}`, {
    method: id === undefined ? 'POST' : 'PUT',
    body: JSON.stringify(datos),
  })
}

export function guardarLoteVariantes(productoId: number, variantes: FilaVarianteLote[]) {
  return api<VarianteProducto[]>('variantes-producto/lote', {
    method: 'POST',
    body: JSON.stringify({ productoId, variantes }),
    readConflictDetail: true,
  })
}

export function cambiarEstadoVariante(variante: VarianteProducto) {
  return api<VarianteProducto>(`variantes-producto/${variante.id}/estado`, {
    method: 'PATCH',
    body: JSON.stringify({ activo: !variante.activo }),
  })
}

export function guardarProducto(datos: ProductoRequest, imagen?: File, id?: number, quitarImagen = false) {
  if (id !== undefined && quitarImagen && !imagen) {
    return api<Producto>(`productos/${id}?quitarImagen=true`, {
      method: 'PUT',
      body: JSON.stringify({ ...datos, imagenUrl: null }),
      readConflictDetail: true,
    })
  }
  const body = new FormData()
  body.set('producto', new Blob([JSON.stringify(datos)], { type: 'application/json' }))
  if (imagen) body.set('imagen', imagen)
  return api<Producto>(id === undefined ? 'productos' : `productos/${id}`, {
    method: id === undefined ? 'POST' : 'PUT',
    body,
    readConflictDetail: true,
  })
}

export function cambiarEstadoProducto(producto: Producto) {
  return api<Producto>(`productos/${producto.id}/estado`, {
    method: 'PATCH',
    body: JSON.stringify({ activo: !producto.activo }),
    readConflictDetail: true,
  })
}

export function crearReferencia(recurso: RecursoReferencia, nombre: string, simbolo: string | null) {
  if (recurso === 'unidades-medida') return guardarUnidad({ nombre, simbolo, activo: true })
  return api<Referencia>(recurso, { method: 'POST', body: JSON.stringify({ nombre, activo: true }) })
}
