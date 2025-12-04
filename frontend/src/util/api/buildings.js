import { apiRequest } from '@/util/api/api.js'

export async function fetchBuildings(villageId) {
  return apiRequest(`/building?villageId=${villageId}`)
}

export async function upgradeBuilding(villageId, buildingId) {
  return apiRequest(`/building/upgrade`, {
    method: 'POST',
    body: { villageId, buildingId },
  })
}

export async function constructBuilding(type, constructionSiteId, villageId, upgradeCost) {
  return apiRequest(`/building`, {
    method: 'POST',
    body: {
      type: type,
      constructionSiteId: constructionSiteId,
      villageId: villageId,
      upgradeCost: upgradeCost,
    },
  })
}

export async function getAvailableBuildings(villageId) {
  return apiRequest(`/building/available?villageId=${villageId}`, {
    method: 'GET',
  })
}
