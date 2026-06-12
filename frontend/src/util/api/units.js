import { apiRequest } from '@/util/api/api.js'

export async function trainUnit(villageId, buildingId, quantity = 1) {
  return apiRequest('/unit/train', {
    method: 'POST',
    body: { villageId, buildingId, quantity },
  })
}

export async function fetchRoster(villageId) {
  return apiRequest(`/unit?villageId=${villageId}`)
}

export async function fetchTrainingQueue(villageId) {
  return apiRequest(`/unit/training?villageId=${villageId}`)
}
