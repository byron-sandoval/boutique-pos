import { useEffect, useState } from 'react'
import { CheckCircle2, Layers3, Pencil, Plus, Power, Save, Text } from 'lucide-react'
import { api } from '../../api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { RecordDialog } from '@/features/empresa/RecordDialog'
import { useSave } from '@/features/empresa/hooks'
import type { Page } from '@/features/empresa/model'
import { useFlashMessage } from '@/lib/use-flash-message'
import '@/features/empresa/empresa.css'

type Categoria = { id: number; nombre: string; descripcion: string | null; activo: boolean }
type Result = { status: 'loading' } | { status: 'error'; message: string } | { status: 'ready'; data: Page<Categoria> }
type Editor = { tipo: 'formulario'; categoria: Categoria | null } | { tipo: 'estado'; categoria: Categoria }

const categoriasPorPagina = 12

export function Categorias({ puedeEditar }: { puedeEditar: boolean }) {
  const [page, setPage] = useState(0)
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState<Result>({ status: 'loading' })
  const [editor, setEditor] = useState<Editor | null>(null)
  const [success, setSuccess] = useFlashMessage()

  useEffect(() => {
    const controller = new AbortController()
    api<Page<Categoria>>(`categorias?page=${page}&size=${categoriasPorPagina}&sort=activo,desc&sort=id,desc`, { signal: controller.signal })
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

  function abrirEditor(siguiente: Editor) {
    setSuccess('')
    setEditor(siguiente)
  }

  function guardado(mensaje: string, pagina = page) {
    setSuccess(mensaje)
    setEditor(null)
    load(pagina)
  }

  return (
    <section className="business-page" aria-labelledby="categorias-title">
      <div className="page-heading business-page-heading">
        <div><h1 id="categorias-title">Categorías</h1><p>Organiza las colecciones de tu boutique.</p></div>
        {puedeEditar && <Button onClick={() => abrirEditor({ tipo: 'formulario', categoria: null })}>
          <Plus size={16} aria-hidden="true" />Nueva categoría
        </Button>}
      </div>
      {success && <p className="business-success" role="status">{success}</p>}
      {result.status === 'loading' && <p role="status">Cargando categorías…</p>}
      {result.status === 'error' && <div>
        <p className="notice error" role="alert">{result.message}</p>
        <Button variant="outline" onClick={() => load()}>Reintentar</Button>
      </div>}
      {result.status === 'ready' && (result.data.content.length === 0
        ? <p className="notice">Todavía no hay categorías en esta página.</p>
        : <div className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,18rem),1fr))] gap-4">
          {result.data.content.map(category => <Card className="business-detail-card business-fiscal-card flex min-h-40 flex-col p-5!" key={category.id}>
            <div className="business-section-heading">
              <span className="business-symbol"><Layers3 size={20} aria-hidden="true" /></span>
              <h3 className="text-sm font-semibold wrap-anywhere">{category.nombre}</h3>
            </div>
            <dl className="business-details">
              <div>
                <dt><span><Text size={14} aria-hidden="true" /></span>Descripción</dt>
                <dd>{category.descripcion || 'Sin descripción'}</dd>
              </div>
              <div>
                <dt><span><CheckCircle2 size={14} aria-hidden="true" /></span>Estado</dt>
                <dd><span className={`business-status ${category.activo ? '' : 'is-inactive'}`}>{category.activo ? 'Activa' : 'Inactiva'}</span></dd>
              </div>
            </dl>
            {puedeEditar && <div className="business-table-actions mt-auto justify-end! pt-3">
              <Button variant="outline" size="icon-sm" title="Editar categoría" aria-label={`Editar categoría ${category.nombre}`}
                onClick={() => abrirEditor({ tipo: 'formulario', categoria: category })}>
                <Pencil size={14} aria-hidden="true" />
              </Button>
              <Button variant="outline" size="icon-sm" data-table-action={category.activo ? 'deactivate' : 'activate'}
                title={category.activo ? 'Desactivar categoría' : 'Activar categoría'}
                aria-label={`${category.activo ? 'Desactivar' : 'Activar'} categoría ${category.nombre}`}
                onClick={() => abrirEditor({ tipo: 'estado', categoria: category })}>
                <Power size={14} aria-hidden="true" />
              </Button>
            </div>}
          </Card>)}
        </div>)}
      {result.status === 'ready' && result.data.page.totalPages > 1 && <div className="pagination">
        <span>{result.data.page.totalElements} categorías · Página {page + 1} de {result.data.page.totalPages}</span>
        <div><Button variant="outline" disabled={page === 0} onClick={() => load(page - 1)}>Anterior</Button>
          <Button variant="outline" disabled={page + 1 >= result.data.page.totalPages} onClick={() => load(page + 1)}>Siguiente</Button></div>
      </div>}
      {puedeEditar && editor?.tipo === 'formulario' && <CategoriaForm categoria={editor.categoria}
        onClose={() => setEditor(null)}
        onSaved={() => editor.categoria ? guardado('Categoría actualizada.') : guardado('Categoría registrada.', 0)} />}
      {puedeEditar && editor?.tipo === 'estado' && <CategoriaEstadoDialog categoria={editor.categoria}
        onClose={() => setEditor(null)}
        onSaved={() => guardado(editor.categoria.activo ? 'Categoría desactivada.' : 'Categoría activada.')} />}
    </section>
  )
}

