import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useResourceStore } from '@/stores/resources.js'

vi.mock('@/util/api/resources.js', () => ({
  refreshStorage: vi.fn(),
}))

import { refreshStorage } from '@/util/api/resources.js'

describe('useResourceStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('initialises all amounts and rates to 0', () => {
    const store = useResourceStore()
    expect(store.food).toBe(0)
    expect(store.wood).toBe(0)
    expect(store.bricks).toBe(0)
    expect(store.iron).toBe(0)
    expect(store.foodPerHour).toBe(0)
    expect(store.woodPerHour).toBe(0)
    expect(store.bricksPerHour).toBe(0)
    expect(store.ironPerHour).toBe(0)
  })

  it('refresh() updates amounts and production rates from API response', async () => {
    refreshStorage.mockResolvedValue({
      food: 100,
      wood: 200,
      bricks: 300,
      iron: 400,
      foodPerHour: 10,
      woodPerHour: 20,
      bricksPerHour: 30,
      ironPerHour: 40,
    })

    const store = useResourceStore()
    await store.refresh(1)

    expect(store.food).toBe(100)
    expect(store.wood).toBe(200)
    expect(store.bricks).toBe(300)
    expect(store.iron).toBe(400)
    expect(store.foodPerHour).toBe(10)
    expect(store.woodPerHour).toBe(20)
    expect(store.bricksPerHour).toBe(30)
    expect(store.ironPerHour).toBe(40)
  })
})
