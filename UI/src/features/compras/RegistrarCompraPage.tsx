import { useCallback, useId, useState } from 'react'
import { ClipboardList, PackagePlus, Plus, Save, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { useRemote, useSave } from '@/features/empresa/hooks'
import { listarProductosReferencia, listarVariantes } from '@/features/productos/data'
import { useFlashMessage } from '@/lib/use-flash-message'
import { cargarSucursalCompra, listarProveedoresCompra, monedaCompra, obtenerUltimoCosto, registrarCompra } from './data'
import '@/features/empresa/empresa.css'
import './compras.css'

type Fila = { varianteId: number; producto: string; variante: string; cantidad: string; costo: string }
const maximoCentavos = 999_999_999_999
const maximoCantidad = 2_147_483_647
const maximoLineas = 500

function costoEnCentavos(texto: string) {
  const partes = /^(\d{1,10})(?:[.,](\d{1,2}))?$/.exec(texto.trim())
  return partes ? Number(partes[1]) * 100 + Number((partes[2] ?? '').padEnd(2, '0')) : null
}

function montoFila(fila: Fila) {
  const cantidad = Number(fila.cantidad)
  const costo = costoEnCentavos(fila.costo)
  if (!Number.isInteger(cantidad) || cantidad <= 0 || cantidad > maximoCantidad || costo === null) return null
  return cantidad * costo
}

export function RegistrarCompraPage() {
  const sucursal = useRemote(cargarSucursalCompra)
  const proveedores = useRemote(listarProveedoresCompra)
  const productos = useRemote(listarProductosReferencia)
  const { busy, error, save, clearError } = useSave()
  const [proveedorId, setProveedorId] = useState('')
  const [productoId, setProductoId] = useState('')
  const [varianteId, setVarianteId] = useState('')
  const [filas, setFilas] = useState<Fila[]>([])
  const [registrada, setRegistrada] = useFlashMessage()
  const [errorForm, setErrorForm] = useState('')
  const cargarVariantes = useCallback((signal: AbortSignal) =>
    productoId ? listarVariantes(productoId, signal) : Promise.resolve([]), [productoId])
  const variantes = useRemote(cargarVariantes)

  const sucursalActual = sucursal.result.status === 'ready' ? sucursal.result.data : null
  const proveedoresActivos = proveedores.result.status === 'ready' ? proveedores.result.data : []
  const proveedor = proveedoresActivos.find(item => String(item.id) === proveedorId)
  const productosActivos = productos.result.status === 'ready' ? productos.result.data.filter(item => item.activo) : []
  const producto = productosActivos.find(item => String(item.id) === productoId)
  const variantesActivas = producto && variantes.result.status === 'ready'
    ? variantes.result.data.filter(item => item.activo) : []
  const variantesDisponibles = variantesActivas.filter(item => !filas.some(fila => fila.varianteId === item.id))
  const montos = filas.map(montoFila)
  const total = montos.some(monto => monto === null) ? null : montos.reduce<number>((suma, monto) => suma + (monto ?? 0), 0)

  function agregarVariante() {
    const variante = variantesDisponibles.find(item => String(item.id) === varianteId)
    if (!variante || !producto) return
    const nombreVariante = [variante.sku, variante.talla, variante.color].filter(Boolean).join(' · ')
    setFilas(actual => actual.length >= maximoLineas || actual.some(fila => fila.varianteId === variante.id)
      ? actual : [...actual, { varianteId: variante.id, producto: producto.nombre, variante: nombreVariante, cantidad: '1', costo: '' }])
    setVarianteId('')
    setErrorForm('')
    clearError()
  }

  function actualizarFila(id: number, campo: 'cantidad' | 'costo', valor: string) {
    setFilas(actual => actual.map(fila => fila.varianteId === id ? { ...fila, [campo]: valor } : fila))
    setErrorForm('')
    clearError()
  }

  function enviar(formulario: HTMLFormElement) {
    if (!sucursalActual?.activa || !proveedor || !filas.length) return
    const form = new FormData(formulario)
    const noDocumento = String(form.get('noDocumento') ?? '').trim()
    if (!noDocumento) { setErrorForm('Ingresa el número de documento.'); return }
    if (total === null) { setErrorForm('Indica una cantidad entera y un costo de hasta dos decimales para cada variante.'); return }
    if (total > maximoCentavos) { setErrorForm('El total de la compra supera el límite permitido.'); return }
    const detalles = filas.map(fila => ({
      varianteId: fila.varianteId, cantidad: Number(fila.cantidad), costoUnitario: Number(costoEnCentavos(fila.costo)) / 100,
    }))
    setErrorForm('')
    void save(() => registrarCompra({
      sucursalId: sucursalActual.id,
      proveedorId: proveedor.id,
      noDocumento,
      observaciones: String(form.get('observaciones') ?? '').trim() || null,
      detalles,
    }), compra => {
      formulario.reset()
      setRegistrada(`Compra ${compra.noDocumento} registrada por ${monedaCompra.format(compra.total)}. Las existencias se actualizaron.`)
      setProveedorId('')
      setFilas([])
      setProductoId('')
      setVarianteId('')
    })
  }

  return <div className="business-page compra-page">
    <div className="page-heading business-page-heading">
      <div><h1>Registrar compra</h1><p>Registra las unidades que llegaron a tu sucursal.</p></div>
    </div>
    {registrada && <p className="business-success" role="status">{registrada}</p>}
    <form className="compra-form" onSubmit={event => {
      event.preventDefault()
      enviar(event.currentTarget)
    }}>
      <Card className="business-table-card">
        <div className="business-section-heading">
          <span className="business-symbol"><ClipboardList size={20} aria-hidden="true" /></span><h2>Datos de la compra</h2>
        </div>
        {sucursal.result.status === 'error' && <ErrorCarga mensaje={sucursal.result.message} etiqueta="Reintentar sucursal" onRetry={sucursal.reload} />}
        {sucursalActual && !sucursalActual.activa && <p className="notice error" role="alert">Tu sucursal debe estar activa para registrar compras.</p>}
        {proveedores.result.status === 'error' && <ErrorCarga mensaje="No se pudieron cargar los proveedores."
          etiqueta="Reintentar proveedores" onRetry={proveedores.reload} />}
        {proveedores.result.status === 'ready' && !proveedoresActivos.length &&
          <p className="notice" role="status">Necesitas un proveedor activo para registrar la compra.</p>}
        <fieldset disabled={busy} className="business-fields compra-datos">
          <label>Sucursal<input value={sucursalActual?.nombre ?? (sucursal.result.status === 'loading' ? 'Cargando…' : '')} readOnly /></label>
          <label>Proveedor<select name="proveedorId" required value={proveedorId} disabled={!proveedoresActivos.length}
            onChange={event => {
              setProveedorId(event.currentTarget.value)
              clearError()
            }}>
            <option value="">{proveedores.result.status === 'loading' ? 'Cargando proveedores…' : 'Selecciona un proveedor'}</option>
            {proveedoresActivos.map(item => <option key={item.id} value={item.id}>{item.nombre}</option>)}
          </select></label>
          <label>Número de documento<input name="noDocumento" maxLength={50} required /></label>
          <label>Observaciones<input name="observaciones" maxLength={255} /></label>
        </fieldset>
      </Card>
      <Card className="business-table-card">
        <div className="business-section-heading">
          <span className="business-symbol"><PackagePlus size={20} aria-hidden="true" /></span><h2>Mercadería recibida</h2>
        </div>
        {productos.result.status === 'loading' && <p role="status">Cargando productos…</p>}
        {productos.result.status === 'error' && <ErrorCarga mensaje={productos.result.message} etiqueta="Reintentar productos" onRetry={productos.reload} />}
        {productos.result.status === 'ready' && !productosActivos.length && <p className="notice">Registra un producto y su variante desde Productos para poder comprarlo.</p>}
        {producto && variantes.result.status === 'error' && <ErrorCarga mensaje={variantes.result.message} etiqueta="Reintentar variantes" onRetry={variantes.reload} />}
        <fieldset disabled={busy || !productosActivos.length} className="business-fields compra-selector">
          <label>Producto<select value={productoId} onChange={event => { setProductoId(event.currentTarget.value); setVarianteId('') }}>
            <option value="">Selecciona un producto</option>
            {productosActivos.map(item => <option key={item.id} value={item.id}>{item.nombre}</option>)}
          </select></label>
          <label>Variante<select value={varianteId} onChange={event => setVarianteId(event.currentTarget.value)} disabled={!variantesDisponibles.length}>
            <option value="">{producto && variantes.result.status === 'loading' ? 'Cargando variantes…' : 'Selecciona una variante'}</option>
            {variantesDisponibles.map(item => <option key={item.id} value={item.id}>
              {[item.sku, item.talla, item.color].filter(Boolean).join(' · ')}
            </option>)}
          </select></label>
          <Button type="button" className="bg-[#016F3E] text-white hover:bg-[#005C33]"
            disabled={!varianteId || filas.length >= maximoLineas} onClick={agregarVariante}>
            <Plus size={16} aria-hidden="true" />Agregar
          </Button>
        </fieldset>
        {producto && variantes.result.status === 'ready' && !variantesActivas.length &&
          <p className="notice">Este producto necesita una variante activa. Puedes registrarla en Productos → Variantes.</p>}
        {filas.length === maximoLineas && <p role="status">La compra admite hasta {maximoLineas} líneas.</p>}
        {filas.length > 0 && <div className="business-table-scroll compra-mercaderia-scroll" role="region" aria-label="Mercadería recibida" tabIndex={0}>
          <table className="business-table">
            <thead><tr><th scope="col">Producto</th><th scope="col">Variante</th><th scope="col">Cantidad</th><th scope="col">Costo unitario</th>
              <th scope="col">Importe</th><th scope="col"><span className="sr-only">Acciones</span></th></tr></thead>
            <tbody>{filas.map((fila, indice) => {
              const monto = montos[indice]
              const etiqueta = `${fila.producto} · ${fila.variante}`
              return <tr key={fila.varianteId}>
                <th scope="row"><span className="compra-linea">{fila.producto}</span></th>
                <td><span className="compra-linea">{fila.variante}</span></td>
                <td><input type="number" min={1} max={maximoCantidad} step={1} required disabled={busy}
                  aria-label={`Cantidad de ${etiqueta}`} value={fila.cantidad}
                  onChange={event => actualizarFila(fila.varianteId, 'cantidad', event.currentTarget.value)} /></td>
                <td><CostoUnitario fila={fila} disabled={busy} onChange={valor => actualizarFila(fila.varianteId, 'costo', valor)} /></td>
                <td className="tabular-nums whitespace-nowrap"><span className="compra-linea">{monto !== null && monedaCompra.format(monto / 100)}</span></td>
                <td><div className="compra-linea justify-center"><Button type="button" variant="destructive" size="icon-sm" disabled={busy} aria-label={`Quitar ${etiqueta}`}
                  onClick={() => {
                    setFilas(actual => actual.filter(item => item.varianteId !== fila.varianteId))
                    setErrorForm('')
                    clearError()
                  }}><Trash2 size={15} aria-hidden="true" /></Button></div></td>
              </tr>
            })}</tbody>
          </table>
        </div>}
        {(errorForm || error) && <p className="notice error" role="alert">{errorForm || error}</p>}
        <div className="business-table-toolbar compra-total">
          <div>Total en córdobas <strong>{total === null ? '—' : monedaCompra.format(total / 100)}</strong></div>
          <Button type="submit" disabled={busy || !sucursalActual?.activa || !proveedor || !filas.length}>
            <Save size={16} aria-hidden="true" />{busy ? 'Registrando…' : 'Registrar compra'}
          </Button>
        </div>
      </Card>
    </form>
  </div>
}

function CostoUnitario({ fila, disabled, onChange }: { fila: Fila; disabled: boolean; onChange: (valor: string) => void }) {
  const referenciaId = useId()
  const varianteId = fila.varianteId
  const cargar = useCallback((signal: AbortSignal) => obtenerUltimoCosto(varianteId, signal), [varianteId])
  const { result } = useRemote(cargar)
  const costo = result.status === 'ready' ? result.data.costoUnitario : null
  const referencia = costo != null ? `Último costo de compra: ${monedaCompra.format(costo)}`
    : result.status === 'ready' ? 'Sin compras previas'
    : result.status === 'loading' ? 'Cargando último costo…'
    : 'Último costo no disponible'

  return <>
    <input inputMode="decimal" maxLength={13} required disabled={disabled}
      aria-label={`Costo de ${fila.producto} · ${fila.variante}`} aria-describedby={referenciaId} value={fila.costo}
      onChange={event => onChange(event.currentTarget.value)} />
    <small id={referenciaId} className="business-help mt-1 block">{referencia}</small>
  </>
}

function ErrorCarga({ mensaje, etiqueta, onRetry }: { mensaje: string; etiqueta: string; onRetry: () => void }) {
  return <div>
    <p className="notice error" role="alert">{mensaje}</p>
    <Button type="button" variant="outline" onClick={onRetry}>{etiqueta}</Button>
  </div>
}
