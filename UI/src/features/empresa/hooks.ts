import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from '@/api/client'

type Result<T> = { status: 'loading' } | { status: 'error'; message: string } | { status: 'ready'; data: T }

export function useRemote<T>(load: (signal: AbortSignal) => Promise<T>) {
  const [version, setVersion] = useState(0)
  const [snapshot, setSnapshot] = useState<{ load: typeof load; version: number; result: Result<T> }>()
  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal).then(data => {
      if (!controller.signal.aborted) setSnapshot({ load, version, result: { status: 'ready', data } })
    }).catch((error: unknown) => {
      if (!controller.signal.aborted) setSnapshot({ load, version, result: { status: 'error', message: error instanceof Error ? error.message : 'No se pudieron cargar los datos.' } })
    })
    return () => controller.abort()
  }, [load, version])
  const reload = useCallback(() => setVersion(value => value + 1), [])
  const result: Result<T> = snapshot?.load === load && snapshot.version === version ? snapshot.result : { status: 'loading' }
  return { result, reload }
}

export function useSave() {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const pending = useRef(false)
  async function save<T>(operation: () => Promise<T>, onSuccess: (result: T) => void, conflictMessage?: string) {
    if (pending.current) return
    pending.current = true
    setBusy(true)
    setError('')
    try {
      const result = await operation()
      onSuccess(result)
    } catch (error) {
      let message = error instanceof Error ? error.message : 'No se pudo guardar la información.'
      if (error instanceof ApiError) {
        if (error.status === 409 && conflictMessage) message = conflictMessage
        if (error.status === 400) message = 'Revisa los campos: el servidor rechazó alguno de los valores.'
        if (error.status === 404) message = 'El registro ya no está disponible. Actualiza la lista.'
        if (error.status === 0 || error.status >= 500) message = 'No se pudo confirmar la operación. Actualiza la lista antes de volver a guardar.'
      }
      setError(message)
    } finally {
      pending.current = false
      setBusy(false)
    }
  }
  return { busy, error, save }
}
