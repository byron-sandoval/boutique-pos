import type { ComponentProps } from 'react'
import { navigate } from './history'

type Props = ComponentProps<'a'> & { href: string }

export function PanelLink({ href, onClick, ...props }: Props) {
  return <a {...props} href={href} onClick={event => {
    onClick?.(event)
    // Mantiene abrir en otra pestaña, descargar y los enlaces externos del navegador.
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey
      || (props.target && props.target !== '_self') || props.download !== undefined) return
    const url = new URL(href, window.location.href)
    if (url.origin !== window.location.origin || url.protocol !== window.location.protocol) return
    event.preventDefault()
    navigate(url)
  }} />
}
