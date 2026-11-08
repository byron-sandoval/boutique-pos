import { PanelLink } from '@/routes/PanelLink'
import { ArrowLeft, Layers3 } from 'lucide-react'
import { Card } from '@/components/ui/card'

export function ModulePlaceholder({ title }: { title: string }) {
  return <>
    <div className="page-heading"><div><p className="eyebrow">Tu espacio de trabajo</p><h1>{title}</h1><p>Todo en su lugar, para trabajar mejor.</p></div></div>
    <Card className="module-placeholder"><span className="placeholder-icon"><Layers3 size={28} strokeWidth={1.5} /></span><h2>Este espacio está tomando forma</h2><p>La pantalla de {title.toLocaleLowerCase('es')} se conectará en la siguiente etapa. Por ahora puedes explorar el diseño del panel.</p><PanelLink className="text-link" href="/inicio"><ArrowLeft size={16} /> Volver al inicio</PanelLink></Card>
  </>
}
