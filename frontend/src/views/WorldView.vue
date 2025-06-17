<template>
  <div
      class="iso-wrapper"
      ref="wrapperRef"
      @wheel.prevent="handleWheel"
      @mousemove="handleMouseMove"
      @pointerdown="handlePointerDown"
      @pointermove="handlePointerMove"
      @pointerup="handlePointerUp"
      @pointerleave="handlePointerUp"
  >
    <div
        class="iso-grid"
        :style="{
        transform: `translate(${translate.x}px, ${translate.y}px) scale(${zoom})`,
        transformOrigin: `${origin.x}px ${origin.y}px`
      }"
    >
      <div
          v-for="(tile, index) in tiles"
          :key="index"
          class="iso-tile"
          :style="{
          backgroundColor: tile.color,
          left: `${getX(index)}px`,
          top: `${getY(index)}px`
        }"
          @click="colorTile(index)"
      ></div>
    </div>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'

const gridSize = 10
const defaultColor = '#ffffff'
const selectedColor = ref('#4caf50')
const zoom = ref(1)
const translate = ref({ x: 0, y: 0 })
const origin = ref({ x: 0, y: 0 })

const wrapperRef = ref(null)

const tiles = ref(
    Array(gridSize * gridSize).fill().map(() => ({
      color: defaultColor,
    }))
)

function colorTile(index) {
  tiles.value[index].color = selectedColor.value
}

function getX(index) {
  const x = index % gridSize
  const y = Math.floor(index / gridSize)
  return (x - y) * 40
}

function getY(index) {
  const x = index % gridSize
  const y = Math.floor(index / gridSize)
  return (x + y) * 20
}


const tileWidth = 80
const tileHeight = 80

onMounted(() => {
  // Calculate grid width and height in pixels using your isometric formulas
  const gridPixelWidth = (gridSize - 1) * 40 * 2 // horizontal max distance between tiles (because getX uses (x - y) * 40)
  const gridPixelHeight = (gridSize - 1) * 20 * 2 // vertical max distance between tiles (because getY uses (x + y) * 20)

  // Calculate center of the viewport
  const wrapperRect = wrapperRef.value.getBoundingClientRect()
  const centerX = wrapperRect.width / 2
  const centerY = wrapperRect.height / 2

  // Calculate translate to center the grid
  translate.value = {
    x: centerX - gridPixelWidth / 2 - tileWidth / 2,
    y: centerY - gridPixelHeight / 2 - tileHeight / 2,
  }
})

// Track mouse position over wrapper
let lastMousePos = { x: 0, y: 0 }

function handleMouseMove(event) {
  const rect = wrapperRef.value.getBoundingClientRect()
  lastMousePos = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top,
  }
}

function handleWheel(event) {
  const delta = event.deltaY > 0 ? -0.1 : 0.1
  const newZoom = Math.min(Math.max(zoom.value + delta, 0.4), 2.5)

  // Compute zoom factor
  const zoomFactor = newZoom / zoom.value

  // Adjust translate to zoom toward the pointer
  translate.value = {
    x: lastMousePos.x - (lastMousePos.x - translate.value.x) * zoomFactor,
    y: lastMousePos.y - (lastMousePos.y - translate.value.y) * zoomFactor,
  }

  zoom.value = newZoom
}

// Track dragging state
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })
const translateStart = ref({ x: 0, y: 0 })

function handlePointerDown(event) {
  isDragging.value = true
  dragStart.value = { x: event.clientX, y: event.clientY }
  translateStart.value = { ...translate.value }
}

function handlePointerMove(event) {
  if (!isDragging.value) return

  const dx = event.clientX - dragStart.value.x
  const dy = event.clientY - dragStart.value.y

  translate.value = {
    x: translateStart.value.x + dx,
    y: translateStart.value.y + dy,
  }
}

function handlePointerUp() {
  isDragging.value = false
}
</script>

<style scoped>
.iso-wrapper {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #1e1e1e;
  position: relative;
}

.iso-grid {
  position: absolute;
  transform-origin: 0 0;
  transition: transform 0.1s ease-out;
}

.iso-tile {
  position: absolute;
  width: 80px;
  height: 80px;
  transform: rotateX(60deg);
  clip-path: polygon(
      50% 0%,
      100% 50%,
      50% 100%,
      0% 50%
  );
  box-sizing: border-box;
  background-color: #eee;
  transition: background-color 0.2s, transform 0.2s;
  cursor: pointer;
}
</style>
