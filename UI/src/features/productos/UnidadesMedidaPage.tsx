import { useState } from 'react'
import { Pencil, Plus, Power, Ruler } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { RecordDialog } from '@/features/empresa/RecordDialog'
import { useRemote, useSave } from '@/features/empresa/hooks'
import { useFlashMessage } from '@/lib/use-flash-message'
import { cambiarEstadoUnidad, guardarUnidad, listarUnidades } from './data'
import type { Unidad } from './data'
import '@/features/empresa/empresa.css'

type Dialogo = { tipo: 'formulario'; unidad: Unidad | null } | { tipo: 'estado'; unidad: Unidad }

export function UnidadesMedidaPage({ puedeEditar }: { puedeEditar: boolean }) {
  const { result, reload } = useRemote(listarUnidades)
  const [dialogo, setDialogo] = useState<Dialogo | null>(null)
  const [success, setSuccess] = useFlashMessage()
  const unidades = result.status === 'ready'
    ? [...result.data].sort((a, b) => Number(b.activo) - Number(a.activo)) : null

  function abrir(next: Dialogo) {
    setSuccess('')
    setDialogo(next)
  }

  return <div className="business-page">
    <div className="page-heading business-page-heading"><h1>Unidades de medida</h1></div>
    {success && <p className="business-success" role="status">{success}</p>}
    <Card className="business-table-card">
      <div className="business-table-toolbar">
        <div className="business-section-heading">
          <span className="business-symbol"><Ruler size={20} aria-hidden="true" /></span>
          <div><h2>Unidades del catálogo</h2><p>Define cómo se cuenta o mide cada producto.</p></div>
        </div>
        {puedeEditar && <Button onClick={() => abrir({ tipo: 'formulario', unidad: null })}>
          <Plus size={16} aria-hidden="true" />Nueva unidad
        </Button>}
      </div>
      {result.status === 'loading' && <p role="status">Cargando unidades de medida…</p>}
      {result.status === 'error' && <div>
        <p className="notice error" role="alert">{result.message}</p>
        <Button variant="outline" onClick={reload}>Reintentar</Button>
      </div>}
      {unidades && (unidades.length === 0
        ? <p>Todavía no hay unidades de medida registradas.</p>
        : <div className="business-table-scroll" role="region" aria-label="Unidades del catálogo" tabIndex={0}>
          <table className="business-table">
            <thead><tr>
              <th scope="col">Nombre</th><th scope="col">Símbolo</th><th scope="col">Estado</th>
              {puedeEditar && <th scope="col">Acciones</th>}
            </tr></thead>
            <tbody>{unidades.map(unidad => <tr key={unidad.id}>
              <th scope="row">{unidad.nombre}</th><td>{unidad.simbolo || '—'}</td>
              <td><span className={`business-status ${unidad.activo ? '' : 'is-inactive'}`}>{unidad.activo ? 'Activa' : 'Inactiva'}</span></td>
              {puedeEditar && <td><div className="business-table-actions">
                <Button variant="outline" size="icon-sm" data-table-action="edit" title="Editar unidad"
                  aria-label={`Editar unidad ${unidad.nombre}`} onClick={() => abrir({ tipo: 'formulario', unidad })}>
                  <Pencil size={14} aria-hidden="true" />
                </Button>
                <Button variant="outline" size="icon-sm" data-table-action={unidad.activo ? 'deactivate' : 'activate'}
                  title={unidad.activo ? 'Desactivar unidad' : 'Activar unidad'}
                  aria-label={`${unidad.activo ? 'Desactivar' : 'Activar'} unidad ${unidad.nombre}`}
                  onClick={() => abrir({ tipo: 'estado', unidad })}>
                  <Power size={14} aria-hidden="true" />
                </Button>
              </div></td>}
            </tr>)}</tbody>
          </table>
        </div>)}
    </Card>
    {puedeEditar && dialogo && <UnidadDialog dialogo={dialogo} onClose={() => setDialogo(null)} onSaved={mensaje => {
      setSuccess(mensaje)
      setDialogo(null)
      reload()
    }} />}
  </div>
}

function UnidadDialog({ dialogo, onClose, onSaved }: {
  dialogo: Dialogo
  onClose: () => void
  onSaved: (mensaje: string) => void
}) {
  const { busy, error, save } = useSave()
  if (dialogo.tipo === 'estado') {
    const { unidad } = dialogo
    const titulo = unidad.activo ? 'Desactivar unidad' : 'Activar unidad'
    return <RecordDialog title={titulo} description={unidad.nombre} busy={busy} onClose={onClose}>
      <div className="business-state-body">
        <p>{unidad.activo ? 'Dejará de estar disponible para asignarla a nuevos productos.' : 'Volverá a estar disponible para el catálogo.'}</p>
        {error && <p className="notice error" role="alert">{error}</p>}
        <div className="business-form-actions">
          <Button variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
          <Button variant={unidad.activo ? 'destructive' : 'default'} disabled={busy}
            onClick={() => void save(() => cambiarEstadoUnidad(unidad), actualizada => onSaved(actualizada.activo ? 'Unidad activada.' : 'Unidad desactivada.'))}>
            {busy ? 'Guardando…' : titulo}
          </Button>
        </div>
      </div>
    </RecordDialog>
  }

  const { unidad } = dialogo
  return <RecordDialog title={unidad ? 'Editar unidad de medida' : 'Nueva unidad de medida'}
    description="El símbolo es opcional. Por ejemplo, Par puede abreviarse como par." busy={busy} onClose={onClose}>
    <form className="business-form" onSubmit={event => {
      event.preventDefault()
      const form = new FormData(event.currentTarget)
      const nombre = String(form.get('nombre') ?? '').trim()
      const simbolo = String(form.get('simbolo') ?? '').trim() || null
      void save(() => guardarUnidad({ nombre, simbolo, activo: unidad?.activo ?? true }, unidad?.id),
        () => onSaved(unidad ? 'Unidad actualizada.' : 'Unidad registrada.'))
    }}>
      <fieldset disabled={busy}>
        <div className="business-fields">
          <label>Nombre<input name="nombre" required pattern={String.raw`.*\S.*`} maxLength={30} defaultValue={unidad?.nombre} autoFocus /></label>
          <label>Símbolo<input name="simbolo" maxLength={10} defaultValue={unidad?.simbolo ?? ''} /></label>
        </div>
      </fieldset>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button type="button" variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button type="submit" disabled={busy}>{busy ? 'Guardando…' : 'Guardar unidad'}</Button>
      </div>
    </form>
  </RecordDialog>
}
