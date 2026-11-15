import { Button } from '@/components/ui/button'

export function PaginacionCatalogo({ total, pagina, totalPaginas, registros, onChange }: {
  total: number
  pagina: number
  totalPaginas: number
  registros: string
  onChange: (pagina: number) => void
}) {
  if (totalPaginas <= 1) return null

  return <div className="pagination">
    <span>{total} {registros} · Página {pagina + 1} de {totalPaginas}</span>
    <div>
      <Button variant="outline" disabled={pagina === 0} onClick={() => onChange(pagina - 1)}>Anterior</Button>
      <Button variant="outline" disabled={pagina + 1 >= totalPaginas} onClick={() => onChange(pagina + 1)}>Siguiente</Button>
    </div>
  </div>
}
