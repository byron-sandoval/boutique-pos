import { useCallback, useState } from 'react'
import { Layers3, Pencil, Plus, Power } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { RecordDialog } from '@/features/empresa/RecordDialog'
import { useRemote, useSave } from '@/features/empresa/hooks'
import { useFlashMessage } from '@/lib/use-flash-message'
import { tonoVariante } from './colores-variantes'
import { cambiarEstadoVariante, guardarVariante, listarProductosReferencia, listarVariantes } from './data'
import type { ProductoReferencia, VarianteProducto } from './data'
import { VarianteLoteDialog } from './VarianteLoteDialog'
import { PaginacionCatalogo } from './PaginacionCatalogo'
import '@/features/empresa/empresa.css'
import './variantes.css'

type Dialogo = { tipo: 'crear' | 'lote' } | { tipo: 'editar' | 'estado'; variante: VarianteProducto }
const variantesPorPagina = 20

export function VariantesPage({ puedeEditar }: { puedeEditar: boolean }) {
  const [productoFiltro, setProductoFiltro] = useState('')
  const [indicePagina, setIndicePagina] = useState(0)
  const [dialogo, setDialogo] = useState<Dialogo | null>(null)
  const [success, setSuccess] = useFlashMessage()
  const cargar = useCallback((signal: AbortSignal) => listarVariantes(productoFiltro, signal), [productoFiltro])
  const { result, reload } = useRemote(cargar)
  const { result: productosResult, reload: reloadProductos } = useRemote(listarProductosReferencia)
  const productos = productosResult.status === 'ready' ? productosResult.data : []
  const productosActivos = productos.filter(producto => producto.activo)
  const porId = new Map(productos.map(producto => [producto.id, producto]))
  const variantes = result.status === 'ready'
    ? [...result.data].sort((a, b) => Number(b.activo) - Number(a.activo)) : null
  const totalVariantes = variantes?.length ?? 0
  const totalPaginas = Math.ceil(totalVariantes / variantesPorPagina)
  const pagina = Math.max(0, Math.min(indicePagina, totalPaginas - 1))
  const inicioPagina = pagina * variantesPorPagina

  function abrir(next: Dialogo) {
    setSuccess('')
    setDialogo(next)
  }

  function guardada(variante: VarianteProducto) {
    if (dialogo?.tipo === 'crear') {
      if (productoFiltro) setProductoFiltro(String(variante.productoId))
      setIndicePagina(0)
      setSuccess('Variante registrada.')
    } else {
      setSuccess(dialogo?.tipo === 'estado'
        ? (variante.activo ? 'Variante activada.' : 'Variante desactivada.')
        : 'Variante actualizada.')
    }
    reload()
    setDialogo(null)
  }

  return <div className="business-page">
    <div className="page-heading business-page-heading">
      <div><h1>Variantes</h1><p>Organiza las tallas, colores y códigos de tus productos.</p></div>
    </div>
    {success && <p className="business-success" role="status">{success}</p>}
    <Card className="business-table-card">
      <div className="business-table-toolbar">
        <div className="business-section-heading">
          <span className="business-symbol"><Layers3 size={20} aria-hidden="true" /></span>
          <h2>Variantes de productos</h2>
        </div>
        <div className="business-toolbar">
          <select aria-label="Filtrar por producto" className="h-8 max-w-full rounded-lg border border-border bg-card px-2.5 text-sm"
            value={productoFiltro} disabled={!productos.length}
            onChange={event => {
              setIndicePagina(0)
              setProductoFiltro(event.currentTarget.value)
            }}>
            <option value="">Todos los productos</option>
            {productos.map(producto => <option key={producto.id} value={producto.id}>{producto.codigo} · {producto.nombre}</option>)}
          </select>
          {puedeEditar && <Button disabled={!productosActivos.length} onClick={() => abrir({ tipo: 'crear' })}>
            <Plus size={16} aria-hidden="true" />Nueva variante
          </Button>}
          {puedeEditar && <Button variant="outline" disabled={!productosActivos.length} onClick={() => abrir({ tipo: 'lote' })}>
            <Layers3 size={16} aria-hidden="true" />Crear variantes en lote
          </Button>}
        </div>
      </div>
      {productosResult.status === 'error' && <div>
        <p className="notice error" role="alert">No se pudo cargar el catálogo de productos. {productosResult.message}</p>
        <Button variant="outline" onClick={reloadProductos}>Reintentar productos</Button>
      </div>}
      {puedeEditar && productosResult.status === 'ready' && !productosActivos.length &&
        <p className="notice">Necesitas un producto activo para registrar una variante.</p>}
      {result.status === 'loading' && <p role="status">Cargando variantes…</p>}
      {result.status === 'error' && <div>
        <p className="notice error" role="alert">{result.message}</p>
        <Button variant="outline" onClick={reload}>Reintentar variantes</Button>
      </div>}
      {variantes && (totalVariantes === 0
        ? <p>{productoFiltro ? 'Este producto todavía no tiene variantes.' : 'Todavía no hay variantes registradas.'}</p>
        : <div className="business-table-scroll" role="region" aria-label="Variantes de productos" tabIndex={0}>
          <table className="business-table">
            <thead><tr>
              <th scope="col">SKU</th><th scope="col">Producto</th><th scope="col">Talla</th>
              <th scope="col">Color</th><th scope="col">Código de barras</th><th scope="col">Estado</th>
              {puedeEditar && <th scope="col">Acciones</th>}
            </tr></thead>
            <tbody>{variantes.slice(inicioPagina, inicioPagina + variantesPorPagina).map(variante => <tr key={variante.id}>
              <th scope="row">{variante.sku}</th>
              <td>{porId.get(variante.productoId)?.nombre ?? `Producto #${variante.productoId}`}</td>
              <td>{variante.talla || '—'}</td><td><ColorVariante color={variante.color} /></td>
              <td className="tabular-nums">{variante.codigoBarras || '—'}</td>
              <td><span className={`business-status ${variante.activo ? '' : 'is-inactive'}`}>{variante.activo ? 'Activa' : 'Inactiva'}</span></td>
              {puedeEditar && <td><div className="business-table-actions">
                <Button variant="outline" size="icon-sm" data-table-action="edit" title="Editar variante"
                  aria-label={`Editar variante ${variante.sku}`} onClick={() => abrir({ tipo: 'editar', variante })}>
                  <Pencil size={14} aria-hidden="true" />
                </Button>
                <Button variant="outline" size="icon-sm" data-table-action={variante.activo ? 'deactivate' : 'activate'}
                  title={variante.activo ? 'Desactivar variante' : 'Activar variante'}
                  aria-label={`${variante.activo ? 'Desactivar' : 'Activar'} variante ${variante.sku}`}
                  onClick={() => abrir({ tipo: 'estado', variante })}>
                  <Power size={14} aria-hidden="true" />
                </Button>
              </div></td>}
            </tr>)}</tbody>
          </table>
        </div>)}
      <PaginacionCatalogo total={totalVariantes} pagina={pagina} totalPaginas={totalPaginas} registros="variantes" onChange={setIndicePagina} />
    </Card>
    {puedeEditar && dialogo?.tipo === 'crear' && <VarianteForm productos={productosActivos}
      productoInicial={productoFiltro} onClose={() => setDialogo(null)} onSaved={guardada} />}
    {puedeEditar && dialogo?.tipo === 'lote' && <VarianteLoteDialog productos={productosActivos}
      productoInicial={productoFiltro} onClose={() => setDialogo(null)} onSaved={(productoId, cantidad) => {
        if (productoFiltro) setProductoFiltro(String(productoId))
        setIndicePagina(0)
        reload()
        setSuccess(cantidad === 1 ? 'Variante registrada.' : `${cantidad} variantes registradas.`)
        setDialogo(null)
      }} />}
    {puedeEditar && dialogo?.tipo === 'editar' && <VarianteForm variante={dialogo.variante} productos={productos}
      onClose={() => setDialogo(null)} onSaved={guardada} />}
    {puedeEditar && dialogo?.tipo === 'estado' && <VarianteEstadoDialog variante={dialogo.variante}
      onClose={() => setDialogo(null)} onSaved={guardada} />}
  </div>
}

