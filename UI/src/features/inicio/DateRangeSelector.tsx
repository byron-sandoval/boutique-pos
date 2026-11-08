import { useId, useRef, useState } from 'react'
import { CalendarDays, ChevronDown } from 'lucide-react'
import { Button } from '@/components/ui/button'
import './date-range-selector.css'

function localDate(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const dateFormatter = new Intl.DateTimeFormat('es-NI', { day: 'numeric', month: 'short', year: 'numeric' })
function displayDate(value: string) {
  return dateFormatter.format(new Date(`${value}T12:00:00`))
}

export function DateRangeSelector() {
  const [range, setRange] = useState(() => {
    const end = new Date()
    const start = new Date(end)
    start.setDate(start.getDate() - 6)
    return { start: localDate(start), end: localDate(end) }
  })
  const [draft, setDraft] = useState(range)
  const details = useRef<HTMLDetailsElement>(null)
  const id = useId()

  return <details className="dashboard-date-range" ref={details} onKeyDown={event => {
    if (event.key === 'Escape') {
      details.current?.removeAttribute('open')
      details.current?.querySelector('summary')?.focus()
    }
  }}>
    <summary aria-label={`Seleccionar fechas: ${displayDate(range.start)} a ${displayDate(range.end)}`}>
      <CalendarDays size={15} aria-hidden="true" />
      <span>{displayDate(range.start)} – {displayDate(range.end)}</span>
      <ChevronDown size={14} className="date-range-chevron" aria-hidden="true" />
    </summary>
    <form className="date-range-options" onSubmit={event => {
      event.preventDefault()
      setRange(draft)
      details.current?.removeAttribute('open')
      details.current?.querySelector('summary')?.focus()
    }}>
      <label htmlFor={`${id}-start`}>Desde</label>
      <input id={`${id}-start`} type="date" required value={draft.start} max={draft.end} onChange={event => setDraft({ ...draft, start: event.target.value })} />
      <label htmlFor={`${id}-end`}>Hasta</label>
      <input id={`${id}-end`} type="date" required value={draft.end} min={draft.start} onChange={event => setDraft({ ...draft, end: event.target.value })} />
      <Button type="submit">Aplicar</Button>
    </form>
  </details>
}
