import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { initializeSession } from './auth/session'
import { initializeTheme } from './lib/theme'
import { initializeNavigation } from './routes/history'

initializeNavigation()
initializeTheme()
void initializeSession()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
