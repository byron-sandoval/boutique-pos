import { ArrowRight, ShoppingBag } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { PanelLink } from '@/routes/PanelLink'
import { featuredProducts, formatMoney } from './demo-data'
import './top-products.css'

export function TopProducts() {
  return <Card className="bestsellers-card">
    <div className="card-heading"><div><h2>Productos más vendidos</h2><p>Top 3 del mes</p></div><ShoppingBag className="tiny-icon" size={17} aria-hidden="true" /></div>
    <ol className="top-products-grid" aria-label="Productos más vendidos del mes">
      {featuredProducts.map((product, index) => <li key={product.name}>
        <article className="top-product-card">
          <div className="top-product-media">
            <span className={`top-product-rank rank-${index + 1}`} aria-label={`Puesto ${index + 1}`}>{index + 1}</span>
          </div>
          <div className="top-product-details"><h3>{product.name}</h3><p>{product.unitsSold} unidades</p></div>
          <p className="top-product-amount"><span className="sr-only">Monto vendido: </span>{formatMoney(product.salesAmount)}</p>
        </article>
      </li>)}
    </ol>
    <PanelLink href="/productos" className="card-footer-link">Explorar productos <ArrowRight size={15} /></PanelLink>
  </Card>
}
