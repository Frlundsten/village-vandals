import { apiRequest } from '@/util/api/api.js'

export async function refreshStorage(villageId) {
  return apiRequest(`/resources/refresh?villageId=${villageId}`, {
    method: 'GET',
  })
}
