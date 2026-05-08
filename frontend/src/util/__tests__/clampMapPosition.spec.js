import { describe, it, expect } from 'vitest'
import { clampMapPosition } from '../clampMapPosition.js'

function makeContainer(x, y, scaleX = 1, boundsW = 400, boundsH = 300) {
  return {
    x,
    y,
    scale: { x: scaleX, y: scaleX },
    getLocalBounds: () => ({ x: 0, y: 0, width: boundsW, height: boundsH }),
  }
}

// With canvasW=800, scaledW=400, scale=1:
//   minX = -400 * 0.3 = -120  (right edge must reach at least 20% of scaledW)
//   maxX =  800 + 400 * 0.3 = 920  (left edge must not exceed canvasW - 20% of scaledW)

describe('clampMapPosition', () => {
  it('clamps x when the map is dragged too far left', () => {
    const c = makeContainer(-9999, 0)
    clampMapPosition(c, 800, 600)
    expect(c.x).toBe(-120)
  })

  it('clamps x when the map is dragged too far right', () => {
    const c = makeContainer(9999, 0)
    clampMapPosition(c, 800, 600)
    expect(c.x).toBe(920)
  })

  it('leaves position unchanged when within valid range', () => {
    const c = makeContainer(200, 150)
    clampMapPosition(c, 800, 600)
    expect(c.x).toBe(200)
    expect(c.y).toBe(150)
  })
})
