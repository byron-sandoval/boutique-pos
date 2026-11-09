import { useState } from 'react'
import { Coins, Pencil, Plus, Power, Save } from 'lucide-react'
import { api } from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { RecordDialog } from '@/features/empresa/RecordDialog'
import { useRemote, useSave } from '@/features/empresa/hooks'
import type { Page } from '@/features/empresa/model'
import { useFlashMessage } from '@/lib/use-flash-message'
import '@/features/empresa/empresa.css'

type Moneda = {
  id: number
  codigo: string
  nombre: string
  simbolo: string
  activo: boolean
}

async function listarMonedas(signal: AbortSignal) {
  const monedas: Moneda[] = []
  let totalPages = 1
  for (let page = 0; page < totalPages; page++) {
    signal.throwIfAborted()
    const result = await api<Page<Moneda>>(`monedas?page=${page}&size=100&sort=codigo,asc`, { signal })
    monedas.push(...result.content)
    totalPages = result.page.totalPages
  }
  return monedas
}

export function MonedasPage() {
  const { result, reload } = useRemote(listarMonedas)
  const [editor, setEditor] = useState<{ moneda: Moneda | null } | null>(null)
  const [monedaEstado, setMonedaEstado] = useState<Moneda | null>(null)
  const [success, setSuccess] = useFlashMessage()

  function abrirEditor(moneda: Moneda | null) {
    setSuccess('')
    setEditor({ moneda })
  }

  return <div className="business-page">
    <div className="page-heading business-page-heading">
      <div><h1>Monedas</h1><p>Administra las monedas que usas para cobrar.</p></div>
    </div>
    {success && <p className="business-success" role="status">{success}</p>}
    <Card className="business-table-card">
      <div className="business-table-toolbar">
        <div className="business-section-heading">
          <span className="business-symbol"><Coins size={20} aria-hidden="true" /></span>
          <div><h2>Listado de monedas</h2><p>Código, símbolo y estado de cada moneda.</p></div>
        </div>
        <Button onClick={() => abrirEditor(null)}><Plus size={16} />Nueva moneda</Button>
      </div>
      {result.status === 'loading' && <p role="status">Cargando monedas…</p>}
      {result.status === 'error' && <div>
        <p className="notice error" role="alert">{result.message}</p>
        <Button variant="outline" onClick={reload}>Reintentar</Button>
      </div>}
      {result.status === 'ready' && (result.data.length === 0
        ? <p>Todavía no hay monedas registradas.</p>
        : <div className="business-table-scroll" role="region" aria-label="Listado de monedas" tabIndex={0}>
          <table className="business-table">
            <thead><tr>
              <th scope="col">Moneda</th>
              <th scope="col">Código</th>
              <th scope="col">Símbolo</th>
              <th scope="col">Estado</th>
              <th scope="col">Acciones</th>
            </tr></thead>
            <tbody>{result.data.map(moneda => <tr key={moneda.id}>
              <th scope="row"><span className="business-table-name"><Coins size={14} aria-hidden="true" />{moneda.nombre}</span></th>
              <td>{moneda.codigo}</td>
              <td>{moneda.simbolo}</td>
              <td><span className={`business-status ${moneda.activo ? '' : 'is-inactive'}`}>{moneda.activo ? 'Activa' : 'Inactiva'}</span></td>
              <td><div className="business-table-actions">
                <Button variant="outline" size="icon-sm"
                  aria-label={`Editar moneda ${moneda.codigo}`} title="Editar moneda" onClick={() => abrirEditor(moneda)}>
                  <Pencil size={14} aria-hidden="true" />
                </Button>
                <Button variant="outline" size="icon-sm"
                  data-table-action={moneda.activo ? 'deactivate' : 'activate'}
                  title={moneda.activo ? 'Desactivar moneda' : 'Activar moneda'}
                  aria-label={`${moneda.activo ? 'Desactivar' : 'Activar'} moneda ${moneda.codigo}`}
                  onClick={() => { setSuccess(''); setMonedaEstado(moneda) }}>
                  <Power size={14} aria-hidden="true" />
                </Button>
              </div></td>
            </tr>)}</tbody>
          </table>
        </div>
      )}
    </Card>
    {editor && <MonedaForm moneda={editor.moneda} onClose={() => setEditor(null)} onSaved={() => {
      setSuccess(editor.moneda ? 'Moneda actualizada correctamente.' : 'Moneda registrada correctamente.')
      setEditor(null)
      reload()
    }} />}
    {monedaEstado && <MonedaEstadoDialog moneda={monedaEstado} onClose={() => setMonedaEstado(null)} onSaved={() => {
      setSuccess(monedaEstado.activo ? 'Moneda desactivada.' : 'Moneda activada.')
      setMonedaEstado(null)
      reload()
    }} />}
  </div>
}

