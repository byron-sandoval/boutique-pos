import { PanelLink } from '@/routes/PanelLink'
import { useState } from 'react'
import { ArrowRight, ArrowUpRight, UsersRound, ReceiptText, ShoppingCart, Wallet } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { SalesChart } from './SalesChart'
import { TopProducts } from './TopProducts'
import { DateRangeSelector } from './DateRangeSelector'
import { dashboardSummary, formatMoney, recentSales } from './demo-data'

type Props = { name: string }

function Sparkline({ reverse = false }: { reverse?: boolean }) {
  return <svg className="sparkline" viewBox="0 0 86 32" aria-hidden="true"><polyline points={reverse ? '0,8 10,10 20,6 30,15 40,12 50,20 60,18 72,24 86,26' : '0,26 10,22 20,25 30,15 40,19 50,10 60,13 72,5 86,7'} fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>
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
      <Card className="metric-card metric-primary"><div className="metric-label"><span>Ventas del mes</span><Wallet size={18} aria-hidden="true" /></div><strong className="metric-value">{formatMoney(dashboardSummary.monthSales)}</strong><div className="metric-bottom"><Sparkline /></div></Card>
      <Card className="metric-card"><div className="metric-label"><span>Ventas del día</span><span className="metric-icon rose"><ShoppingCart size={17} /></span></div><strong className="metric-value">{formatMoney(dashboardSummary.daySales)}</strong><div className="metric-bottom"><Sparkline /></div></Card>
      <Card className="metric-card"><div className="metric-label"><span>Facturas del día</span><span className="metric-icon sage"><ReceiptText size={17} /></span></div><strong className="metric-value">{dashboardSummary.dayInvoiceCount}<small>facturas</small></strong><div className="metric-bottom"><Sparkline /></div></Card>
      <Card className="metric-card"><div className="metric-label"><span>Clientes activos</span><span className="metric-icon sand"><UsersRound size={17} aria-hidden="true" /></span></div><strong className="metric-value" aria-label="Total de clientes pendiente de consultar">—<small>clientes</small></strong><div className="metric-bottom"><PanelLink className="metric-stock-link" href="/clientes">Ver clientes <ArrowRight size={14} /></PanelLink></div></Card>
    </div>

    <div className="dashboard-main-grid">
      <Card className="revenue-card"><SalesChart /></Card>
      <TopProducts />
    </div>

    <div className="dashboard-bottom-grid">
      <Card className="recent-sales-card"><div className="card-heading"><div><h2>Ventas recientes</h2><p>Detalle de las últimas operaciones.</p></div><PanelLink href="/ventas" className="text-link">Ver todas <ArrowUpRight size={15} /></PanelLink></div>
        <div className="sales-tabs" aria-label="Filtrar ventas de ejemplo">{['Todas', 'Completada', 'Anulada'].map(value => <Button key={value} variant="ghost" className={filter === value ? 'tab-active' : ''} aria-pressed={filter === value} onClick={() => setFilter(value)}>{value === 'Todas' ? 'Todas las ventas' : value === 'Completada' ? 'Completadas' : 'Anuladas'}</Button>)}</div>
        <div className="table-scroll"><table className="sales-table"><thead><tr><th>Factura / Cliente</th><th>Fecha</th><th>Método</th><th>Estado</th><th className="text-right">Total</th></tr></thead><tbody>{sales.map(sale => <tr key={sale.invoice}><td><div className="customer-cell"><span className="customer-avatar">{sale.initials}</span><div><strong>{sale.invoice}</strong><span>{sale.name}</span></div></div></td><td className="nowrap">{sale.date}</td><td>{sale.method}</td><td><span className={`status-badge ${sale.status === 'Anulada' ? 'cancelled' : ''}`}><span />{sale.status}</span></td><td className="sale-total">{formatMoney(sale.total)}</td></tr>)}</tbody></table></div>
        <div className="table-footnote"><span>{sales.length} ventas</span><span>Moneda base: NIO</span></div>
      </Card>
      <div className="right-column"><Card className="category-card"><div className="card-heading"><div><h2>Ventas por categoría</h2><p>Una mirada a tu colección</p></div></div><div className="donut-layout"><div className="category-donut" role="img" aria-label="Distribución ilustrativa: ropa 58%, calzado 28%, accesorios 14%"><div><span>Total</span><strong>100<span>%</span></strong></div></div><div className="category-legend"><div><span><i className="legend-dot plum" />Ropa</span><strong>58%</strong></div><div><span><i className="legend-dot sage-dot" />Calzado</span><strong>28%</strong></div><div><span><i className="legend-dot sand-dot" />Accesorios</span><strong>14%</strong></div></div></div></Card>
      </div>
    </div>
  </>
}
