import { apiRequest } from '@/util/api/api.js'

export async function fetchUsers() {
  return apiRequest(`/user/all`, {
    method: 'GET',
  })
}
