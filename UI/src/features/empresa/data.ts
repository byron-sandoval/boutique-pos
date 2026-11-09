import { api } from '@/api'
import { createEmpresaService } from './service'

export const empresaService = createEmpresaService(api)