type DialogActions = { onClose: () => void; onSaved: (variante: VarianteProducto) => void }

function ColorVariante({ color }: { color: string | null }) {
  const tono = tonoVariante(color)
  return <span className="inline-flex items-center gap-2">
    {tono && <span className="variante-color-muestra" style={{ backgroundColor: tono }} aria-hidden="true" />}
    {color || '—'}
  </span>
}

function VarianteForm({ variante, productos, productoInicial, onClose, onSaved }: DialogActions & {
  variante?: VarianteProducto
  productos: ProductoReferencia[]
  productoInicial?: string
}) {
  const { busy, error, save } = useSave()
  const productoActual = productos.find(producto => producto.id === variante?.productoId)
  return <RecordDialog title={variante ? 'Editar variante' : 'Nueva variante'}
    description="Registra el SKU y los atributos de la variante." busy={busy} onClose={onClose}>
    <form className="business-form" onSubmit={event => {
      event.preventDefault()
      const form = new FormData(event.currentTarget)
      const sku = String(form.get('sku') ?? '').trim()
      const productoId = variante?.productoId ?? Number(form.get('productoId'))
      void save(() => guardarVariante({
        productoId, sku,
        talla: String(form.get('talla') ?? '').trim() || null,
        color: String(form.get('color') ?? '').trim() || null,
        codigoBarras: String(form.get('codigoBarras') ?? '').trim() || null,
        activo: variante?.activo ?? true,
      }, variante?.id), onSaved, 'Ya existe ese SKU, código de barras o combinación de producto, talla y color.')
    }}>
      <fieldset disabled={busy}>
        <div className="business-fields">
          <label className="business-field-wide">Producto {variante ? <input readOnly
            value={productoActual ? `${productoActual.codigo} · ${productoActual.nombre}` : `Producto #${variante.productoId}`} />
            : <select name="productoId" required defaultValue={productos.some(producto => String(producto.id) === productoInicial) ? productoInicial : ''}>
              <option value="">Selecciona un producto</option>
              {productos.map(producto => <option key={producto.id} value={producto.id}>{producto.codigo} · {producto.nombre}</option>)}
            </select>}</label>
          <label>SKU<input name="sku" required pattern={String.raw`.*\S.*`} maxLength={50} defaultValue={variante?.sku} /></label>
          <label>Código de barras<input name="codigoBarras" maxLength={50} defaultValue={variante?.codigoBarras ?? ''} /></label>
          <label>Talla<input name="talla" maxLength={20} defaultValue={variante?.talla ?? ''} /></label>
          <label>Color<input name="color" maxLength={50} defaultValue={variante?.color ?? ''} /></label>
        </div>
      </fieldset>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button type="button" variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button type="submit" disabled={busy}>{busy ? 'Guardando…' : 'Guardar variante'}</Button>
      </div>
    </form>
  </RecordDialog>
}

function VarianteEstadoDialog({ variante, onClose, onSaved }: DialogActions & { variante: VarianteProducto }) {
  const { busy, error, save } = useSave()
  const titulo = variante.activo ? 'Desactivar variante' : 'Activar variante'
  return <RecordDialog title={titulo} description={variante.sku} busy={busy} onClose={onClose}>
    <div className="business-state-body">
      <p>{variante.activo ? 'La variante quedará inactiva y seguirá apareciendo en el catálogo.' : 'La variante volverá a estar activa.'}</p>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button variant={variante.activo ? 'destructive' : 'default'} disabled={busy}
          onClick={() => void save(() => cambiarEstadoVariante(variante), onSaved)}>{busy ? 'Guardando…' : titulo}</Button>
      </div>
    </div>
  </RecordDialog>
}
