import { PanelLink } from '@/routes/PanelLink'
import { useState } from 'react'
import { ArrowRight, ArrowUpRight, CalendarDays, ChevronDown, CreditCard, Footprints, Package, Shirt, ShoppingBag, ShoppingCart, Wallet } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { SalesChart } from './SalesChart'
import { featuredProducts, formatMoney, periods, recentSales } from './demo-data'

type Props = { name: string }

function Sparkline({ reverse = false }: { reverse?: boolean }) {
  return <svg className="sparkline" viewBox="0 0 86 32" aria-hidden="true"><polyline points={reverse ? '0,8 10,10 20,6 30,15 40,12 50,20 60,18 72,24 86,26' : '0,26 10,22 20,25 30,15 40,19 50,10 60,13 72,5 86,7'} fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>
}

export function DashboardPage({ name }: Props) {
  const [period, setPeriod] = useState<keyof typeof periods>('week')
  const [filter, setFilter] = useState('Todas')
  const data = periods[period]
  const previousSales = data.previous.reduce((sum, value) => sum + value, 0)
  const percentage = (current: number, previous: number) => `${current >= previous ? '+' : ''}${((current / previous - 1) * 100).toFixed(1)}%`
  const salesChange = percentage(data.sales, previousSales)
  const countChange = percentage(data.count, data.previousCount)
  const ticketChange = percentage(data.sales / data.count, previousSales / data.previousCount)
  const sales = recentSales.filter(sale => filter === 'Todas' || sale.status === filter)
  const firstName = name.split(' ')[0] || 'de nuevo'

  return <>
    <div className="page-heading"><div><p className="eyebrow">VISTA GENERAL</p><h1>Hola, {firstName}<span className="heading-dot">.</span></h1><p>Un vistazo a lo que mueve tu boutique.</p></div>
      <div className="heading-actions"><div className="period-select"><CalendarDays size={15} aria-hidden="true" /><select aria-label="Período del resumen" value={period} onChange={event => setPeriod(event.target.value as keyof typeof periods)}><option value="week">Últimos 7 días</option><option value="month">Últimos 30 días</option></select><ChevronDown size={13} aria-hidden="true" /></div></div>
    </div>

    <div className="metrics-grid" aria-label="Indicadores ilustrativos">
      <Card className="metric-card metric-primary"><div className="metric-label"><span>Ventas del período</span><Wallet size={18} aria-hidden="true" /></div><strong className="metric-value">{formatMoney(data.sales)}</strong><div className="metric-bottom"><span className="metric-change"><ArrowUpRight size={14} />{salesChange}<span> vs. período anterior</span></span><Sparkline /></div></Card>
      <Card className="metric-card"><div className="metric-label"><span>Ventas realizadas</span><span className="metric-icon rose"><ShoppingCart size={17} /></span></div><strong className="metric-value">{data.count}<small>ventas</small></strong><div className="metric-bottom"><span className="metric-change positive"><ArrowUpRight size={14} />{countChange}<span> vs. período anterior</span></span><Sparkline /></div></Card>
      <Card className="metric-card"><div className="metric-label"><span>Ticket promedio</span><span className="metric-icon sage"><CreditCard size={17} /></span></div><strong className="metric-value">{formatMoney(data.sales / data.count)}</strong><div className="metric-bottom"><span className="metric-change positive"><ArrowUpRight size={14} />{ticketChange}<span> vs. período anterior</span></span><Sparkline /></div></Card>
      <Card className="metric-card"><div className="metric-label"><span>Stock bajo</span><span className="metric-icon sand"><Package size={17} /></span></div><strong className="metric-value">7<small>productos</small></strong><div className="metric-bottom"><PanelLink className="metric-stock-link" href="/inventario">Revisar existencias <ArrowRight size={14} /></PanelLink><Sparkline reverse /></div></Card>
    </div>

    <div className="dashboard-main-grid">
      <Card className="revenue-card"><div className="card-heading"><div><h2>Comportamiento de ventas</h2><p>El ritmo de tu negocio, día a día.</p></div><span className="subtle-label">{data.label}</span></div><div className="revenue-summary"><strong>{formatMoney(data.sales)}</strong><span className="trend-pill"><ArrowUpRight size={13} /> {salesChange}</span></div><SalesChart key={period} period={period} /></Card>
      <Card className="bestsellers-card"><div className="card-heading"><div><h2>Los favoritos</h2><p>Productos más vendidos</p></div><span className="tiny-icon"><ShoppingBag size={18} /></span></div><div className="featured-products">{featuredProducts.map((product, index) => {
        const Icon = product.type === 'shirt' ? Shirt : product.type === 'shoe' ? Footprints : ShoppingBag
        return <div className="featured-product" key={product.name}><span className={`product-thumbnail ${product.color}`}><Icon size={25} strokeWidth={1.25} /></span><div className="product-info"><div><strong>{product.name}</strong><span className="product-rank">0{index + 1}</span></div><p>{product.detail}</p><div className="product-progress" aria-hidden="true"><span style={{ width: `${product.percent}%` }} /></div></div></div>
      })}</div><PanelLink href="/productos" className="card-footer-link">Explorar productos <ArrowRight size={15} /></PanelLink></Card>
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
