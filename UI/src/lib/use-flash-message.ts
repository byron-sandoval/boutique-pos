import { useCallback, useEffect, useRef, useState } from 'react'

export function useFlashMessage(duration = 4000) {
  const [message, setMessage] = useState('')
  const timer = useRef<number | undefined>(undefined)

  const showMessage = useCallback((text: string) => {
    window.clearTimeout(timer.current)
    setMessage(text)
    if (text) timer.current = window.setTimeout(() => setMessage(''), duration)
  }, [duration])

  useEffect(() => () => window.clearTimeout(timer.current), [])

  return [message, showMessage] as const
}
