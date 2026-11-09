import { useCallback, useState } from 'react'
import type { ReactNode } from 'react'
import { Barcode, CheckCircle2, Coins, Eye, Layers3, Package, Pencil, Plus, Power, Ruler, ShoppingBag, Tag, Text } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { RecordDialog } from '@/features/empresa/RecordDialog'
import { useRemote, useSave } from '@/features/empresa/hooks'
import { useFlashMessage } from '@/lib/use-flash-message'
import { cambiarEstadoProducto, cargarReferencias, crearReferencia, listarProductos } from './data'
import type { Producto, RecursoReferencia } from './data'
import { ProductoForm } from './ProductoForm'
import '@/features/empresa/empresa.css'
import './producto-detalle.css'

const cordobas = new Intl.NumberFormat('es-NI', { style: 'currency', currency: 'NIO', currencyDisplay: 'narrowSymbol' })

export function ProductosPage({ puedeEditar }: { puedeEditar: boolean }) {
  const [filtro, setFiltro] = useState({ pagina: 0, categoriaId: '' })
  const cargar = useCallback((signal: AbortSignal) => listarProductos(filtro.pagina, filtro.categoriaId, signal), [filtro])
  const { result, reload } = useRemote(cargar)
  const { result: referenciasResult, reload: reloadReferencias } = useRemote(cargarReferencias)
  const [editor, setEditor] = useState<{ producto: Producto | null } | null>(null)
  const [detalle, setDetalle] = useState<Producto | null>(null)
  const [estado, setEstado] = useState<Producto | null>(null)
  const [nuevaReferencia, setNuevaReferencia] = useState<RecursoReferencia | null>(null)
  const [success, setSuccess] = useFlashMessage()
  const referencias = referenciasResult.status === 'ready' ? referenciasResult.data : null
  const pagina = result.status === 'ready' ? result.data : null
  const hayCategoriasActivas = referencias?.categorias.some(categoria => categoria.activo) ?? false
  const puedeCrear = hayCategoriasActivas && Boolean(referencias?.unidades.length)
  const categorias = new Map(referencias?.categorias.map(categoria => [categoria.id, categoria.nombre]))
  const unidades = new Map(referencias?.unidades.map(unidad => [unidad.id, unidad.simbolo || unidad.nombre]))

  function abrirEditor(producto: Producto | null) {
    setSuccess('')
    setEditor({ producto })
  }

  function productoGuardado(producto: Producto) {
    setSuccess(editor?.producto ? 'Producto actualizado.' : 'Producto registrado.')
    if (!editor?.producto) {
      setFiltro({ pagina: 0, categoriaId: '' })
    } else if (filtro.categoriaId && filtro.categoriaId !== String(producto.categoriaId)) {
      setFiltro({ pagina: 0, categoriaId: '' })
    } else {
      reload()
    }
    setEditor(null)
  }

  return <div className="business-page">
    <div className="page-heading business-page-heading">
      <div><h1>Productos</h1><p>Consulta el catálogo, sus precios y el estado de cada producto.</p></div>
    </div>
    {success && <p className="business-success" role="status">{success}</p>}
    <Card className="business-table-card">
      <div className="business-table-toolbar">
        <div className="business-section-heading">
          <span className="business-symbol"><Package size={20} aria-hidden="true" /></span>
          <div><h2>Catálogo de productos</h2><p>Organiza tus artículos por categoría.</p></div>
        </div>
        <div className="business-toolbar">
          <select aria-label="Filtrar por categoría" className="h-8 rounded-lg border border-border bg-card px-2.5 text-sm"
            value={filtro.categoriaId} disabled={!referencias?.categorias.length}
            onChange={event => setFiltro({ pagina: 0, categoriaId: event.currentTarget.value })}>
            <option value="">Todas las categorías</option>
            {referencias?.categorias.map(categoria => <option key={categoria.id} value={categoria.id}>{categoria.nombre}</option>)}
          </select>
          {puedeEditar && <Button disabled={!puedeCrear} onClick={() => abrirEditor(null)}><Plus size={16} />Nuevo producto</Button>}
        </div>
      </div>
      {referenciasResult.status === 'error' && <div>
        <p className="notice error" role="alert">No se pudieron cargar las categorías y unidades. {referenciasResult.message}</p>
        <Button variant="outline" onClick={reloadReferencias}>Reintentar categorías y unidades</Button>
      </div>}
      {puedeEditar && referencias && !puedeCrear && <div className="notice">
        <p>Para crear productos necesitas una categoría activa y una unidad de medida.</p>
        <div className="business-toolbar mt-2">
          {!hayCategoriasActivas && <Button variant="outline" onClick={() => { setSuccess(''); setNuevaReferencia('categorias') }}>Nueva categoría</Button>}
          {!referencias.unidades.length && <Button variant="outline" onClick={() => { setSuccess(''); setNuevaReferencia('unidades-medida') }}>Nueva unidad de medida</Button>}
        </div>
      </div>}
      {result.status === 'loading' && <p role="status">Cargando productos…</p>}
      {result.status === 'error' && <div>
        <p className="notice error" role="alert">{result.message}</p>
        <Button variant="outline" onClick={reload}>Reintentar productos</Button>
      </div>}
      {pagina && (pagina.content.length === 0
        ? <p>{filtro.categoriaId ? 'No hay productos en esta categoría.' : 'No hay productos en esta página.'}</p>
        : <div className="business-table-scroll" role="region" aria-label="Catálogo de productos" tabIndex={0}>
          <table className="business-table">
            <thead><tr>
              <th scope="col">Código</th><th scope="col">Producto</th><th scope="col">Categoría</th>
              <th scope="col">Unidad</th><th scope="col">Precio</th><th scope="col">Estado</th>
              <th scope="col">Acciones</th>
            </tr></thead>
            <tbody>{pagina.content.map(producto => <tr key={producto.id}>
              <td>{producto.codigo}</td>
              <th scope="row">
                <div className="business-table-name">
                  <ProductoFoto key={producto.imagenUrl} url={producto.imagenUrl} />
                  <div><span>{producto.nombre}</span>{producto.marca && <p className="text-xs font-normal text-muted-foreground">{producto.marca}</p>}</div>
                </div>
              </th>
              <td>{categorias.get(producto.categoriaId) ?? producto.categoriaId}</td>
              <td>{unidades.get(producto.unidadMedidaId) ?? producto.unidadMedidaId}</td>
              <td className="whitespace-nowrap tabular-nums">{cordobas.format(producto.precio)}</td>
              <td><span className={`business-status ${producto.activo ? '' : 'is-inactive'}`}>{producto.activo ? 'Activo' : 'Inactivo'}</span></td>
              <td><div className="business-table-actions">
                <Button variant="outline" size="icon-sm" data-table-action="view"
                  title="Ver detalles" aria-label={`Ver detalles de ${producto.nombre}`} onClick={() => setDetalle(producto)}>
                  <Eye size={14} aria-hidden="true" />
                </Button>
                {puedeEditar && <>
                  <Button variant="outline" size="icon-sm" title="Editar producto"
                    aria-label={`Editar producto ${producto.nombre}`} disabled={!referencias} onClick={() => abrirEditor(producto)}>
                    <Pencil size={14} aria-hidden="true" />
                  </Button>
                  <Button variant="outline" size="icon-sm" data-table-action={producto.activo ? 'deactivate' : 'activate'}
                    title={producto.activo ? 'Desactivar producto' : 'Activar producto'}
                    aria-label={`${producto.activo ? 'Desactivar' : 'Activar'} producto ${producto.nombre}`}
                    onClick={() => { setSuccess(''); setEstado(producto) }}>
                    <Power size={14} aria-hidden="true" />
                  </Button>
                </>}
              </div></td>
            </tr>)}</tbody>
          </table>
        </div>
      )}
      {pagina && (pagina.page.totalPages > 1 || filtro.pagina > 0) && <div className="pagination">
        <span>{pagina.page.totalElements} productos · Página {filtro.pagina + 1} de {Math.max(1, pagina.page.totalPages)}</span>
        <div>
          <Button variant="outline" disabled={filtro.pagina === 0}
            onClick={() => setFiltro(actual => ({ ...actual, pagina: actual.pagina - 1 }))}>Anterior</Button>
          <Button variant="outline" disabled={filtro.pagina + 1 >= pagina.page.totalPages}
            onClick={() => setFiltro(actual => ({ ...actual, pagina: actual.pagina + 1 }))}>Siguiente</Button>
        </div>
      </div>}
    </Card>
    {detalle && <ProductoDetalleDialog producto={detalle}
      categoria={categorias.get(detalle.categoriaId) ?? String(detalle.categoriaId)}
      unidad={referencias?.unidades.find(unidad => unidad.id === detalle.unidadMedidaId)?.nombre ?? String(detalle.unidadMedidaId)}
      mostrarCostos={puedeEditar} onClose={() => setDetalle(null)} />}
    {puedeEditar && editor && referencias && <ProductoForm producto={editor.producto} referencias={referencias}
      onClose={() => setEditor(null)} onSaved={productoGuardado} />}
    {puedeEditar && estado && <ProductoEstadoDialog producto={estado} onClose={() => setEstado(null)} onSaved={() => {
      setSuccess(estado.activo ? 'Producto desactivado.' : 'Producto activado.')
      setEstado(null)
      reload()
    }} />}
    {puedeEditar && nuevaReferencia && <ReferenciaForm recurso={nuevaReferencia} onClose={() => setNuevaReferencia(null)} onSaved={() => {
      setSuccess(nuevaReferencia === 'categorias' ? 'Categoría registrada.' : 'Unidad de medida registrada.')
      setNuevaReferencia(null)
      reloadReferencias()
    }} />}
  </div>
}

