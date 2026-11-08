import { useState } from 'react'
import { logout } from './auth/session'
import { useSession } from './auth/use-session'
import { Categorias } from './features/categorias/Categorias'
import './App.css'

function App() {
  const session = useSession()
  const [actionError, setActionError] = useState('')

  function run(action: () => Promise<void>) {
    setActionError('')
    void action().catch(() => setActionError('No se pudo completar la acción. Intenta nuevamente.'))
  }

  if (session.status !== 'authenticated') {
    return <main className="welcome">
      {session.status === 'error'
        ? <section className="welcome-card">
            <p role="alert" className="notice error">No se pudo completar el acceso. Comprueba que Keycloak esté disponible.</p>
            <button onClick={() => window.location.reload()}>Reintentar</button>
          </section>
        : <p role="status">Redirigiendo al acceso…</p>}
    </main>
  }

  const allowed = session.roles.length > 0
  return <div className="app-shell">
    <header className="app-header"><div><p className="eyebrow">Boutique POS</p><h1>Bienvenido, {session.name}</h1></div>
      <div className="session-actions"><span className="badge">{session.roles.join(' · ') || 'Sin rol asignado'}</span>
        <button className="secondary" onClick={() => run(logout)}>Cerrar sesión</button></div>
    </header>
    <main className="app-content">
      {actionError && <p role="alert" className="notice error">{actionError}</p>}
      {allowed ? <Categorias /> : <p className="notice error" role="alert">Tu cuenta necesita el rol ADMIN o CAJERO para acceder al sistema.</p>}
    </main>
  </div>
}

export default App