function MonedaEstadoDialog({ moneda, onClose, onSaved }: {
  moneda: Moneda
  onClose: () => void
  onSaved: () => void
}) {
  const { busy, error, save } = useSave()
  const titulo = moneda.activo ? 'Desactivar moneda' : 'Activar moneda'

  return <RecordDialog title={titulo} description={`${moneda.nombre} (${moneda.codigo})`} busy={busy} onClose={onClose}>
    <div className="business-state-body">
      <p>{moneda.activo
        ? 'Esta moneda dejará de estar disponible para nuevos cobros.'
        : 'Esta moneda volverá a estar disponible para nuevos cobros.'}</p>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button variant={moneda.activo ? 'destructive' : 'default'} disabled={busy}
          onClick={() => void save(() => api(`monedas/${moneda.id}/estado`, {
            method: 'PATCH',
            body: JSON.stringify({ activo: !moneda.activo }),
          }), onSaved)}>
          {busy ? 'Guardando…' : titulo}
        </Button>
      </div>
    </div>
  </RecordDialog>
}

function MonedaForm({ moneda, onClose, onSaved }: {
  moneda: Moneda | null
  onClose: () => void
  onSaved: () => void
}) {
  const { busy, error, save } = useSave()
  return <RecordDialog title={moneda ? 'Editar moneda' : 'Nueva moneda'}
    description="Configura cómo se identifica esta moneda en los cobros."
    busy={busy} onClose={onClose}>
    <form className="business-form" onSubmit={event => {
      event.preventDefault()
      const form = new FormData(event.currentTarget)
      const datos = {
        codigo: String(form.get('codigo') ?? '').toUpperCase(),
        nombre: String(form.get('nombre') ?? '').trim(),
        simbolo: String(form.get('simbolo') ?? '').trim(),
        activo: form.get('activo') === 'on',
      }
      const conflicto = moneda
        ? 'El código puede estar en uso por otra moneda o protegido por ventas registradas.'
        : 'Ya existe una moneda con ese código.'
      void save(async () => {
        if (!datos.nombre || !datos.simbolo) throw new Error('Escribe el nombre y el símbolo de la moneda.')
        return api(moneda ? `monedas/${moneda.id}` : 'monedas', {
          method: moneda ? 'PUT' : 'POST',
          body: JSON.stringify(datos),
        })
      }, onSaved, conflicto)
    }}>
      <fieldset disabled={busy}>
        <legend>Datos de la moneda</legend>
        <div className="business-fields">
          <label>Código de tres letras<input name="codigo" defaultValue={moneda?.codigo ?? ''} pattern="[A-Za-z]{3}" maxLength={3} required autoFocus /></label>
          <label>Símbolo<input name="simbolo" defaultValue={moneda?.simbolo ?? ''} maxLength={5} required /></label>
          <label className="business-field-wide">Nombre<input name="nombre" defaultValue={moneda?.nombre ?? ''} maxLength={50} required /></label>
          <label className="business-checkbox business-field-wide"><input name="activo" type="checkbox" defaultChecked={moneda?.activo ?? true} />Moneda activa</label>
        </div>
      </fieldset>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button type="button" variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button type="submit" disabled={busy}><Save size={15} />{busy ? 'Guardando…' : 'Guardar moneda'}</Button>
      </div>
    </form>
  </RecordDialog>
}
