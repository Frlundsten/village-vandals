import { BASE_URL } from '@/util/util.js'

export async function fetchBuildings(villageId) {
  try {
    const response = await fetch(
      `${BASE_URL}/building?villageId=${villageId}`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem("jwt_token")}`,
        },
      }
    )

    if (!response.ok) {
      throw new Error('Failed to fetch buildings')
    }

    return await response.json()
  } catch (e) {
    console.error('Error fetching buildings:', e)
  }
}