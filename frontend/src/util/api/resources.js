import { BASE_URL } from '@/util/util.js'

export async function refreshStorage(villageId) {
  try {
    const response = await fetch(`${BASE_URL}/resources/refresh?villageId=${villageId}`, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${localStorage.getItem('jwt_token')}`,
      },
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    return await response.json()
  } catch (error) {
    console.error('Failed to refresh storage:', error)
  }
}
