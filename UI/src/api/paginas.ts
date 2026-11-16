import type { Page } from '@/features/empresa/model'

export async function listarPaginas<T>(cargarPagina: (pagina: number) => Promise<Page<T>>, signal: AbortSignal) {
  const registros: T[] = []
  let totalPages = 1
  for (let pagina = 0; pagina < totalPages; pagina++) {
    signal.throwIfAborted()
    const respuesta = await cargarPagina(pagina)
    registros.push(...respuesta.content)
    totalPages = respuesta.page.totalPages
  }
  return registros
}
