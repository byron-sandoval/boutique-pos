import { PanelLink } from '@/routes/PanelLink'
import { useId, useState } from 'react'
import type { ReactNode } from 'react'
import { ArrowRight, ArrowUpRight, UsersRound, ReceiptText, ShoppingCart, Wallet } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { SalesChart } from './SalesChart'
import { TopProducts } from './TopProducts'
import { DateRangeSelector } from './DateRangeSelector'
import { dashboardSummary, formatMoney, recentSales } from './demo-data'

type Props = { name: string }

function SummaryCard({ title, description, icon: Icon, children, className = '', valueLabel, footer }: {
  title: string
  description: string
  icon: LucideIcon
  children: ReactNode
  className?: string
  valueLabel?: string
  footer?: ReactNode
}) {
  const fillId = useId()
  const curve = 'M 1 51 C 12 44 19 39 28 40 S 43 25 54 28 S 68 32 77 23 S 92 21 100 23 S 117 12 129 10'
  return <Card className={`metric-card ${className}`}>
    <div className="metric-heading">
      <span className="metric-icon"><Icon size={22} aria-hidden="true" /></span>
      <div className="metric-copy">
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
    </div>
    <strong className="metric-value" aria-label={valueLabel}>{children}</strong>
    {footer ?? <svg className="metric-trend" viewBox="0 0 138 56" aria-hidden="true">
      <defs>
        <linearGradient id={fillId} x2="0%" y2="100%">
          <stop stopColor="currentColor" stopOpacity=".26" />
          <stop offset="100%" stopColor="currentColor" stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={`${curve} L 129 56 H 1 Z`} fill={`url(#${fillId})`} />
      <path d={curve} fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <circle cx="129" cy="10" r="2.8" fill="var(--card)" stroke="currentColor" strokeWidth="1.6" />
    </svg>}
  </Card>
}

export function DashboardPage({ name }: Props) {
  const [filter, setFilter] = useState('Todas')
  const sales = recentSales.filter(sale => filter === 'Todas' || sale.status === filter)
  const firstName = name.split(' ')[0] || 'de nuevo'

  return <>
    <div className="page-heading"><div><h1 className="dashboard-greeting">Bienvenido, {firstName} <span role="img" aria-label="Saludo">👋</span></h1><p>Un vistazo a lo que mueve tu boutique.</p></div>
      <DateRangeSelector />
    </div>

    <div className="metrics-grid" aria-label="Indicadores ilustrativos">
      <SummaryCard title="Ventas del mes" description="Total de ventas del mes" icon={Wallet}>
        {formatMoney(dashboardSummary.monthSales)}
      </SummaryCard>
      <SummaryCard title="Ventas del día" description="Total de ventas realizadas hoy" icon={ShoppingCart}>
        {formatMoney(dashboardSummary.daySales)}
      </SummaryCard>
      <SummaryCard title="Facturas del día" description="Facturas emitidas hoy" icon={ReceiptText} className="metric-invoices">
        {dashboardSummary.dayInvoiceCount}<small>facturas</small>
      </SummaryCard>
      <SummaryCard title="Clientes activos" description="Clientes registrados y activos" icon={UsersRound} className="metric-clients"
        valueLabel="Total de clientes pendiente de consultar"
        footer={<PanelLink className="metric-stock-link" href="/clientes">Ver clientes <ArrowRight size={14} /></PanelLink>}>
        —<small>clientes</small>
      </SummaryCard>
    </div>

    <div className="dashboard-main-grid">
      <Card className="revenue-card"><SalesChart /></Card>
      <TopProducts />
    </div>

    <div className="dashboard-bottom-grid">
      <Card className="recent-sales-card"><div className="card-heading"><div><h2>Ventas recientes</h2><p>Detalle de las últimas operaciones.</p></div><PanelLink href="/ventas" className="text-link">Ver todas <ArrowUpRight size={15} /></PanelLink></div>
        <div className="sales-tabs" aria-label="Filtrar ventas de ejemplo">{['Todas', 'Completada', 'Anulada'].map(value => <Button key={value} variant="ghost" className={filter === value ? 'tab-active' : ''} aria-pressed={filter === value} onClick={() => setFilter(value)}>{value === 'Todas' ? 'Todas las ventas' : value === 'Completada' ? 'Completadas' : 'Anuladas'}</Button>)}</div>
        <div className="table-scroll"><table className="sales-table"><thead><tr><th>Factura / Cliente</th><th>Fecha</th><th>Método</th><th>Estado</th><th className="text-right">Total</th></tr></thead><tbody>{sales.map(sale => <tr key={sale.invoice}><td><div className="customer-cell"><span className="customer-avatar">{sale.initials}</span><div><strong>{sale.invoice}</strong><span>{sale.name}</span></div></div></td><td className="nowrap">{sale.date}</td><td>{sale.method}</td><td><span className={`status-badge ${sale.status === 'Anulada' ? 'cancelled' : ''}`}>{sale.status}</span></td><td className="sale-total">{formatMoney(sale.total)}</td></tr>)}</tbody></table></div>
        <div className="table-footnote"><span>{sales.length} ventas</span><span>Moneda base: NIO</span></div>
      </Card>
      <div className="right-column"><Card className="category-card"><div className="card-heading"><div><h2>Ventas por categoría</h2><p>Una mirada a tu colección</p></div></div><div className="donut-layout"><div className="category-donut" role="img" aria-label="Distribución ilustrativa: ropa 58%, calzado 28%, accesorios 14%"><div><span>Total</span><strong>100<span>%</span></strong></div></div><div className="category-legend"><div><span><i className="legend-dot plum" />Ropa</span><strong>58%</strong></div><div><span><i className="legend-dot sage-dot" />Calzado</span><strong>28%</strong></div><div><span><i className="legend-dot sand-dot" />Accesorios</span><strong>14%</strong></div></div></div></Card>
      </div>
    </div>
  </>
}
