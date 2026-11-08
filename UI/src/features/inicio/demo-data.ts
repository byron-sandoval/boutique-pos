// Datos ilustrativos del diseño. No representan operaciones de PostgreSQL.
export const periods = {
  week: {
    label: 'Últimos 7 días',
    sales: 24850,
    count: 38,
    previousCount: 34,
    labels: ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'],
    values: [1800, 3200, 2600, 4100, 3650, 5600, 3900],
    previous: [2200, 1800, 3000, 2700, 3400, 4200, 2800],
  },
  month: {
    label: 'Últimos 30 días',
    sales: 98400,
    count: 152,
    previousCount: 136,
    labels: ['Sem 1', 'Sem 2', 'Sem 3', 'Sem 4'],
    values: [19800, 23650, 24850, 30100],
    previous: [17500, 20000, 22100, 26400],
  },
}

export const recentSales = [
  { invoice: 'B-000128', name: 'María Fernanda', initials: 'MF', date: 'Hoy, 11:42', items: 3, method: 'Efectivo', total: 1850, status: 'Completada' },
  { invoice: 'B-000127', name: 'Consumidor final', initials: 'CF', date: 'Hoy, 11:18', items: 1, method: 'Tarjeta', total: 890, status: 'Completada' },
  { invoice: 'B-000126', name: 'Daniela López', initials: 'DL', date: 'Hoy, 10:56', items: 2, method: 'Transferencia', total: 1420, status: 'Completada' },
  { invoice: 'B-000125', name: 'Ana Martínez', initials: 'AM', date: 'Hoy, 10:24', items: 1, method: 'Efectivo', total: 650, status: 'Anulada' },
]

export const featuredProducts = [
  { name: 'Blusa de lino', detail: 'Ropa · 24 unidades vendidas', price: 650, type: 'shirt', color: 'rose', percent: 82 },
  { name: 'Sandalias clásicas', detail: 'Calzado · 18 unidades vendidas', price: 890, type: 'shoe', color: 'sand', percent: 64 },
  { name: 'Bolso de mano', detail: 'Accesorios · 12 unidades vendidas', price: 1200, type: 'bag', color: 'sage', percent: 46 },
] as const

export function formatMoney(value: number) {
  return `C$ ${new Intl.NumberFormat('es-NI', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)}`
}
