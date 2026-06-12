import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import ArmyView from '../ArmyView.vue'
import { useArmyStore } from '@/stores/army.js'
import * as unitsApi from '@/util/api/units.js'

vi.mock('@/util/api/units.js', () => ({
  fetchRoster: vi.fn(),
  fetchTrainingQueue: vi.fn().mockResolvedValue([]),
  trainUnit: vi.fn(),
}))

describe('ArmyView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.setItem('villageId', '1')
    vi.clearAllMocks()
  })

  it('renders unit cards with icon, count, HP, and DMG when roster is non-empty', async () => {
    unitsApi.fetchRoster.mockResolvedValue([{ unitType: 'VANDAL', count: 5, hp: 4, damage: 1 }])

    const wrapper = mount(ArmyView)
    await flushPromises()

    expect(wrapper.text()).toContain('VANDAL')
    expect(wrapper.text()).toContain('× 5')
    expect(wrapper.text()).toContain('HP 4')
    expect(wrapper.text()).toContain('DMG 1')
    expect(wrapper.text()).toContain('⚔️')
  })

  it('renders empty state message when roster is empty', async () => {
    unitsApi.fetchRoster.mockResolvedValue([])

    const wrapper = mount(ArmyView)
    await flushPromises()

    expect(wrapper.text()).toContain('No units yet')
    expect(wrapper.text()).toContain('Barrack')
  })

  it('renders one card per unit type', async () => {
    unitsApi.fetchRoster.mockResolvedValue([
      { unitType: 'VANDAL', count: 3, hp: 4, damage: 1 },
      { unitType: 'ARCHER', count: 2, hp: 2, damage: 2 },
    ])

    const wrapper = mount(ArmyView)
    await flushPromises()

    const cards = wrapper.findAll('.card')
    expect(cards).toHaveLength(2)
  })

  it('shows updated count reactively when army store is refreshed after training completes', async () => {
    // Simulate the army store starting empty, then being refreshed when training finishes.
    // This verifies the army tab reflects new units without page navigation.
    unitsApi.fetchRoster.mockResolvedValue([])

    const armyStore = useArmyStore()
    const wrapper = mount(ArmyView)
    await flushPromises()

    expect(wrapper.text()).toContain('No units yet')

    // Training completes elsewhere — army store is refreshed
    armyStore.roster = [{ unitType: 'VANDAL', count: 3, hp: 4, damage: 1 }]
    await nextTick()

    expect(wrapper.text()).toContain('VANDAL')
    expect(wrapper.text()).toContain('× 3')
    expect(wrapper.find('.card').exists()).toBe(true)
  })
})
