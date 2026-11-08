import { expireSession, getAccessToken } from '../auth/session'
import { config } from '../config'
import { createApiClient } from './client'

export const api = createApiClient({
  baseUrl: config.apiUrl,
  getToken: getAccessToken,
  onUnauthorized: expireSession,
})
