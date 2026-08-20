import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import WorldView from '../WorldView.vue'
import * as villagesApi from '@/util/api/villages.js'

vi.mock('@/util/api/villages.js', () => ({
  fetchUsers: vi.fn(),
}))

describe('WorldView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('marks occupied tiles and centres the grid when the fetch succeeds', async () => {
    villagesApi.fetchUsers.mockResolvedValue([{ userId: 'u1', username: 'alice', x: 2, y: 3 }])

    const wrapper = mount(WorldView)
    await flushPromises()

    const occupied = wrapper.findAll('.iso-tile').filter((tile) => {
      const style = tile.attributes('style') ?? ''
      return style.includes('f44336') || style.includes('244, 67, 54')
    })
    expect(occupied.length).toBe(1)
    expect(wrapper.find('.iso-grid').attributes('style')).toContain('translate(')
  })

  it('still centres the grid and shows an error when the fetch fails', async () => {
    villagesApi.fetchUsers.mockRejectedValue(new Error('Your session has expired'))
    const unhandled = vi.fn()
    process.on('unhandledRejection', unhandled)

    const wrapper = mount(WorldView)
    await flushPromises()

    expect(wrapper.text()).toContain('Your session has expired')
    // The centring block must still have run — otherwise half the diamond renders off-screen.
    expect(wrapper.find('.iso-grid').attributes('style')).toContain('translate(')
    expect(unhandled).not.toHaveBeenCalled()
    process.off('unhandledRejection', unhandled)
  })

  it('tolerates a non-array response without throwing', async () => {
    villagesApi.fetchUsers.mockResolvedValue(null)

    const wrapper = mount(WorldView)
    await flushPromises()

    expect(wrapper.find('.iso-grid').exists()).toBe(true)
  })
})
