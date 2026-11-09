import { useCallback, useState } from 'react'
import { MapPin, Pencil, Plus, Power } from 'lucide-react'
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
    <div className="page-heading"><div><h1>Sucursales</h1><p>Gestiona los locales de tu negocio.</p></div></div>
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
    <div className="page-heading">
      <div><h1>Sucursales</h1><p>Gestiona los locales de {empresa.nombre}.</p></div>
      <Button onClick={() => { setSuccess(''); setEditor({ sucursal: null }) }}><Plus size={16} />Nueva sucursal</Button>
    </div>
    {success && <p className="business-success" role="status">{success}</p>}
    <Card className="business-list">
      <div className="business-list-toolbar"><h2>Listado de sucursales</h2></div>
      {result.status === 'loading' && <p className="business-loading" role="status">Cargando sucursales…</p>}
      {result.status === 'error' && <div className="business-empty"><p className="notice error" role="alert">{result.message}</p><Button variant="outline" onClick={reload}>Reintentar</Button></div>}
      {result.status === 'ready' && (
        result.data.length === 0 ? <div className="business-empty"><MapPin size={30} aria-hidden="true" /><h3>No hay sucursales registradas</h3><p>Registra un local de tu negocio.</p><Button onClick={() => setEditor({ sucursal: null })}><Plus size={16} />Nueva sucursal</Button></div>
          : <div className="table-scroll"><table className="business-table"><thead><tr><th>Sucursal</th><th>Dirección</th><th>Teléfono</th><th>Estado</th><th className="text-right">Acciones</th></tr></thead><tbody>{result.data.map(sucursal => <tr key={sucursal.id}>
            <td><strong>{sucursal.nombre}</strong></td><td>{sucursal.direccion}</td><td>{sucursal.telefono || '—'}</td><td><span className={`badge ${sucursal.activa ? '' : 'inactive'}`}>{sucursal.activa ? 'Activa' : 'Inactiva'}</span></td><td><div className="business-row-actions"><Button variant="ghost" size="sm" aria-label={`Editar sucursal ${sucursal.nombre}`} onClick={() => { setSuccess(''); setEditor({ sucursal }) }}><Pencil size={14} />Editar</Button><Button variant="ghost" size="sm" aria-label={`${sucursal.activa ? 'Desactivar' : 'Activar'} sucursal ${sucursal.nombre}`} onClick={() => { setSuccess(''); setStateEditor(sucursal) }}><Power size={14} />{sucursal.activa ? 'Desactivar' : 'Activar'}</Button></div></td>
          </tr>)}</tbody></table></div>
      )}
    </Card>
    {editor && <SucursalForm sucursal={editor.sucursal} empresa={empresa} onClose={() => setEditor(null)} onSaved={() => { setSuccess(editor.sucursal ? 'Sucursal actualizada correctamente.' : 'Sucursal registrada correctamente.'); setEditor(null); reload() }} />}
    {stateEditor && <SucursalEstadoDialog sucursal={stateEditor} onClose={() => setStateEditor(null)} onSaved={() => { setSuccess(stateEditor.activa ? 'Sucursal desactivada.' : 'Sucursal activada.'); setStateEditor(null); reload() }} />}
  </div>
}
