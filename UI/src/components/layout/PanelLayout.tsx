import { useId, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { LogOut, Menu } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Sidebar } from './Sidebar'
import { StockNotifications } from './StockNotifications'
import './logout-confirmation.css'

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
  const logoutDialog = useRef<HTMLDialogElement>(null)
  const logoutTitleId = useId()
  const logoutDescriptionId = useId()
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
        <div className="topbar-user"><StockNotifications /><div className="user-avatar" aria-hidden="true">{initials || 'U'}</div><div className="user-copy"><strong>{name}</strong><span>{roles.includes('ADMIN') ? 'Administrador' : roles.includes('CAJERO') ? 'Cajero' : 'Sin rol'}</span></div>
          <Button variant="ghost" size="icon" className="user-logout" onClick={() => logoutDialog.current?.showModal()} disabled={loggingOut} aria-haspopup="dialog" aria-label={loggingOut ? 'Cerrando sesión…' : 'Cerrar sesión'} title={loggingOut ? 'Cerrando sesión…' : 'Cerrar sesión'}><LogOut size={18} aria-hidden="true" /></Button>
        </div>
      </header>
      <main id="main-content" className="panel-content" tabIndex={-1}>{children}</main>
    </div>
    <dialog ref={logoutDialog} className="logout-confirmation" aria-labelledby={logoutTitleId} aria-describedby={logoutDescriptionId}>
      <h2 id={logoutTitleId}>¿Quieres cerrar sesión?</h2>
      <p id={logoutDescriptionId}>Tendrás que iniciar sesión de nuevo para acceder al sistema.</p>
      <div className="logout-confirmation-actions">
        <Button variant="outline" autoFocus onClick={() => logoutDialog.current?.close()}>Cancelar</Button>
        <Button disabled={loggingOut} onClick={() => {
          if (loggingOut) return
          logoutDialog.current?.close()
          onLogout()
        }}><LogOut size={16} aria-hidden="true" />Cerrar sesión</Button>
      </div>
    </dialog>
  </div>
}
