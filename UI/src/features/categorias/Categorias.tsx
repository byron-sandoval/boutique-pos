import { useEffect, useState } from 'react'
import { api } from '../../api'
import { Button } from '@/components/ui/button'

type Categoria = { id: number; nombre: string; descripcion: string | null; activo: boolean }
type Page<T> = { content: T[]; page: { number: number; totalPages: number; totalElements: number } }
type Result = { status: 'loading' } | { status: 'error'; message: string } | { status: 'ready'; data: Page<Categoria> }

export function Categorias() {
  const [page, setPage] = useState(0)
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState<Result>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    api<Page<Categoria>>(`categorias?page=${page}&size=10&sort=id,asc`, { signal: controller.signal })
      .then(data => { if (!controller.signal.aborted) setResult({ status: 'ready', data }) })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) setResult({ status: 'error', message: error instanceof Error ? error.message : 'No se pudieron cargar las categorías.' })
      })
    return () => controller.abort()
  }, [page, attempt])

  function load(nextPage = page) {
    setResult({ status: 'loading' })
    setPage(nextPage)
    setAttempt(value => value + 1)
  }

  return (
    <section className="panel" aria-labelledby="categorias-title">
      <div className="section-heading">
        <div><p className="eyebrow">Catálogo</p><h2 id="categorias-title">Categorías</h2></div>
        <Button variant="outline" onClick={() => load()} disabled={result.status === 'loading'}>Actualizar</Button>
      </div>
      {result.status === 'loading' && <p role="status">Cargando categorías…</p>}
      {result.status === 'error' && <p className="notice error" role="alert">{result.message}</p>}
      {result.status === 'ready' && (result.data.content.length === 0
        ? <p className="notice">Todavía no hay categorías en esta página.</p>
        : <div className="table-scroll"><table>
            <thead><tr><th>Nombre</th><th>Descripción</th><th>Estado</th></tr></thead>
            <tbody>{result.data.content.map(category => <tr key={category.id}>
              <td>{category.nombre}</td><td>{category.descripcion || '—'}</td>
              <td><span className={`badge ${category.activo ? '' : 'inactive'}`}>{category.activo ? 'Activa' : 'Inactiva'}</span></td>
            </tr>)}</tbody>
          </table></div>)}
      {result.status === 'ready' && <div className="pagination">
        <span>{result.data.page.totalElements} categorías · Página {page + 1} de {Math.max(1, result.data.page.totalPages)}</span>
        <div><Button variant="outline" disabled={page === 0} onClick={() => load(page - 1)}>Anterior</Button>
          <Button variant="outline" disabled={page + 1 >= result.data.page.totalPages} onClick={() => load(page + 1)}>Siguiente</Button></div>
      </div>}
    </section>
  )
}
