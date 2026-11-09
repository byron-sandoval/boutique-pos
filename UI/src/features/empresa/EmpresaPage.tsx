import { useState } from 'react'
import type { ReactNode } from 'react'
import { Building2, ChevronRight, FileText, Image, Layers3, Mail, MapPin, Pencil, Percent, Phone, Plus, ShoppingBag, Store, Upload } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
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

function BusinessDetailCard({ icon: Icon, title, description, className, children }: {
  icon: LucideIcon
  title: string
  description: string
  className?: string
  children: ReactNode
}) {
  return <Card className={`business-detail-card ${className ?? ''}`}>
    <div className="business-section-heading">
      <span className="business-symbol"><Icon size={20} aria-hidden="true" /></span>
      <div><h2>{title}</h2><p>{description}</p></div>
    </div>
    {children}
  </Card>
}

function BusinessDetail({ icon: Icon, label, children }: {
  icon: LucideIcon
  label: string
  children: ReactNode
}) {
  return <div>
    <dt><span><Icon size={14} aria-hidden="true" /></span>{label}</dt>
    <dd>{children}</dd>
  </div>
}

export function EmpresaPage({ onNombreGuardado }: { onNombreGuardado: (nombre: string) => void }) {
  const { result, reload } = useRemote(empresaService.empresa)
  const [editor, setEditor] = useState<{ empresa: Empresa | null; focusLogo?: boolean } | null>(null)
  const [success, setSuccess] = useFlashMessage()
  const selected = result.status === 'ready' ? result.data : null

  function editar(empresa: Empresa, focusLogo = false) {
    setSuccess('')
    setEditor({ empresa, focusLogo })
  }

  return <div className="business-page">
    <div className="page-heading business-page-heading">
      <div><h1>Empresa</h1><p>Los datos y la identidad de tu negocio.</p></div>
      {selected && <Button variant="outline" onClick={() => editar(selected)}><Pencil size={15} />Editar datos</Button>}
    </div>
    {success && <p className="business-success" role="status">{success}</p>}
    {result.status === 'loading' && <Card className="business-empty"><p role="status">Cargando los datos de la empresa…</p></Card>}
    {result.status === 'error' && <Card className="business-empty"><p className="notice error" role="alert">{result.message}</p><Button variant="outline" onClick={reload}>Reintentar</Button></Card>}
    {result.status === 'ready' && !selected && <Card className="business-empty"><Building2 size={30} aria-hidden="true" /><h2>Configura tu empresa</h2><p>Registra los datos del negocio para poder asociar sus sucursales.</p><Button onClick={() => setEditor({ empresa: null })}><Plus size={16} />Registrar empresa</Button></Card>}
    {selected && <>
      <div className="business-detail-grid">
        <BusinessDetailCard icon={Building2} title="Datos generales" description="Información básica del negocio.">
          <dl className="business-details">
            <BusinessDetail icon={Store} label="Nombre comercial">{selected.nombre}</BusinessDetail>
            <BusinessDetail icon={MapPin} label="Dirección">{selected.direccion}</BusinessDetail>
            <BusinessDetail icon={Phone} label="Teléfono">{selected.telefono || 'No registrado'}</BusinessDetail>
            <BusinessDetail icon={Mail} label="Correo electrónico">{selected.correo || 'No registrado'}</BusinessDetail>
          </dl>
        </BusinessDetailCard>
        <BusinessDetailCard icon={FileText} title="Información fiscal" description="Datos fiscales y de impuestos." className="business-fiscal-card">
          <dl className="business-details">
            <BusinessDetail icon={FileText} label="RUC">{selected.ruc || 'No registrado'}</BusinessDetail>
            <BusinessDetail icon={Layers3} label="Régimen fiscal">{selected.regimenFiscal || 'No registrado'}</BusinessDetail>
            <BusinessDetail icon={Percent} label="IVA">
              <span className="business-tax-pill">{selected.porcentajeIva.toLocaleString('es-NI', { maximumFractionDigits: 2 })}%</span>
            </BusinessDetail>
          </dl>
        </BusinessDetailCard>
        <BusinessDetailCard icon={Image} title="Identidad de marca" description="Logo y elementos de la empresa." className="business-brand-card">
          <div className="business-brand-content">
            <div className="business-brand-logo">
              {selected.logoUrl
                ? <img src={selected.logoUrl} alt={`Logo de ${selected.nombre}`} />
                : <ShoppingBag size={42} strokeWidth={1.6} aria-hidden="true" />}
            </div>
            <div className="business-brand-copy">
              <p>Logo del negocio</p>
              <strong>{selected.logoUrl ? 'Configurado' : 'Sin configurar'}</strong>
              <Button variant="outline" size="sm" className="business-brand-upload" onClick={() => editar(selected, true)}>
                <Upload size={14} />{selected.logoUrl ? 'Cambiar logo' : 'Subir logo'}
              </Button>
            </div>
          </div>
          {selected.eslogan && <p className="business-brand-tagline">{selected.eslogan}</p>}
        </BusinessDetailCard>
      </div>
      <SeriesFacturaCard empresaId={selected.id} />
      <PanelLink href="/configuracion/sucursales" className="business-related">
        <span className="business-symbol"><Store size={20} aria-hidden="true" /></span>
        <span className="business-related-copy">
          <strong>Sucursales</strong>
          <span>Administra los locales asociados a tu empresa.</span>
        </span>
        <Store className="business-related-art" size={104} strokeWidth={1} aria-hidden="true" />
        <span className="business-related-arrow"><ChevronRight size={18} aria-hidden="true" /></span>
      </PanelLink>
    </>}
    {editor && <EmpresaForm empresa={editor.empresa} focusLogo={editor.focusLogo} onClose={() => setEditor(null)} onSaved={guardada => {
      onNombreGuardado(guardada.nombre)
      setSuccess(editor.empresa ? 'Datos de la empresa actualizados.' : 'Empresa registrada correctamente.')
      setEditor(null)
      reload()
    }} />}
  </div>
}
