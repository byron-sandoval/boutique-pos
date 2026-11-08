import { useSyncExternalStore } from 'react'
import { subscribeNavigation } from './history'
import { panelPath } from './paths'

function getRoute() {
  return panelPath(window.location.pathname)
}

export function usePanelRoute() {
  return useSyncExternalStore(subscribeNavigation, getRoute)
}
