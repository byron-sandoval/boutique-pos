import { useId, useState } from 'react'
import { periods, formatMoney } from './demo-data'

export function SalesChart({ period }: { period: keyof typeof periods }) {
  const data = periods[period]
  const [hovered, setHovered] = useState<number | null>(null)
  const titleId = useId()
  const max = Math.ceil(Math.max(...data.values, ...data.previous) / 1000) * 1000
  const x = (index: number) => 64 + index * (520 / (data.values.length - 1))
  const y = (value: number) => 190 - value / max * 155
  const line = (values: number[]) => values.map((value, i) => `${x(i)},${y(value)}`).join(' ')
  const selected = hovered === null ? null : data.values[hovered]

  return <div className="sales-chart">
    <div className="chart-legend"><span><i className="legend-dot plum" />Este período</span><span><i className="legend-dot gray" />Período anterior</span><span className="chart-hover-value" aria-live="polite">{selected !== null ? `${data.labels[hovered!]} · ${formatMoney(selected)}` : 'Importes en córdobas'}</span></div>
    <svg viewBox="0 0 620 230" role="img" aria-labelledby={titleId}>
      <title id={titleId}>Ventas de ejemplo: {data.labels.map((label, i) => `${label}: ${formatMoney(data.values[i])}`).join(', ')}</title>
      {[0, 1, 2, 3].map(step => {
        const value = max * step / 3
        return <g key={step}><line x1="58" x2="596" y1={y(value)} y2={y(value)} stroke="#eae8ec" strokeDasharray="3 5" /><text x="43" y={y(value) + 4} textAnchor="end" className="chart-axis">{value === 0 ? '0' : `${(value / 1000).toFixed(0)} mil`}</text></g>
      })}
      <polygon points={`64,190 ${line(data.values)} 584,190`} fill="#f5edf2" />
      <polyline points={line(data.previous)} fill="none" stroke="#d4cdd6" strokeWidth="2" strokeDasharray="5 5" strokeLinejoin="round" />
      <polyline points={line(data.values)} fill="none" stroke="#865470" strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round" />
      {data.values.map((value, i) => <g key={i}>
        <text x={x(i)} y="219" textAnchor="middle" className="chart-axis">{data.labels[i]}</text>
        <circle cx={x(i)} cy={y(value)} r={hovered === i ? 6 : 4} fill="white" stroke="#865470" strokeWidth="2" />
        <circle cx={x(i)} cy={y(value)} r="14" fill="transparent" tabIndex={0} role="button" aria-label={`${data.labels[i]}: ${formatMoney(value)}`} onMouseEnter={() => setHovered(i)} onMouseLeave={() => setHovered(null)} onFocus={() => setHovered(i)} onBlur={() => setHovered(null)} onClick={() => setHovered(i)} onKeyDown={event => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); setHovered(i) } }} />
      </g>)}
    </svg>
  </div>
}
