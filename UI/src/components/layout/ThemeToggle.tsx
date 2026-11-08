import { useSyncExternalStore } from 'react'
import { Moon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { getTheme, subscribeTheme, toggleTheme } from '@/lib/theme'

export function ThemeToggle() {
  const dark = useSyncExternalStore(subscribeTheme, getTheme) === 'dark'

  return <Button variant="ghost" className="theme-toggle" role="switch" aria-checked={dark} aria-label="Modo oscuro" title={dark ? 'Desactivar modo oscuro' : 'Activar modo oscuro'} onClick={toggleTheme}>
    <Moon size={17} aria-hidden="true" />
    <span className="theme-label">Modo oscuro</span>
    <span className="theme-track" aria-hidden="true"><span /></span>
  </Button>
}
