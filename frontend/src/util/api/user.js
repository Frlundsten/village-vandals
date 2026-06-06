import { apiRequest } from '@/util/api/api.js'

export async function getUser() {
  return apiRequest('/user')
}
