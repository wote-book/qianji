<template>
  <div ref="pupilRef" class="pupil" :style="pupilStyle"></div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  size: { type: Number, default: 12 },
  maxDistance: { type: Number, default: 5 },
  pupilColor: { type: String, default: '#2D2D2D' },
  forceLookX: { type: Number, default: undefined },
  forceLookY: { type: Number, default: undefined }
})

const pupilRef = ref()
const mouseX = ref(0)
const mouseY = ref(0)

function onMouseMove(e) {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

onMounted(() => window.addEventListener('mousemove', onMouseMove))
onBeforeUnmount(() => window.removeEventListener('mousemove', onMouseMove))

const offset = computed(() => {
  if (props.forceLookX !== undefined && props.forceLookY !== undefined) {
    return { x: props.forceLookX, y: props.forceLookY }
  }
  if (!pupilRef.value) return { x: 0, y: 0 }
  const rect = pupilRef.value.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 2
  const deltaX = mouseX.value - centerX
  const deltaY = mouseY.value - centerY
  const distance = Math.min(Math.hypot(deltaX, deltaY), props.maxDistance)
  const angle = Math.atan2(deltaY, deltaX)
  return { x: Math.cos(angle) * distance, y: Math.sin(angle) * distance }
})

const pupilStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  backgroundColor: props.pupilColor,
  transform: `translate(${offset.value.x}px, ${offset.value.y}px)`
}))
</script>

<style scoped>
.pupil {
  border-radius: 999px;
  transition: transform 0.1s ease-out;
}
</style>
