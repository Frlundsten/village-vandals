import { apiRequest } from '@/util/api/api.js'

export async function registerUser(userInfo){
  return apiRequest(`/user/register`,{
    method: 'POST',
    body: userInfo,
  })
}