type DialogActions = { onClose: () => void; onSaved: () => void }

function CategoriaForm({ categoria, onClose, onSaved }: DialogActions & { categoria: Categoria | null }) {
  const { busy, error, save } = useSave()
  return <RecordDialog title={categoria ? 'Editar categoría' : 'Nueva categoría'}
    className="max-w-110"
    description="Define el nombre y la descripción de la categoría." busy={busy} onClose={onClose}>
    <form className="business-form" onSubmit={event => {
      event.preventDefault()
      const form = new FormData(event.currentTarget)
      const nombre = String(form.get('nombre') ?? '').trim()
      const descripcion = String(form.get('descripcion') ?? '').trim() || null
      void save(() => {
        if (!nombre) throw new Error('Escribe un nombre válido.')
        return api<Categoria>(categoria ? `categorias/${categoria.id}` : 'categorias', {
          method: categoria ? 'PUT' : 'POST',
          body: JSON.stringify({ nombre, descripcion, activo: categoria?.activo ?? true }),
          readConflictDetail: true,
        })
      }, onSaved)
    }}>
      <fieldset disabled={busy}>
        <div className="business-fields">
          <label className="business-field-wide">Nombre
            <input name="nombre" required maxLength={50} defaultValue={categoria?.nombre} autoFocus />
          </label>
          <label className="business-field-wide">Descripción
            <textarea name="descripcion" maxLength={255} rows={categoria ? 3 : 2} defaultValue={categoria?.descripcion ?? ''} />
          </label>
        </div>
      </fieldset>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button type="button" variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button type="submit" disabled={busy}><Save size={15} aria-hidden="true" />{busy ? 'Guardando…' : 'Guardar categoría'}</Button>
      </div>
    </form>
  </RecordDialog>
}

function CategoriaEstadoDialog({ categoria, onClose, onSaved }: DialogActions & { categoria: Categoria }) {
  const { busy, error, save } = useSave()
  const titulo = categoria.activo ? 'Desactivar categoría' : 'Activar categoría'
  return <RecordDialog title={titulo} description={categoria.nombre} busy={busy} onClose={onClose}>
    <div className="business-state-body">
      <p>La categoría quedará marcada como {categoria.activo ? 'inactiva' : 'activa'} en el catálogo.</p>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button variant={categoria.activo ? 'destructive' : 'default'} disabled={busy}
          onClick={() => void save(() => api(`categorias/${categoria.id}/estado`, {
            method: 'PATCH',
            body: JSON.stringify({ activo: !categoria.activo }),
            readConflictDetail: true,
          }), onSaved)}>
          {busy ? 'Guardando…' : titulo}
        </Button>
      </div>
    </div>
  </RecordDialog>
}
