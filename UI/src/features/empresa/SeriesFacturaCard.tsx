import { useCallback, useState } from 'react'
import { Pencil, Plus, Power, ReceiptText, Save, Store } from 'lucide-react'
import { api } from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { useFlashMessage } from '@/lib/use-flash-message'
import { empresaService } from './data'
import { useRemote, useSave } from './hooks'
import type { Page, Sucursal } from './model'
import { RecordDialog } from './RecordDialog'

type SerieFactura = {
  id: number
  sucursalId: number
  prefijo: string
  digitos: number
  siguienteNumero: number
  activa: boolean
}

const mensajeSerieConVentas = 'No se puede desactivar esta serie de factura porque tiene ventas registradas.'

async function listarSeries(signal: AbortSignal) {
  const series: SerieFactura[] = []
  let totalPages = 1
  for (let page = 0; page < totalPages; page++) {
    signal.throwIfAborted()
    const result = await api<Page<SerieFactura>>(`series-factura?page=${page}&size=100&sort=id,asc`, { signal })
    series.push(...result.content)
    totalPages = result.page.totalPages
  }
  return series
}

export function SeriesFacturaCard({ empresaId }: { empresaId: number }) {
  const cargar = useCallback(async (signal: AbortSignal) => {
    const [sucursales, series] = await Promise.all([empresaService.sucursales(empresaId, signal), listarSeries(signal)])
    const nombres = new Map(sucursales.map(sucursal => [sucursal.id, sucursal.nombre]))
    return { sucursales, nombres, series: series.filter(serie => nombres.has(serie.sucursalId)) }
  }, [empresaId])
  const { result, reload } = useRemote(cargar)
  const [editor, setEditor] = useState<{ serie: SerieFactura | null } | null>(null)
  const [serieEstado, setSerieEstado] = useState<SerieFactura | null>(null)
  const [success, setSuccess] = useFlashMessage()
  const datos = result.status === 'ready' ? result.data : undefined
  const sucursalesActivas = datos?.sucursales.filter(sucursal => sucursal.activa) ?? []

  return <Card className="business-table-card business-series-card">
    <div className="business-table-toolbar">
      <div className="business-section-heading">
        <span className="business-symbol"><ReceiptText size={20} aria-hidden="true" /></span>
        <div><h2>Series de factura</h2><p>Configura el prefijo y la numeración de tus facturas.</p></div>
      </div>
      {sucursalesActivas.length > 0 && <Button onClick={() => { setSuccess(''); setEditor({ serie: null }) }}>
        <Plus size={16} />Nueva serie
      </Button>}
    </div>
    {success && <p className="business-success" role="status">{success}</p>}
    {result.status === 'loading' && <p role="status">Cargando series de factura…</p>}
    {result.status === 'error' && <div>
      <p className="notice error" role="alert">{result.message}</p>
      <Button variant="outline" onClick={reload}>Reintentar</Button>
    </div>}
    {datos && datos.series.length === 0 && <p>
      {sucursalesActivas.length > 0 ? 'Todavía no hay series de factura registradas.' : 'Necesitas una sucursal activa para crear una serie.'}
    </p>}
    {datos && datos.series.length > 0 && <div className="business-table-scroll" role="region" aria-label="Series de factura" tabIndex={0}>
      <table className="business-table business-series-table">
        <thead><tr>
          <th scope="col">Sucursal</th>
          <th scope="col">Prefijo</th>
          <th scope="col">Dígitos</th>
          <th scope="col">Siguiente</th>
          <th scope="col">Estado</th>
          <th scope="col">Acciones</th>
        </tr></thead>
        <tbody>{datos.series.map(serie => <tr key={serie.id}>
          <th scope="row"><span className="business-table-name"><Store size={14} aria-hidden="true" />{datos.nombres.get(serie.sucursalId)}</span></th>
          <td>{serie.prefijo}</td>
          <td>{serie.digitos}</td>
          <td className="business-series-next">{Number.isSafeInteger(serie.siguienteNumero) ? String(serie.siguienteNumero).padStart(serie.digitos, '0') : '—'}</td>
          <td><span className={`business-status ${serie.activa ? '' : 'is-inactive'}`}>{serie.activa ? 'Activa' : 'Inactiva'}</span></td>
          <td><div className="business-table-actions">
            <Button variant="outline" size="icon-sm"
              aria-label={`Editar serie ${serie.prefijo} de ${datos.nombres.get(serie.sucursalId)}`}
              title="Editar serie" onClick={() => { setSuccess(''); setEditor({ serie }) }}>
              <Pencil size={14} aria-hidden="true" />
            </Button>
            <Button variant="outline" size="icon-sm"
              data-table-action={serie.activa ? 'deactivate' : 'activate'}
              aria-label={`${serie.activa ? 'Desactivar' : 'Activar'} serie ${serie.prefijo} de ${datos.nombres.get(serie.sucursalId)}`}
              title={serie.activa ? 'Desactivar serie' : 'Activar serie'}
              onClick={() => { setSuccess(''); setSerieEstado(serie) }}>
              <Power size={14} aria-hidden="true" />
            </Button>
          </div></td>
        </tr>)}</tbody>
      </table>
    </div>}
    {editor && datos && <SerieFacturaForm
      serie={editor.serie}
      sucursales={sucursalesActivas}
      nombreSucursal={editor.serie ? datos.nombres.get(editor.serie.sucursalId) : undefined}
      onClose={() => setEditor(null)}
      onSaved={() => {
        setSuccess(editor.serie ? 'Serie de factura actualizada.' : 'Serie de factura registrada.')
        setEditor(null)
        reload()
      }}
    />}
    {serieEstado && <SerieEstadoDialog serie={serieEstado} onClose={() => setSerieEstado(null)} onSaved={() => {
      setSuccess(serieEstado.activa ? 'Serie de factura desactivada.' : 'Serie de factura activada.')
      setSerieEstado(null)
      reload()
    }} />}
  </Card>
}

