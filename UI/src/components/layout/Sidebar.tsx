import { PanelLink } from '@/routes/PanelLink'
import { useId, useState } from 'react'
import { Banknote, Boxes, ChevronDown, House, Menu, Package, Settings2, ShoppingBag, ShoppingCart, Truck, UserRound, UsersRound } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { visibleNavigation } from '@/routes/navigation'
import type { NavItem } from '@/routes/navigation'
import { ThemeToggle } from './ThemeToggle'

const icons = { inicio: House, ventas: ShoppingBag, caja: Banknote, productos: Package, compras: ShoppingCart, inventario: Boxes, clientes: UserRound, proveedores: Truck, usuarios: UsersRound, configuracion: Settings2 }

type SidebarProps = {
  roles: string[]
  path: string
  onNavigate?: () => void
  collapsed?: boolean
  onToggle: () => void
  toggleLabel?: string
}

function NavigationItem({ item, path, onNavigate, collapsed, onExpandSidebar }: { item: NavItem; path: string; onNavigate?: () => void; collapsed: boolean; onExpandSidebar: () => void }) {
  const active = item.links.some(link => link.path === path)
  const [expanded, setExpanded] = useState(active)
  const id = useId()
  const Icon = icons[item.icon]

  if (item.links.length === 1) {
    const link = item.links[0]
    return <PanelLink className={`nav-link ${active ? 'is-active' : ''}`} href={link.path} aria-label={link.label} title={collapsed ? link.label : undefined} aria-current={active ? 'page' : undefined} onClick={onNavigate}>
      <Icon size={18} aria-hidden="true" /><span>{link.label}</span>
    </PanelLink>
  }

  return <div className="nav-group">
    <button className={`nav-link ${active ? 'group-active' : ''}`} aria-label={item.label} title={collapsed ? item.label : undefined} aria-expanded={!collapsed && expanded} aria-controls={id} onClick={() => {
      if (collapsed) { setExpanded(true); onExpandSidebar() }
      else setExpanded(!expanded)
    }}>
      <Icon size={18} aria-hidden="true" /><span>{item.label}</span><ChevronDown size={14} className={`nav-chevron ${expanded ? 'is-rotated' : ''}`} aria-hidden="true" />
    </button>
    <div id={id} hidden={collapsed || !expanded} className="nav-children">{item.links.map(link => <PanelLink key={link.path} href={link.path} className={link.path === path ? 'is-active' : ''} aria-current={link.path === path ? 'page' : undefined} onClick={onNavigate}>{link.label}</PanelLink>)}</div>
  </div>
}

export function Sidebar({ roles, path, onNavigate, collapsed = false, onToggle, toggleLabel }: SidebarProps) {
  const navId = useId()
  return <div className="sidebar-inner">
    <div className="brand">
      <Button variant="ghost" size="icon" className="brand-toggle" onClick={onToggle} aria-label={toggleLabel || (collapsed ? 'Expandir menú lateral' : 'Contraer menú lateral')} aria-expanded={!collapsed} aria-controls={navId} title={toggleLabel || (collapsed ? 'Expandir menú lateral' : 'Contraer menú lateral')}><Menu size={21} aria-hidden="true" /></Button>
      <PanelLink className="brand-name" href="/inicio" onClick={onNavigate} aria-label="Boutique POS, ir al inicio">Boutique<span className="brand-pos">POS</span></PanelLink>
    </div>
    <p className="nav-section-label">PRINCIPAL</p>
    <nav id={navId} aria-label="Navegación principal">{visibleNavigation(roles).map(item => <NavigationItem key={item.icon} item={item} path={path} onNavigate={onNavigate} collapsed={collapsed} onExpandSidebar={onToggle} />)}</nav>
    <div className="sidebar-bottom">
      <ThemeToggle />
      <p className="sidebar-footnote">BOUTIQUE POS</p>
    </div>
  </div>
}
