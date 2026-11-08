import { useId } from 'react'
import { Bell } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { PanelLink } from '@/routes/PanelLink'
import './stock-notifications.css'

export function StockNotifications() {
  const id = useId()

  return <div className="stock-notifications">
    <Button variant="ghost" size="icon" popoverTarget={id} aria-label="Notificaciones de stock" title="Notificaciones de stock">
      <Bell size={19} aria-hidden="true" />
    </Button>
    <div id={id} popover="auto" className="stock-notifications-panel" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Alertas de stock</h2>
      <div className="stock-notifications-empty">
        <Bell size={24} aria-hidden="true" />
        <p>Sin alertas para mostrar</p>
      </div>
      <PanelLink href="/inventario/alertas" onClick={() => document.getElementById(id)?.hidePopover()}>Ver inventario</PanelLink>
    </div>
  </div>
}
