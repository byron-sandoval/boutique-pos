import { useCallback, useState } from 'react'
import { Pencil, Plus, ReceiptText, Save } from 'lucide-react'
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
  const [success, setSuccess] = useFlashMessage()
  const datos = result.status === 'ready' ? result.data : undefined
  const sucursalesActivas = datos?.sucursales.filter(sucursal => sucursal.activa) ?? []

  return <Card className="business-series-card">
    <div className="business-list-toolbar">
      <h2 className="business-series-heading"><ReceiptText size={18} aria-hidden="true" /> Series de factura</h2>
      {sucursalesActivas.length > 0 && <Button variant="outline" onClick={() => { setSuccess(''); setEditor({ serie: null }) }}>
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
    {datos?.series.map(serie => <section className="business-series-row" key={serie.id}>
      <div className="business-list-toolbar">
        <h3>{datos.nombres.get(serie.sucursalId)}</h3>
        <div className="business-row-actions">
          <span className={`badge ${serie.activa ? '' : 'inactive'}`}>{serie.activa ? 'Activa' : 'Inactiva'}</span>
          <Button variant="ghost" size="sm" aria-label={`Editar serie ${serie.prefijo} de ${datos.nombres.get(serie.sucursalId)}`} onClick={() => { setSuccess(''); setEditor({ serie }) }}>
            <Pencil size={14} />Editar
          </Button>
        </div>
      </div>
      <dl className="business-invoice-details">
        <div><dt>Prefijo</dt><dd>{serie.prefijo}</dd></div>
        <div><dt>Dígitos</dt><dd>{serie.digitos}</dd></div>
        <div><dt>Siguiente</dt><dd>{Number.isSafeInteger(serie.siguienteNumero) ? String(serie.siguienteNumero).padStart(serie.digitos, '0') : '—'}</dd></div>
      </dl>
    </section>)}
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
  </Card>
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

  async function guardar(form: FormData) {
    const formato = {
      prefijo: String(form.get('prefijo') ?? '').toUpperCase(),
      digitos: Number(form.get('digitos')),
      activa: form.get('activa') === 'on',
    }

    if (serie) {
      await api(`series-factura/${serie.id}`, { method: 'PUT', body: JSON.stringify(formato) })
      return
    }

    await api('series-factura', {
      method: 'POST',
      body: JSON.stringify({ ...formato, sucursalId: Number(form.get('sucursalId')), siguienteNumero: Number(form.get('siguienteNumero')) }),
    })
  }

  return <RecordDialog title={serie ? 'Editar serie de factura' : 'Nueva serie de factura'}
    description={serie ? `Sucursal ${nombreSucursal}. El siguiente número se asigna automáticamente al facturar.` : 'Configura la numeración para una sucursal activa.'}
    busy={busy} onClose={onClose}>
    <form className="business-form" onSubmit={event => {
      event.preventDefault()
      const form = new FormData(event.currentTarget)
      const conflicto = serie
        ? 'Revisa si el prefijo ya existe o si la serie tiene ventas que impiden cambiar su formato.'
        : 'Revisa que la sucursal siga activa y que el prefijo esté disponible.'
      void save(() => guardar(form), onSaved, conflicto)
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
