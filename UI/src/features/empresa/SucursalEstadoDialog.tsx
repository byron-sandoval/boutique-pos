import { Button } from '@/components/ui/button'
import { empresaService } from './data'
import { useSave } from './hooks'
import { mensajeSucursalConVentas } from './model'
import type { Sucursal } from './model'
import { RecordDialog } from './RecordDialog'

export function SucursalEstadoDialog({ sucursal, onClose, onSaved }: { sucursal: Sucursal; onClose: () => void; onSaved: () => void }) {
  const { busy, error, save } = useSave()
  return <RecordDialog title={`${sucursal.activa ? 'Desactivar' : 'Activar'} sucursal`} description={sucursal.nombre} busy={busy} onClose={onClose}>
    <div className="business-state-body"><p>{sucursal.activa ? 'Los usuarios asociados a una sucursal inactiva no podrán operar. Sus datos se conservarán.' : 'La sucursal volverá a estar disponible para operar.'}</p>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button variant={sucursal.activa ? 'destructive' : 'default'} disabled={busy}
          onClick={() => void save(
            () => empresaService.estadoSucursal(sucursal.id, !sucursal.activa),
            onSaved,
            sucursal.activa ? mensajeSucursalConVentas : undefined,
          )}>
          {busy ? 'Guardando…' : sucursal.activa ? 'Desactivar sucursal' : 'Activar sucursal'}
        </Button>
      </div>
    </div>
  </RecordDialog>
}
