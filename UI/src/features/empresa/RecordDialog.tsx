import { useEffect, useId, useRef } from 'react'
import type { ReactNode } from 'react'
import { X } from 'lucide-react'
import { Button } from '@/components/ui/button'

type Props = { title: string; description: string; busy: boolean; onClose: () => void; children: ReactNode }

export function RecordDialog({ title, description, busy, onClose, children }: Props) {
  const dialog = useRef<HTMLDialogElement>(null)
  const id = useId()
  useEffect(() => {
    const element = dialog.current
    element?.showModal()
    return () => element?.close()
  }, [])

  return <dialog ref={dialog} className="business-dialog" aria-labelledby={`${id}-title`} aria-describedby={`${id}-description`} onCancel={event => {
    event.preventDefault()
    if (!busy) onClose()
  }}>
    <div className="business-dialog-heading"><div><h2 id={`${id}-title`}>{title}</h2><p id={`${id}-description`}>{description}</p></div>
      <Button variant="ghost" size="icon" aria-label="Cerrar formulario" disabled={busy} onClick={onClose}><X size={18} /></Button>
    </div>
    {children}
  </dialog>
}