function ProductoFoto({ url, descripcion }: { url: string | null; descripcion?: string }) {
  const [error, setError] = useState(false)
  return <span className={descripcion ? 'product-detail-image' : 'grid size-9 shrink-0 place-items-center overflow-hidden rounded-lg bg-accent text-accent-foreground'}>
    {url && !error
      ? <img src={url} alt={descripcion ?? ''} className={`size-full ${descripcion ? 'object-contain' : 'object-cover'}`} loading="lazy" onError={() => setError(true)} />
      : !descripcion && <Package size={17} aria-hidden="true" />}
  </span>
}

function ProductoDetalleDialog({ producto, categoria, unidad, mostrarCostos, onClose }: {
  producto: Producto
  categoria: string
  unidad: string
  mostrarCostos: boolean
  onClose: () => void
}) {
  const estado = <span className={`business-status ${producto.activo ? '' : 'is-inactive'}`}>{producto.activo ? 'Activo' : 'Inactivo'}</span>
  return <RecordDialog title="Detalle del producto" description={producto.nombre} busy={false} onClose={onClose}
    className="product-detail-dialog"
    headingIcon={<span className="product-detail-title-icon"><ShoppingBag size={22} aria-hidden="true" /></span>}>
    <div className="product-detail-body">
      <div className="product-detail-photo">
        <ProductoFoto url={producto.imagenUrl} descripcion={`Foto de ${producto.nombre}`} />
        <div className="product-detail-photo-status">{estado}</div>
      </div>
      <section className="product-detail-info">
        <h3><Tag size={18} aria-hidden="true" />Información del producto</h3>
        <dl className="product-detail-fields">
          <DetalleDato icono={Barcode} nombre="Código">{producto.codigo}</DetalleDato>
          <DetalleDato icono={Layers3} nombre="Categoría">{categoria}</DetalleDato>
          <DetalleDato icono={Ruler} nombre="Unidad de medida">{unidad}</DetalleDato>
          <DetalleDato icono={Tag} nombre="Marca">{producto.marca || 'Sin registrar'}</DetalleDato>
          <DetalleDato icono={Tag} nombre="Precio de venta"><strong className="product-detail-price">{cordobas.format(producto.precio)}</strong></DetalleDato>
          {mostrarCostos && <DetalleDato icono={Coins} nombre="Costo">{cordobas.format(producto.costo)}</DetalleDato>}
          <DetalleDato icono={CheckCircle2} nombre="Estado">{estado}</DetalleDato>
          <DetalleDato icono={Text} nombre="Descripción">{producto.descripcion || 'Sin descripción'}</DetalleDato>
        </dl>
      </section>
    </div>
    <div className="business-form-actions product-detail-footer">
      <Button variant="outline" onClick={onClose}>Cerrar</Button>
    </div>
  </RecordDialog>
}

