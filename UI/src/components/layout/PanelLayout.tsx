import { useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { LogOut, Menu } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Sidebar } from './Sidebar'

type Props = {
  name: string
  roles: string[]
  path: string
  onLogout: () => void
  loggingOut: boolean
  children: ReactNode
}

export function PanelLayout({ name, roles, path, onLogout, loggingOut, children }: Props) {
  const mobileMenu = useRef<HTMLDialogElement>(null)
  const [collapsed, setCollapsed] = useState(false)
  const initials = name.split(/\s+/).filter(Boolean).slice(0, 2).map(word => word[0]).join('').toUpperCase()
  const sidebarProps = { roles, path }

  return <div className={`panel-shell ${collapsed ? 'sidebar-collapsed' : ''}`}>
    <a className="skip-link" href="#main-content" onClick={event => { event.preventDefault(); document.getElementById('main-content')?.focus() }}>Saltar al contenido</a>
    <aside className="desktop-sidebar"><Sidebar {...sidebarProps} collapsed={collapsed} onToggle={() => setCollapsed(value => !value)} /></aside>
    <dialog ref={mobileMenu} className="mobile-sidebar" aria-label="Menú principal" onClick={event => { if (event.target === event.currentTarget) mobileMenu.current?.close() }}>
      <div className="mobile-sidebar-content">
        <Sidebar {...sidebarProps} onToggle={() => mobileMenu.current?.close()} toggleLabel="Cerrar menú" onNavigate={() => mobileMenu.current?.close()} />
      </div>
    </dialog>
    <div className="panel-body">
      <header className="topbar">
        <Button className="mobile-menu-button" variant="ghost" size="icon" aria-label="Abrir menú" onClick={() => mobileMenu.current?.showModal()}><Menu size={20} /></Button>
        <div className="topbar-user"><div className="user-avatar" aria-hidden="true">{initials || 'U'}</div><div className="user-copy"><strong>{name}</strong><span>{roles.includes('ADMIN') ? 'Administrador' : roles.includes('CAJERO') ? 'Cajero' : 'Sin rol'}</span></div>
          <Button variant="ghost" size="icon" className="user-logout" onClick={onLogout} disabled={loggingOut} aria-label={loggingOut ? 'Cerrando sesión…' : 'Cerrar sesión'} title={loggingOut ? 'Cerrando sesión…' : 'Cerrar sesión'}><LogOut size={18} aria-hidden="true" /></Button>
        </div>
      </header>
      <main id="main-content" className="panel-content" tabIndex={-1}>{children}</main>
    </div>
  </div>
}
