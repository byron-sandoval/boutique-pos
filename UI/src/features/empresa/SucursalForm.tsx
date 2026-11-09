import { Save } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { sucursalRequest } from './model'
import type { Empresa, Sucursal } from './model'
import { empresaService } from './data'
import { useSave } from './hooks'
import { RecordDialog } from './RecordDialog'

type Props = { sucursal: Sucursal | null; empresa: Empresa; onClose: () => void; onSaved: () => void }

export function SucursalForm({ sucursal, empresa, onClose, onSaved }: Props) {
  const { busy, error, save } = useSave()
  return <RecordDialog title={sucursal ? 'Editar sucursal' : 'Nueva sucursal'} description={`Sucursal de ${empresa.nombre}. Los campos con * son obligatorios.`} busy={busy} onClose={onClose}>
    <form className="business-form" onSubmit={event => {
      event.preventDefault()
      const form = new FormData(event.currentTarget)
      form.set('empresaId', String(empresa.id))
      void save(() => empresaService.guardarSucursal(sucursalRequest(form), sucursal?.id), onSaved)
    }}>
      <fieldset disabled={busy}>
        <legend>Datos de la sucursal</legend>
        <div className="business-fields">
          <label>Nombre *<input name="nombre" required maxLength={100} defaultValue={sucursal?.nombre ?? ''} autoFocus /></label>
          <label>Teléfono<input name="telefono" type="tel" maxLength={20} defaultValue={sucursal?.telefono ?? ''} autoComplete="tel" /></label>
          <label className="business-field-wide">Dirección *<textarea name="direccion" required maxLength={255} rows={3} defaultValue={sucursal?.direccion ?? ''} autoComplete="street-address" /></label>
          <label className="business-checkbox business-field-wide"><input name="activa" type="checkbox" defaultChecked={sucursal?.activa ?? true} /><span>Sucursal activa</span></label>
        </div>
      </fieldset>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions"><Button type="button" variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button><Button type="submit" disabled={busy}><Save size={15} />{busy ? 'Guardando…' : 'Guardar sucursal'}</Button></div>
    </form>
  </RecordDialog>
}