function DetalleDato({ icono: Icono, nombre, children }: { icono: LucideIcon; nombre: string; children: ReactNode }) {
  return <div>
    <dt><span><Icono size={16} aria-hidden="true" /></span>{nombre}</dt>
    <dd>{children}</dd>
  </div>
}

type DialogActions = { onClose: () => void; onSaved: () => void }

function ProductoEstadoDialog({ producto, onClose, onSaved }: DialogActions & { producto: Producto }) {
  const { busy, error, save } = useSave()
  const titulo = producto.activo ? 'Desactivar producto' : 'Activar producto'
  return <RecordDialog title={titulo} description={producto.nombre} busy={busy} onClose={onClose}>
    <div className="business-state-body">
      <p>{producto.activo ? 'El producto dejará de estar disponible para nuevas ventas.' : 'El producto volverá a estar activo en el catálogo.'}</p>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button variant={producto.activo ? 'destructive' : 'default'} disabled={busy}
          onClick={() => void save(() => cambiarEstadoProducto(producto), onSaved)}>
          {busy ? 'Guardando…' : titulo}
        </Button>
      </div>
    </div>
  </RecordDialog>
}

function ReferenciaForm({ recurso, onClose, onSaved }: DialogActions & { recurso: RecursoReferencia }) {
  const { busy, error, save } = useSave()
  const esCategoria = recurso === 'categorias'
  return <RecordDialog title={esCategoria ? 'Nueva categoría' : 'Nueva unidad de medida'}
    description="Registra el dato que falta para crear productos." busy={busy} onClose={onClose}>
    <form className="business-form" onSubmit={event => {
      event.preventDefault()
      const form = new FormData(event.currentTarget)
      const nombre = String(form.get('nombre') ?? '').trim()
      const simbolo = String(form.get('simbolo') ?? '').trim() || null
      void save(() => {
        if (!nombre) throw new Error('Escribe un nombre válido.')
        return crearReferencia(recurso, nombre, simbolo)
      }, onSaved, 'Ya existe un registro con ese nombre.')
    }}>
      <fieldset disabled={busy}>
        <div className="business-fields">
          <label>Nombre<input name="nombre" required maxLength={esCategoria ? 50 : 30} autoFocus /></label>
          {!esCategoria && <label>Símbolo<input name="simbolo" maxLength={10} /></label>}
        </div>
      </fieldset>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button type="button" variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button type="submit" disabled={busy}>{busy ? 'Guardando…' : 'Guardar'}</Button>
      </div>
    </form>
  </RecordDialog>
}
