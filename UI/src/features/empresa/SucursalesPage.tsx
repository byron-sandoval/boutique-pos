import { useCallback, useState } from 'react'
import { MapPin, Pencil, Plus, Power, Store } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { PanelLink } from '@/routes/PanelLink'
import { useFlashMessage } from '@/lib/use-flash-message'
import { empresaService } from './data'
import { useRemote } from './hooks'
import type { Empresa, Sucursal } from './model'
import { SucursalForm } from './SucursalForm'
import { SucursalEstadoDialog } from './SucursalEstadoDialog'
import './empresa.css'

export function SucursalesPage() {
  const { result, reload } = useRemote(empresaService.empresa)
  if (result.status === 'ready' && result.data) return <SucursalesContent empresa={result.data} />
  return <div className="business-page">
    <div className="page-heading business-page-heading"><div><h1>Sucursales</h1><p>Gestiona los locales de tu negocio.</p></div></div>
    {result.status === 'loading' && <Card className="business-empty"><p role="status">Cargando la empresa…</p></Card>}
    {result.status === 'error' && <Card className="business-empty"><p className="notice error" role="alert">{result.message}</p><Button variant="outline" onClick={reload}>Reintentar</Button></Card>}
    {result.status === 'ready' && !result.data && <Card className="business-empty"><MapPin size={30} aria-hidden="true" /><h2>Primero configura tu empresa</h2><p>Después podrás registrar los locales del negocio.</p><PanelLink href="/configuracion/empresa" className="text-link">Configurar empresa</PanelLink></Card>}
  </div>
}

function SucursalesContent({ empresa }: { empresa: Empresa }) {
  const load = useCallback((signal: AbortSignal) => empresaService.sucursales(empresa.id, signal), [empresa.id])
  const { result, reload } = useRemote(load)
  const [editor, setEditor] = useState<{ sucursal: Sucursal | null } | null>(null)
  const [stateEditor, setStateEditor] = useState<Sucursal | null>(null)
  const [success, setSuccess] = useFlashMessage()

  return <div className="business-page">
    <div className="page-heading business-page-heading">
      <div><h1>Sucursales</h1><p>Gestiona los locales de {empresa.nombre}.</p></div>
    </div>
    {success && <p className="business-success" role="status">{success}</p>}
    <Card className="business-table-card">
      <div className="business-table-toolbar">
        <div className="business-section-heading">
          <span className="business-symbol"><Store size={20} aria-hidden="true" /></span>
          <div><h2>Listado de sucursales</h2><p>Dirección, teléfono y estado de cada local.</p></div>
        </div>
        <Button onClick={() => { setSuccess(''); setEditor({ sucursal: null }) }}><Plus size={16} />Nueva sucursal</Button>
      </div>
      {result.status === 'loading' && <p role="status">Cargando sucursales…</p>}
      {result.status === 'error' && <div>
        <p className="notice error" role="alert">{result.message}</p>
        <Button variant="outline" onClick={reload}>Reintentar</Button>
      </div>}
      {result.status === 'ready' && (result.data.length === 0
        ? <p>Todavía no hay sucursales registradas.</p>
        : <div className="business-table-scroll" role="region" aria-label="Listado de sucursales" tabIndex={0}>
          <table className="business-table branch-table">
            <thead><tr>
              <th scope="col">Sucursal</th>
              <th scope="col">Dirección</th>
              <th scope="col">Teléfono</th>
              <th scope="col">Estado</th>
              <th scope="col">Acciones</th>
            </tr></thead>
            <tbody>{result.data.map(sucursal => <tr key={sucursal.id}>
              <th scope="row"><span className="business-table-name"><Store size={14} aria-hidden="true" />{sucursal.nombre}</span></th>
              <td>{sucursal.direccion}</td>
              <td>{sucursal.telefono || 'No registrado'}</td>
              <td><span className={`business-status ${sucursal.activa ? '' : 'is-inactive'}`}>{sucursal.activa ? 'Activa' : 'Inactiva'}</span></td>
              <td><div className="business-table-actions">
                <Button variant="outline" size="icon-sm" title="Editar sucursal"
                  aria-label={`Editar sucursal ${sucursal.nombre}`} onClick={() => { setSuccess(''); setEditor({ sucursal }) }}>
                  <Pencil size={14} aria-hidden="true" />
                </Button>
                <Button variant="outline" size="icon-sm"
                  data-table-action={sucursal.activa ? 'deactivate' : 'activate'}
                  title={sucursal.activa ? 'Desactivar sucursal' : 'Activar sucursal'}
                  aria-label={`${sucursal.activa ? 'Desactivar' : 'Activar'} sucursal ${sucursal.nombre}`}
                  onClick={() => { setSuccess(''); setStateEditor(sucursal) }}>
                  <Power size={14} aria-hidden="true" />
                </Button>
              </div></td>
            </tr>)}</tbody>
          </table>
        </div>
      )}
    </Card>
    {editor && <SucursalForm sucursal={editor.sucursal} empresa={empresa} onClose={() => setEditor(null)} onSaved={() => { setSuccess(editor.sucursal ? 'Sucursal actualizada correctamente.' : 'Sucursal registrada correctamente.'); setEditor(null); reload() }} />}
    {stateEditor && <SucursalEstadoDialog sucursal={stateEditor} onClose={() => setStateEditor(null)} onSaved={() => { setSuccess(stateEditor.activa ? 'Sucursal desactivada.' : 'Sucursal activada.'); setStateEditor(null); reload() }} />}
  </div>
}
