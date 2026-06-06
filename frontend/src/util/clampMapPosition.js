export function clampMapPosition(container, canvasWidth, canvasHeight) {
  const scale = container.scale.x
  const bounds = container.getLocalBounds()
  const scaledW = bounds.width * scale
  const scaledH = bounds.height * scale
  const margin = 0.3 // 0.5 (half-size) - 0.2 (minVisible fraction)

  container.x = Math.max(-scaledW * margin, Math.min(canvasWidth + scaledW * margin, container.x))
  container.y = Math.max(-scaledH * margin, Math.min(canvasHeight + scaledH * margin, container.y))
}
