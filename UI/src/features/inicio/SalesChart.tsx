import { useId, useState } from 'react'
import { Button } from '@/components/ui/button'
import { periods, formatMoney } from './demo-data'

export function SalesChart() {
  const [period, setPeriod] = useState<keyof typeof periods>('week')
  const data = periods[period]
  const [hovered, setHovered] = useState<number | null>(null)
  const titleId = useId()
  const highestSale = Math.max(0, ...data.values)
  const max = Math.max(1000, Math.ceil(highestSale / 1000) * 1000)
  const plotLeft = 58
  const plotRight = 596
  const baseline = 190
  const columnWidth = (plotRight - plotLeft) / data.values.length
  const barWidth = Math.min(56, columnWidth * 0.58)
  const x = (index: number) => plotLeft + (index + 0.5) * columnWidth
  const y = (value: number) => baseline - value / max * 155
  const selected = hovered === null ? null : {
    x: x(hovered),
    y: y(data.values[hovered]),
    amount: formatMoney(data.values[hovered]),
  }

  return <>
    <div className="card-heading chart-heading"><h2>Comportamiento de ventas</h2>
      <div className="chart-periods" role="group" aria-label="Período de la gráfica">
        {([['day', 'Día'], ['week', 'Semana'], ['month', 'Mes']] as const).map(([value, label]) => <Button key={value} variant="ghost" aria-pressed={period === value} onClick={() => { setHovered(null); setPeriod(value) }}>{label}</Button>)}
      </div>
    </div>
    <div className="sales-chart">
      <div className="sales-chart-plot">
        <svg viewBox="0 0 620 230" role="group" aria-labelledby={titleId}>
          <title id={titleId}>Ventas de ejemplo, {data.label}</title>
          <g aria-hidden="true">
            {[0, 1, 2, 3].map(step => {
              const value = max * step / 3
              const label = value >= 1000 ? `${(value / 1000).toLocaleString('es-NI', { maximumFractionDigits: 1 })} mil` : Math.round(value).toString()
              return <g key={step}>
                <line x1={plotLeft} x2={plotRight} y1={y(value)} y2={y(value)} stroke="var(--border)" strokeDasharray="3 5" />
                <text x="43" y={y(value) + 4} textAnchor="end" className="chart-axis">{label}</text>
              </g>
            })}
          </g>
          {data.values.map((value, i) => {
            const isPeak = value > 0 && value === highestSale
            const center = x(i)
            const top = y(value)
            const left = center - barWidth / 2
            const radius = Math.min(6, baseline - top)
            return <g key={data.labels[i]} className="chart-column" data-active={hovered === i} tabIndex={0}
              data-peak={isPeak} role="button"
              aria-label={`${data.labels[i]}: ${formatMoney(value)}${isPeak ? ', mayor venta del período' : ''}`}
              onPointerEnter={() => setHovered(i)}
              onPointerLeave={event => { if (event.pointerType === 'mouse') setHovered(null) }}
              onFocus={() => setHovered(i)} onBlur={() => setHovered(null)} onClick={() => setHovered(i)}
              onKeyDown={event => {
                if (event.key === 'Escape') setHovered(null)
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault()
                  setHovered(i)
                }
              }}>
              <path className="chart-bar" d={`M ${left} ${baseline} V ${top + radius} q 0 ${-radius} ${radius} ${-radius} h ${barWidth - radius * 2} q ${radius} 0 ${radius} ${radius} V ${baseline} Z`} />
              <rect x={center - columnWidth / 2} y="35" width={columnWidth} height="193" fill="transparent" />
              <text x={center} y="219" textAnchor="middle" className="chart-axis" aria-hidden="true">{data.labels[i]}</text>
            </g>
          })}
        </svg>
        {selected && <span className="chart-amount" aria-hidden="true" style={{
          left: `clamp(3.5rem, ${selected.x / 620 * 100}%, calc(100% - 3.5rem))`,
          top: `${selected.y / 230 * 100}%`,
        }}>{selected.amount}</span>}
      </div>
    </div>
  </>
}
