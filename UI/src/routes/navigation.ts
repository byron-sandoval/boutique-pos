export type PanelRole = 'ADMIN' | 'CAJERO'
export type NavLink = { label: string; path: string; roles: PanelRole[] }
export type NavItem = {
  label: string
  icon: 'inicio' | 'ventas' | 'caja' | 'productos' | 'compras' | 'inventario' | 'clientes' | 'proveedores' | 'usuarios' | 'configuracion'
  links: NavLink[]
}

const both: PanelRole[] = ['ADMIN', 'CAJERO']
const admin: PanelRole[] = ['ADMIN']

export const navigation: NavItem[] = [
  { label: 'Inicio', icon: 'inicio', links: [{ label: 'Inicio', path: '/inicio', roles: both }] },
  { label: 'Ventas', icon: 'ventas', links: [
    { label: 'Nueva venta', path: '/ventas/nueva', roles: both },
    { label: 'Historial de ventas', path: '/ventas', roles: both },
  ] },
  { label: 'Caja', icon: 'caja', links: [{ label: 'Caja', path: '/caja', roles: both }] },
  { label: 'Productos', icon: 'productos', links: [
    { label: 'Todos los productos', path: '/productos', roles: both },
    { label: 'Categorías', path: '/productos/categorias', roles: both },
    { label: 'Variantes', path: '/productos/variantes', roles: both },
    { label: 'Unidades de medida', path: '/productos/unidades', roles: both },
  ] },
  { label: 'Compras', icon: 'compras', links: [
    { label: 'Registrar compra', path: '/compras/nueva', roles: admin },
    { label: 'Historial de compras', path: '/compras', roles: admin },
  ] },
  { label: 'Inventario', icon: 'inventario', links: [
    { label: 'Existencias', path: '/inventario', roles: both },
    { label: 'Alertas de stock bajo', path: '/inventario/alertas', roles: both },
  ] },
  { label: 'Clientes', icon: 'clientes', links: [{ label: 'Clientes', path: '/clientes', roles: both }] },
  { label: 'Proveedores', icon: 'proveedores', links: [{ label: 'Proveedores', path: '/proveedores', roles: admin }] },
  { label: 'Usuarios', icon: 'usuarios', links: [{ label: 'Usuarios', path: '/usuarios', roles: admin }] },
  { label: 'Configuración', icon: 'configuracion', links: [
    { label: 'Empresa', path: '/configuracion/empresa', roles: admin },
    { label: 'Sucursales', path: '/configuracion/sucursales', roles: admin },
    { label: 'Monedas', path: '/configuracion/monedas', roles: admin },
  ] },
]

export function visibleNavigation(roles: readonly string[]) {
  return navigation.map(item => ({
    ...item,
    links: item.links.filter(link => link.roles.some(role => roles.includes(role))),
  })).filter(item => item.links.length > 0)
}

export function findPanelPage(path: string, roles: readonly string[]) {
  return visibleNavigation(roles).flatMap(item => item.links).find(link => link.path === path)
}
