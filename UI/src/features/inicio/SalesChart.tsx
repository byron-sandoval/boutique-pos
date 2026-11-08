import { useId, useState } from 'react'
import { Button } from '@/components/ui/button'
import { periods, formatMoney } from './demo-data'

export function SalesChart() {
  const [period, setPeriod] = useState<keyof typeof periods>('week')
  const data = periods[period]
  const [hovered, setHovered] = useState<number | null>(null)
  const titleId = useId()
  const gradientId = useId()
  const max = Math.max(1000, Math.ceil(Math.max(...data.values) / 1000) * 1000)
  const x = (index: number) => 64 + index * (520 / (data.values.length - 1))
  const y = (value: number) => 190 - value / max * 155
  const points = data.values.map((value, index) => ({ x: x(index), y: y(value) }))
  // Controles dentro de cada tramo: curvas suaves sin exceder los valores reales.
  const line = points.map((point, index) => {
    if (index === 0) return `M ${point.x},${point.y}`
    const previous = points[index - 1]
    const middle = (previous.x + point.x) / 2
    return `C ${middle},${previous.y} ${middle},${point.y} ${point.x},${point.y}`
  }).join(' ')
  const area = `${line} L ${points[points.length - 1].x},190 L ${points[0].x},190 Z`
  const selected = hovered === null ? null : data.values[hovered]

  return <>
    <div className="card-heading chart-heading"><h2>Comportamiento de ventas</h2>
      <div className="chart-periods" role="group" aria-label="Período de la gráfica">
        {([['day', 'Día'], ['week', 'Semana'], ['month', 'Mes']] as const).map(([value, label]) => <Button key={value} variant="ghost" aria-pressed={period === value} onClick={() => { setHovered(null); setPeriod(value) }}>{label}</Button>)}
      </div>
    </div>
    <div className="sales-chart">
    <p className="sr-only" aria-live="polite">{selected !== null ? `${data.labels[hovered!]}: ${formatMoney(selected)}` : ''}</p>
    <svg viewBox="0 0 620 230" role="img" aria-labelledby={titleId}>
      <title id={titleId}>Ventas de ejemplo, {data.label}: {data.labels.map((label, i) => `${label}: ${formatMoney(data.values[i])}`).join(', ')}</title>
      <defs><linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stopColor="var(--sales-line)" stopOpacity="0.18" />
        <stop offset="100%" stopColor="var(--sales-line)" stopOpacity="0.01" />
      </linearGradient></defs>
      {[0, 1, 2, 3].map(step => {
        const value = max * step / 3
        const label = value >= 1000 ? `${(value / 1000).toLocaleString('es-NI', { maximumFractionDigits: 1 })} mil` : Math.round(value).toString()
        return <g key={step}><line x1="58" x2="596" y1={y(value)} y2={y(value)} stroke="#eae8ec" strokeDasharray="3 5" /><text x="43" y={y(value) + 4} textAnchor="end" className="chart-axis">{label}</text></g>
      })}
      <path d={area} fill={`url(#${gradientId})`} />
      <path d={line} fill="none" stroke="var(--sales-line)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      {data.values.map((value, i) => <g key={i}>
        <text x={x(i)} y="219" textAnchor="middle" className="chart-axis">{data.labels[i]}</text>
        {hovered === i && <circle cx={x(i)} cy={y(value)} r="5" fill="var(--sales-surface)" stroke="var(--sales-line)" strokeWidth="2" />}
        <circle cx={x(i)} cy={y(value)} r="14" fill="transparent" tabIndex={0} role="button" aria-label={`${data.labels[i]}: ${formatMoney(value)}`} onMouseEnter={() => setHovered(i)} onMouseLeave={() => setHovered(null)} onFocus={() => setHovered(i)} onBlur={() => setHovered(null)} onClick={() => setHovered(i)} onKeyDown={event => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); setHovered(i) } }} />
      </g>)}
    </svg>
    </div>
  </>
}
