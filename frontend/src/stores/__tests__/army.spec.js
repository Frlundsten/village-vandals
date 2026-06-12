import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useArmyStore } from '@/stores/army.js'
import { fetchRoster } from '@/util/api/units.js'

vi.mock('@/util/api/units.js', () => ({
  fetchRoster: vi.fn(),
}))

describe('useArmyStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('initialises roster to empty array', () => {
    const store = useArmyStore()
    expect(store.roster).toEqual([])
  })

  it('refresh_populatesRoster_fromFetchRoster', async () => {
    const units = [{ unitType: 'VANDAL', count: 5, hp: 4, damage: 1 }]
    fetchRoster.mockResolvedValue(units)

    const store = useArmyStore()
    await store.refresh(1)

    expect(fetchRoster).toHaveBeenCalledWith(1)
    expect(store.roster).toEqual(units)
  })

  it('refresh_setsEmptyArray_whenNoUnits', async () => {
    fetchRoster.mockResolvedValue([])

    const store = useArmyStore()
    await store.refresh(1)

    expect(store.roster).toEqual([])
  })

  it('refresh error leaves roster unchanged', async () => {
    const initial = [{ unitType: 'VANDAL', count: 2, hp: 4, damage: 1 }]
    fetchRoster.mockResolvedValueOnce(initial)
    const store = useArmyStore()
    await store.refresh(1)

    fetchRoster.mockRejectedValueOnce(new Error('network'))
    await store.refresh(1)

    expect(store.roster).toEqual(initial)
  })
})