function SerieEstadoDialog({ serie, onClose, onSaved }: {
  serie: SerieFactura
  onClose: () => void
  onSaved: () => void
}) {
  const { busy, error, save } = useSave()
  const titulo = serie.activa ? 'Desactivar serie' : 'Activar serie'

  return <RecordDialog title={titulo} description={`Serie ${serie.prefijo}`} busy={busy} onClose={onClose}>
    <div className="business-state-body">
      <p>{serie.activa
        ? 'La serie dejará de estar disponible para nuevas facturas.'
        : 'La serie volverá a estar disponible para nuevas facturas.'}</p>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button variant={serie.activa ? 'destructive' : 'default'} disabled={busy}
          onClick={() => void save(() => api(`series-factura/${serie.id}/estado`, {
            method: 'PATCH',
            body: JSON.stringify({ activa: !serie.activa }),
          }), onSaved, serie.activa ? mensajeSerieConVentas : undefined)}>
          {busy ? 'Guardando…' : titulo}
        </Button>
      </div>
    </div>
  </RecordDialog>
}

type FormProps = {
  serie: SerieFactura | null
  sucursales: Sucursal[]
  nombreSucursal?: string
  onClose: () => void
  onSaved: () => void
}

function SerieFacturaForm({ serie, sucursales, nombreSucursal, onClose, onSaved }: FormProps) {
  const { busy, error, save } = useSave()

  return <RecordDialog title={serie ? 'Editar serie de factura' : 'Nueva serie de factura'}
    description={serie ? `Sucursal ${nombreSucursal}. El siguiente número se asigna automáticamente al facturar.` : 'Configura la numeración para una sucursal activa.'}
    busy={busy} onClose={onClose}>
    <form className="business-form" onSubmit={event => {
      event.preventDefault()
      const form = new FormData(event.currentTarget)
      const formato = {
        prefijo: String(form.get('prefijo') ?? '').toUpperCase(),
        digitos: Number(form.get('digitos')),
        activa: form.get('activa') === 'on',
      }

      if (serie) {
        const desactiva = serie.activa && !formato.activa
        const cambiaFormato = formato.prefijo !== serie.prefijo || formato.digitos !== serie.digitos
        let conflicto = 'Revisa si el prefijo ya existe o si la serie tiene ventas que impiden cambiar su formato.'
        if (desactiva) {
          conflicto = cambiaFormato
            ? 'Una serie con ventas debe permanecer activa y conservar su formato. Revisa también que el prefijo esté disponible.'
            : mensajeSerieConVentas
        }
        void save(() => api(`series-factura/${serie.id}`, { method: 'PUT', body: JSON.stringify(formato) }), onSaved, conflicto)
      } else {
        void save(() => api('series-factura', {
          method: 'POST',
          body: JSON.stringify({ ...formato, sucursalId: Number(form.get('sucursalId')), siguienteNumero: Number(form.get('siguienteNumero')) }),
        }), onSaved, 'Revisa que la sucursal siga activa y que el prefijo esté disponible.')
      }
    }}>
      <fieldset disabled={busy}>
        <legend>Numeración de facturas</legend>
        <div className="business-fields">
          {!serie && <label className="business-field-wide">Sucursal
            <select name="sucursalId" required defaultValue={sucursales.length === 1 ? sucursales[0].id : ''}>
              <option value="" disabled>Selecciona una sucursal</option>
              {sucursales.map(sucursal => <option value={sucursal.id} key={sucursal.id}>{sucursal.nombre}</option>)}
            </select>
          </label>}
          <label>Prefijo<input name="prefijo" required maxLength={20} pattern={String.raw`[A-Za-z0-9_\-]+`} defaultValue={serie?.prefijo ?? 'FAC-'} /></label>
          <label>Dígitos<input name="digitos" type="number" required min={1} max={15} step={1} defaultValue={serie?.digitos ?? 6} /></label>
          {!serie && <label>Número inicial<input name="siguienteNumero" type="number" required min={1} max={Number.MAX_SAFE_INTEGER} step={1} defaultValue={1} /></label>}
          <label className="business-checkbox business-field-wide"><input name="activa" type="checkbox" defaultChecked={serie?.activa ?? true} />Serie activa</label>
        </div>
      </fieldset>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button type="button" variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button type="submit" disabled={busy}><Save size={15} />{busy ? 'Guardando…' : 'Guardar serie'}</Button>
      </div>
    </form>
  </RecordDialog>
}
