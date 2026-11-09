import { useEffect, useState } from 'react'
import { Save } from 'lucide-react'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { empresaRequest } from './model'
import type { Empresa } from './model'
import { empresaService } from './data'
import { useSave } from './hooks'
import { RecordDialog } from './RecordDialog'

const logoFormats = ['image/png', 'image/jpeg', 'image/webp']
const maxLogoBytes = 2 * 1024 * 1024

export function EmpresaForm({ empresa, onClose, onSaved }: { empresa: Empresa | null; onClose: () => void; onSaved: (empresa: Empresa) => void }) {
  const { busy, error, save } = useSave()
  const [logoPreview, setLogoPreview] = useState<string>()
  const [logoError, setLogoError] = useState('')

  useEffect(() => {
    if (logoPreview) return () => URL.revokeObjectURL(logoPreview)
  }, [logoPreview])

  return <RecordDialog title={empresa ? 'Editar empresa' : 'Registrar empresa'} description="Configura los datos del negocio. Los campos con * son obligatorios." busy={busy} onClose={onClose}>
    <form className="business-form" onSubmit={event => {
      event.preventDefault()
      const form = new FormData(event.currentTarget)
      const file = form.get('logo')
      const logo = file instanceof File && file.name ? file : undefined
      if (logoError) return
      void save(async () => {
        try {
          return await empresaService.guardarEmpresa(empresaRequest(form), empresa?.id, { logo })
        } catch (error) {
          if (logo && error instanceof ApiError && error.status === 503) {
            throw new Error('No se pudo subir el logo a Supabase. Revisa la configuración de Storage e inténtalo nuevamente.', { cause: error })
          }
          if (logo && error instanceof ApiError && error.status === 413) {
            throw new Error('El logo debe pesar como máximo 2 MB.', { cause: error })
          }
          throw error
        }
      }, onSaved, 'Ya existe una empresa con ese RUC. Verifica el número e intenta nuevamente.')
    }}>
      <fieldset disabled={busy}>
        <legend>Datos generales</legend>
        <div className="business-fields">
          <label>Nombre de la empresa *<input name="nombre" required maxLength={100} defaultValue={empresa?.nombre ?? ''} autoComplete="organization" autoFocus /></label>
          <label>RUC<input name="ruc" maxLength={20} defaultValue={empresa?.ruc ?? ''} /></label>
          <label className="business-field-wide">Dirección *<textarea name="direccion" required maxLength={255} rows={2} defaultValue={empresa?.direccion ?? ''} autoComplete="street-address" /></label>
          <label>Teléfono<input name="telefono" type="tel" maxLength={20} defaultValue={empresa?.telefono ?? ''} autoComplete="tel" /></label>
          <label>Correo electrónico<input name="correo" type="email" maxLength={100} defaultValue={empresa?.correo ?? ''} autoComplete="email" /></label>
        </div>
      </fieldset>
      <fieldset disabled={busy}>
        <legend>Información fiscal</legend>
        <div className="business-fields">
          <label>Régimen fiscal<input name="regimenFiscal" maxLength={100} defaultValue={empresa?.regimenFiscal ?? ''} /></label>
          <label>IVA (%) *<input name="porcentajeIva" type="number" required min="0" max="100" step="0.01" defaultValue={empresa?.porcentajeIva ?? 15} /><span className="business-help">Porcentaje que se aplica en las nuevas ventas.</span></label>
        </div>
      </fieldset>
      <fieldset disabled={busy}>
        <legend>Identidad del negocio</legend>
        <div className="business-fields">
          <label className="business-field-wide">Eslogan<input name="eslogan" maxLength={150} defaultValue={empresa?.eslogan ?? ''} /></label>
          <label className="business-field-wide">Subir logo
            <input name="logo" type="file" accept={logoFormats.join(',')} onChange={event => {
              const file = event.currentTarget.files?.[0]
              setLogoError('')
              if (file && (!logoFormats.includes(file.type) || file.size > maxLogoBytes)) {
                event.currentTarget.value = ''
                setLogoPreview(undefined)
                setLogoError('Elige una imagen PNG, JPG o WebP de hasta 2 MB.')
                return
              }
              setLogoPreview(file ? URL.createObjectURL(file) : undefined)
            }} />
            <span className="business-help">PNG, JPG o WebP de hasta 2 MB.</span>
          </label>
          {logoPreview && !logoError && <img className="business-logo-preview business-field-wide" src={logoPreview} alt="Vista previa del logo" onError={() => setLogoError('No se pudo abrir la imagen seleccionada.')} />}
          {!logoPreview && empresa?.logoUrl && <img className="business-logo-preview business-field-wide" src={empresa.logoUrl} alt="Logo actual" />}
          {logoError && <p className="notice error business-field-wide" role="alert">{logoError}</p>}
        </div>
      </fieldset>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions"><Button type="button" variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button><Button type="submit" disabled={busy || !!logoError}><Save size={15} />{busy ? 'Guardando…' : 'Guardar empresa'}</Button></div>
    </form>
  </RecordDialog>
}
