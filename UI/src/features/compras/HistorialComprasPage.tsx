import { useCallback, useState } from 'react'
import { CalendarDays, CircleCheck, CircleX, Coins, Eye, Package, PackageOpen, ShoppingCart, UserRound, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { RecordDialog } from '@/features/empresa/RecordDialog'
import { useRemote } from '@/features/empresa/hooks'
import { PaginacionCatalogo } from '@/features/productos/PaginacionCatalogo'
import { listarCompras, listarProveedoresHistorial, listarSucursalesHistorial, monedaCompra, obtenerCompra } from './data'
import '@/features/empresa/empresa.css'
import './compras.css'

const fechaCompra = new Intl.DateTimeFormat('es-NI', { dateStyle: 'medium', timeStyle: 'short' })
type Seleccion = { id: number; documento: string }

export function HistorialComprasPage() {
  const [filtro, setFiltro] = useState({ pagina: 0, sucursalId: '' })
  const [seleccion, setSeleccion] = useState<Seleccion | null>(null)
  const cargar = useCallback((signal: AbortSignal) => listarCompras(filtro.pagina, filtro.sucursalId, signal), [filtro])
  const { result, reload } = useRemote(cargar)
  const sucursales = useRemote(listarSucursalesHistorial)
  const proveedores = useRemote(listarProveedoresHistorial)
  const sucursalesLista = sucursales.result.status === 'ready' ? sucursales.result.data : []
  const proveedoresLista = proveedores.result.status === 'ready' ? proveedores.result.data : []
  const sucursalPorId = new Map(sucursalesLista.map(item => [item.id, item.nombre]))
  const proveedorPorId = new Map(proveedoresLista.map(item => [item.id, item.nombre]))
  const pagina = result.status === 'ready' ? result.data : null

  return <div className="business-page">
    <div className="page-heading business-page-heading">
      <div><h1>Historial de compras</h1><p>Consulta los documentos y la mercadería recibida.</p></div>
    </div>
    <Card className="business-table-card">
      <div className="business-table-toolbar">
        <div className="business-section-heading">
          <span className="business-symbol"><PackageOpen size={20} aria-hidden="true" /></span><h2>Compras registradas</h2>
        </div>
        <select aria-label="Filtrar por sucursal" className="h-8 max-w-full rounded-lg border border-border bg-card px-2.5 text-sm"
          value={filtro.sucursalId} disabled={!sucursalesLista.length}
          onChange={event => setFiltro({ pagina: 0, sucursalId: event.currentTarget.value })}>
          <option value="">Todas las sucursales</option>
          {sucursalesLista.map(item => <option key={item.id} value={item.id}>{item.nombre}</option>)}
        </select>
      </div>
      {sucursales.result.status === 'error' && <ErrorCarga mensaje="No se pudieron cargar las sucursales." onRetry={sucursales.reload} />}
      {proveedores.result.status === 'error' && <ErrorCarga mensaje="No se pudieron cargar los nombres de los proveedores." onRetry={proveedores.reload} />}
      {result.status === 'loading' && <p role="status">Cargando compras…</p>}
      {result.status === 'error' && <ErrorCarga mensaje={result.message} onRetry={reload} />}
      {pagina && <>
        {pagina.content.length ? <div className="business-table-scroll" role="region" aria-label="Compras registradas" tabIndex={0}>
          <table className="business-table">
            <thead><tr><th scope="col">Documento</th><th scope="col">Fecha</th><th scope="col">Sucursal</th>
              <th scope="col">Proveedor</th><th scope="col">Total</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
            <tbody>{pagina.content.map(compra => <tr key={compra.id}>
              <th scope="row">{compra.noDocumento}</th>
              <td className="whitespace-nowrap">{fechaCompra.format(new Date(compra.fecha))}</td>
              <td>{sucursalPorId.get(compra.sucursalId) ?? `Sucursal #${compra.sucursalId}`}</td>
              <td>{compra.proveedorId === null ? 'Sin proveedor' : proveedorPorId.get(compra.proveedorId) ?? `Proveedor #${compra.proveedorId}`}</td>
              <td className="tabular-nums whitespace-nowrap">{monedaCompra.format(compra.total)}</td>
              <td><EstadoCompra activa={compra.activo} /></td>
              <td><div className="business-table-actions">
                <Button variant="outline" size="icon-sm" data-table-action="view"
                  aria-label={`Ver compra ${compra.noDocumento}`} title="Ver detalle"
                  onClick={() => setSeleccion({ id: compra.id, documento: compra.noDocumento })}><Eye size={15} aria-hidden="true" /></Button>
              </div></td>
            </tr>)}</tbody>
          </table>
        </div> : <p>No hay compras registradas para este filtro.</p>}
        <PaginacionCatalogo total={pagina.page.totalElements} pagina={filtro.pagina} totalPaginas={pagina.page.totalPages}
          registros="compras" onChange={numero => setFiltro(actual => ({ ...actual, pagina: numero }))} />
      </>}
    </Card>
    {seleccion && <DetalleCompra compra={seleccion} onClose={() => setSeleccion(null)} />}
  </div>
}

function DetalleCompra({ compra, onClose }: { compra: Seleccion; onClose: () => void }) {
  const cargar = useCallback((signal: AbortSignal) => obtenerCompra(compra.id, signal), [compra.id])
  const { result, reload } = useRemote(cargar)

  return <RecordDialog title="Detalle de compra" description={compra.documento} busy={false} onClose={onClose}
    className="compra-detalle-dialog" headingIcon={<span className="compra-detalle-icono"><ShoppingCart size={23} aria-hidden="true" /></span>}>
    <div className="business-state-body compra-detalle-cuerpo">
      {result.status === 'loading' && <p role="status">Cargando detalle…</p>}
      {result.status === 'error' && <ErrorCarga mensaje={result.message} onRetry={reload} />}
      {result.status === 'ready' && <>
        <div className="compra-detalle-resumen">
          <div className="compra-detalle-dato">
            <span className="compra-detalle-icono"><CalendarDays size={20} aria-hidden="true" /></span>
            <div><span>Fecha de registro</span><strong>{fechaCompra.format(new Date(result.data.fecha))}</strong></div>
          </div>
          <div className="compra-detalle-dato">
            <span className="compra-detalle-icono"><UserRound size={20} aria-hidden="true" /></span>
            <div><span>Registrada por</span><strong>{result.data.registradoPor?.trim() || 'Username no disponible'}</strong></div>
          </div>
          <EstadoCompra activa={result.data.activo} conIcono />
        </div>
        <h3 className="compra-detalle-productos"><Package size={21} aria-hidden="true" />Productos ({result.data.detalles.length})</h3>
        <div className="business-table-scroll" role="region" aria-label="Mercadería de la compra" tabIndex={0}>
          <table className="business-table">
            <thead><tr><th scope="col">#</th><th scope="col">Producto y variante</th>
              <th scope="col">Descripción</th><th scope="col">Cantidad</th>
              <th scope="col">Costo unitario</th><th scope="col">Importe</th></tr></thead>
            <tbody>{result.data.detalles.map((linea, indice) => <tr key={linea.id}>
              <td>{indice + 1}</td>
              <th scope="row">
                {linea.producto || `Variante #${linea.varianteId}`}
                {linea.sku && <span className="compra-detalle-variante">{[linea.sku, linea.talla, linea.color].filter(Boolean).join(' · ')}</span>}
              </th>
              <td className="compra-detalle-descripcion">
                {indice === 0 && (result.data.observaciones?.trim() || 'Sin descripción')}
              </td>
              <td>{linea.cantidad}</td>
              <td className="tabular-nums whitespace-nowrap">{monedaCompra.format(linea.costoUnitario)}</td>
              <td className="tabular-nums whitespace-nowrap"><strong className="compra-detalle-importe">{monedaCompra.format(linea.monto)}</strong></td>
            </tr>)}</tbody>
          </table>
        </div>
        <div className="compra-detalle-total">
          <span><Coins size={20} aria-hidden="true" />Total</span>
          <strong>{monedaCompra.format(result.data.total)}</strong>
        </div>
      </>}
      <div className="business-form-actions"><Button variant="outline" onClick={onClose}><X size={16} aria-hidden="true" />Cerrar</Button></div>
    </div>
  </RecordDialog>
}

function EstadoCompra({ activa, conIcono = false }: { activa: boolean; conIcono?: boolean }) {
  const Icono = activa ? CircleCheck : CircleX
  return <span className={`business-status ${activa ? '' : 'is-inactive'}`}>
    {conIcono && <Icono size={16} aria-hidden="true" />}
    {activa ? 'Registrada' : 'Anulada'}
  </span>
}

function ErrorCarga({ mensaje, onRetry }: { mensaje: string; onRetry: () => void }) {
  return <div>
    <p className="notice error" role="alert">{mensaje}</p>
    <Button variant="outline" onClick={onRetry}>Reintentar</Button>
  </div>
}
