<template>
  <div ref="el" class="chart" :style="{ height }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '320px' }
})

const emit = defineEmits(['click'])

const el = ref(null)
let chart = null

function resize() {
  chart && chart.resize()
}

onMounted(() => {
  chart = echarts.init(el.value)
  chart.setOption(props.option)
  chart.on('click', (params) => emit('click', params))
  window.addEventListener('resize', resize)
})

watch(
  () => props.option,
  (value) => {
    if (chart) {
      chart.setOption(value, true)
    }
  },
  { deep: true }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>
