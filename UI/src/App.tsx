import { PanelLink } from '@/routes/PanelLink'
import { useState } from 'react'
import { logout } from './auth/session'
import type { Session } from './auth/session'
import { useSession } from './auth/use-session'
import { Categorias } from './features/categorias/Categorias'
import { HistorialComprasPage } from './features/compras/HistorialComprasPage'
import { RegistrarCompraPage } from './features/compras/RegistrarCompraPage'
import { EmpresaPage } from './features/empresa/EmpresaPage'
import { SucursalesPage } from './features/empresa/SucursalesPage'
import { MonedasPage } from './features/monedas/MonedasPage'
import { ProductosPage } from './features/productos/ProductosPage'
import { VariantesPage } from './features/productos/VariantesPage'
import { UnidadesMedidaPage } from './features/productos/UnidadesMedidaPage'
import { empresaService } from './features/empresa/data'
import { useRemote } from './features/empresa/hooks'
import { Button } from './components/ui/button'
import { PanelLayout } from './components/layout/PanelLayout'
import { ModulePlaceholder } from './components/layout/ModulePlaceholder'
import { DashboardPage } from './features/inicio/DashboardPage'
import { usePanelRoute } from './routes/use-panel-route'
import { findPanelPage } from './routes/navigation'
import './App.css'
import './theme.css'

function App() {
  const session = useSession()
  if (session.status === 'authenticated') return <PanelAutenticado key={session.username} session={session} />

  return <main className="welcome">
    {session.status === 'error'
      ? <section className="welcome-card">
          <p role="alert" className="notice error">No se pudo completar el acceso. Comprueba que Keycloak esté disponible.</p>
          <Button onClick={() => window.location.reload()}>Reintentar</Button>
        </section>
      : <p role="status">Redirigiendo al acceso…</p>}
  </main>
}

function PanelAutenticado({ session }: { session: Session }) {
  const [actionError, setActionError] = useState('')
  const [loggingOut, setLoggingOut] = useState(false)
  const [nombreGuardado, setNombreGuardado] = useState<string>()
  const { result: identidad } = useRemote(empresaService.nombre)
  const path = usePanelRoute()

  function signOut() {
    setActionError('')
    setLoggingOut(true)
    void logout().catch(() => {
      setLoggingOut(false)
      setActionError('No se pudo cerrar la sesión. Intenta nuevamente.')
    })
  }

  const nombreEmpresa = nombreGuardado ?? (identidad.status === 'ready' ? identidad.data.nombre : null)
  const page = findPanelPage(path, session.roles)
  return <PanelLayout name={session.username} nombreEmpresa={nombreEmpresa || 'Boutique POS'} roles={session.roles} path={path} onLogout={signOut} loggingOut={loggingOut}>
    {actionError && <p role="alert" className="notice error">{actionError}</p>}
    {!page ? <div className="module-placeholder"><h1>Esta página no está disponible</h1><p>Comprueba la dirección y los permisos de tu cuenta.</p><PanelLink className="text-link" href="/inicio">Volver al inicio</PanelLink></div>
      : path === '/inicio' ? <DashboardPage name={session.name} />
      : path === '/configuracion/empresa' ? <EmpresaPage onNombreGuardado={setNombreGuardado} />
      : path === '/configuracion/sucursales' ? <SucursalesPage />
      : path === '/configuracion/monedas' ? <MonedasPage />
      : path === '/productos' ? <ProductosPage puedeEditar={session.roles.includes('ADMIN')} />
      : path === '/productos/categorias' ? <Categorias puedeEditar={session.roles.includes('ADMIN')} />
      : path === '/productos/variantes' ? <VariantesPage puedeEditar={session.roles.includes('ADMIN')} />
      : path === '/productos/unidades' ? <UnidadesMedidaPage puedeEditar={session.roles.includes('ADMIN')} />
      : path === '/compras/nueva' ? <RegistrarCompraPage />
      : path === '/compras' ? <HistorialComprasPage />
      : <ModulePlaceholder title={page.label} />}
  </PanelLayout>
}

export default App
