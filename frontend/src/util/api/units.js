import { apiRequest } from '@/util/api/api.js'

export async function trainUnit(villageId, buildingId) {
  return apiRequest('/unit/train', {
    method: 'POST',
    body: { villageId, buildingId },
  })
}

export async function fetchRoster(villageId) {
  return apiRequest(`/unit?villageId=${villageId}`)
}
