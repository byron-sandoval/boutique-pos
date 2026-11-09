import { useState } from 'react'
import { Building2, MapPin, Pencil, Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { PanelLink } from '@/routes/PanelLink'
import { useFlashMessage } from '@/lib/use-flash-message'
import { empresaService } from './data'
import { useRemote } from './hooks'
import { EmpresaForm } from './EmpresaForm'
import { SeriesFacturaCard } from './SeriesFacturaCard'
import type { Empresa } from './model'
import './empresa.css'

export function EmpresaPage({ onNombreGuardado }: { onNombreGuardado: (nombre: string) => void }) {
  const { result, reload } = useRemote(empresaService.empresa)
  const [editor, setEditor] = useState<{ empresa: Empresa | null } | null>(null)
  const [success, setSuccess] = useFlashMessage()
  const selected = result.status === 'ready' ? result.data : null

  return <div className="business-page">
    <div className="page-heading"><div><h1>Empresa</h1><p>Los datos y la identidad de tu negocio.</p></div></div>
    {success && <p className="business-success" role="status">{success}</p>}
    {result.status === 'loading' && <Card className="business-empty"><p role="status">Cargando los datos de la empresa…</p></Card>}
    {result.status === 'error' && <Card className="business-empty"><p className="notice error" role="alert">{result.message}</p><Button variant="outline" onClick={reload}>Reintentar</Button></Card>}
    {result.status === 'ready' && !selected && <Card className="business-empty"><Building2 size={30} aria-hidden="true" /><h2>Configura tu empresa</h2><p>Registra los datos del negocio para poder asociar sus sucursales.</p><Button onClick={() => setEditor({ empresa: null })}><Plus size={16} />Registrar empresa</Button></Card>}
    {selected && <>
      <Card className="business-profile">
        <div className="business-profile-heading"><div className="business-profile-name"><span className="business-symbol"><Building2 size={25} aria-hidden="true" /></span><div><h2>{selected.nombre}</h2><p>{selected.eslogan || 'Información del negocio'}</p></div></div><Button variant="outline" onClick={() => { setSuccess(''); setEditor({ empresa: selected }) }}><Pencil size={15} />Editar datos</Button></div>
        <div className="business-detail-grid">
          <section><h3>Datos generales</h3><dl><div><dt>Nombre</dt><dd>{selected.nombre}</dd></div><div><dt>Dirección</dt><dd>{selected.direccion}</dd></div><div><dt>Teléfono</dt><dd>{selected.telefono || 'No registrado'}</dd></div><div><dt>Correo electrónico</dt><dd>{selected.correo || 'No registrado'}</dd></div></dl></section>
          <section><h3>Información fiscal y marca</h3><dl><div><dt>RUC</dt><dd>{selected.ruc || 'No registrado'}</dd></div><div><dt>Régimen fiscal</dt><dd>{selected.regimenFiscal || 'No registrado'}</dd></div><div><dt>IVA</dt><dd>{selected.porcentajeIva.toLocaleString('es-NI', { maximumFractionDigits: 2 })}%</dd></div><div><dt>Logo</dt><dd>{selected.logoUrl ? <img className="business-logo-preview" src={selected.logoUrl} alt={`Logo de ${selected.nombre}`} /> : 'Sin configurar'}</dd></div></dl></section>
        </div>
      </Card>
      <SeriesFacturaCard empresaId={selected.id} />
      <PanelLink href="/configuracion/sucursales" className="business-related"><MapPin size={18} aria-hidden="true" /><span><strong>Sucursales</strong><span>Administra los locales asociados a tu empresa.</span></span><span aria-hidden="true">→</span></PanelLink>
    </>}
    {editor && <EmpresaForm empresa={editor.empresa} onClose={() => setEditor(null)} onSaved={guardada => {
      onNombreGuardado(guardada.nombre)
      setSuccess(editor.empresa ? 'Datos de la empresa actualizados.' : 'Empresa registrada correctamente.')
      setEditor(null)
      reload()
    }} />}
  </div>
}
