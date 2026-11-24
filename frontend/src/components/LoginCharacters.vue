<template>
  <div class="characters">
    <!-- 紫色高个子 · 后排 -->
    <div ref="purpleRef" class="character" :style="purpleStyle">
      <div class="eyes" :style="purpleEyesStyle">
        <CharacterEye
          :size="18"
          :pupil-size="7"
          :max-distance="5"
          eye-color="#ffffff"
          pupil-color="#2D2D2D"
          :is-blinking="isPurpleBlinking"
          :force-look-x="purpleLookX"
          :force-look-y="purpleLookY"
        />
        <CharacterEye
          :size="18"
          :pupil-size="7"
          :max-distance="5"
          eye-color="#ffffff"
          pupil-color="#2D2D2D"
          :is-blinking="isPurpleBlinking"
          :force-look-x="purpleLookX"
          :force-look-y="purpleLookY"
        />
      </div>
    </div>

    <!-- 黑色高个子 · 中层 -->
    <div ref="blackRef" class="character" :style="blackStyle">
      <div class="eyes" :style="blackEyesStyle">
        <CharacterEye
          :size="16"
          :pupil-size="6"
          :max-distance="4"
          eye-color="#ffffff"
          pupil-color="#2D2D2D"
          :is-blinking="isBlackBlinking"
          :force-look-x="blackLookX"
          :force-look-y="blackLookY"
        />
        <CharacterEye
          :size="16"
          :pupil-size="6"
          :max-distance="4"
          eye-color="#ffffff"
          pupil-color="#2D2D2D"
          :is-blinking="isBlackBlinking"
          :force-look-x="blackLookX"
          :force-look-y="blackLookY"
        />
      </div>
    </div>

    <!-- 橙色半圆 · 前排左 -->
    <div ref="orangeRef" class="character orange" :style="orangeStyle">
      <div class="eyes" :style="orangeEyesStyle">
        <CharacterPupil
          :size="12"
          :max-distance="5"
          pupil-color="#2D2D2D"
          :force-look-x="simpleLookX"
          :force-look-y="simpleLookY"
        />
        <CharacterPupil
          :size="12"
          :max-distance="5"
          pupil-color="#2D2D2D"
          :force-look-x="simpleLookX"
          :force-look-y="simpleLookY"
        />
      </div>
    </div>

    <!-- 黄色高个子 · 前排右 -->
    <div ref="yellowRef" class="character yellow" :style="yellowStyle">
      <div class="eyes" :style="yellowEyesStyle">
        <CharacterPupil
          :size="12"
          :max-distance="5"
          pupil-color="#2D2D2D"
          :force-look-x="simpleLookX"
          :force-look-y="simpleLookY"
        />
        <CharacterPupil
          :size="12"
          :max-distance="5"
          pupil-color="#2D2D2D"
          :force-look-x="simpleLookX"
          :force-look-y="simpleLookY"
        />
      </div>
      <div class="mouth" :style="yellowMouthStyle"></div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import CharacterEye from './CharacterEye.vue'
import CharacterPupil from './CharacterPupil.vue'

const props = defineProps({
  passwordLength: { type: Number, default: 0 },
  showPassword: { type: Boolean, default: false },
  typing: { type: Boolean, default: false }
})

const purpleRef = ref()
const blackRef = ref()
const yellowRef = ref()
const orangeRef = ref()

const mouseX = ref(0)
const mouseY = ref(0)
let mouseHandler = null

const isPurpleBlinking = ref(false)
const isBlackBlinking = ref(false)
const isLookingAtEachOther = ref(false)
const isPurplePeeking = ref(false)

const timers = []
function later(fn, delay) {
  const id = setTimeout(fn, delay)
  timers.push(id)
  return id
}
function clearTimers() {
  timers.forEach((id) => clearTimeout(id))
  timers.length = 0
}

function startBlink(target) {
  const schedule = () => {
    later(() => {
      target.value = true
      later(() => {
        target.value = false
        schedule()
      }, 150)
    }, Math.random() * 4000 + 3000)
  }
  schedule()
}

const hiddenPassword = computed(() => props.passwordLength > 0 && !props.showPassword)
const visiblePassword = computed(() => props.passwordLength > 0 && props.showPassword)
const acting = computed(() => props.typing || hiddenPassword.value)

function calcPos(el) {
  if (!el) return { faceX: 0, faceY: 0, bodySkew: 0 }
  const rect = el.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 3
  const deltaX = mouseX.value - centerX
  const deltaY = mouseY.value - centerY
  return {
    faceX: Math.max(-15, Math.min(15, deltaX / 20)),
    faceY: Math.max(-10, Math.min(10, deltaY / 30)),
    bodySkew: Math.max(-6, Math.min(6, -deltaX / 120))
  }
}

const purplePos = computed(() => calcPos(purpleRef.value))
const blackPos = computed(() => calcPos(blackRef.value))
const yellowPos = computed(() => calcPos(yellowRef.value))
const orangePos = computed(() => calcPos(orangeRef.value))

const purpleStyle = computed(() => ({
  left: '70px',
  width: '180px',
  height: acting.value ? '440px' : '400px',
  backgroundColor: '#6C3FF5',
  borderRadius: '10px 10px 0 0',
  zIndex: 1,
  transform: visiblePassword.value
    ? 'skewX(0deg)'
    : acting.value
      ? `skewX(${(purplePos.value.bodySkew || 0) - 12}deg) translateX(40px)`
      : `skewX(${purplePos.value.bodySkew || 0}deg)`
}))

const purpleEyesStyle = computed(() => ({
  gap: '32px',
  left: visiblePassword.value ? '20px' : isLookingAtEachOther.value ? '55px' : `${45 + purplePos.value.faceX}px`,
  top: visiblePassword.value ? '35px' : isLookingAtEachOther.value ? '65px' : `${40 + purplePos.value.faceY}px`
}))

const purpleLookX = computed(() => {
  if (visiblePassword.value) return isPurplePeeking.value ? 4 : -4
  return isLookingAtEachOther.value ? 3 : undefined
})
const purpleLookY = computed(() => {
  if (visiblePassword.value) return isPurplePeeking.value ? 5 : -4
  return isLookingAtEachOther.value ? 4 : undefined
})

const blackStyle = computed(() => ({
  left: '240px',
  width: '120px',
  height: '310px',
  backgroundColor: '#2D2D2D',
  borderRadius: '8px 8px 0 0',
  zIndex: 2,
  transform: visiblePassword.value
    ? 'skewX(0deg)'
    : isLookingAtEachOther.value
      ? `skewX(${(blackPos.value.bodySkew || 0) * 1.5 + 10}deg) translateX(20px)`
      : acting.value
        ? `skewX(${(blackPos.value.bodySkew || 0) * 1.5}deg)`
        : `skewX(${blackPos.value.bodySkew || 0}deg)`
}))

const blackEyesStyle = computed(() => ({
  gap: '24px',
  left: visiblePassword.value ? '10px' : isLookingAtEachOther.value ? '32px' : `${26 + blackPos.value.faceX}px`,
  top: visiblePassword.value ? '28px' : isLookingAtEachOther.value ? '12px' : `${32 + blackPos.value.faceY}px`
}))

const blackLookX = computed(() => (visiblePassword.value ? -4 : isLookingAtEachOther.value ? 0 : undefined))
const blackLookY = computed(() => (visiblePassword.value ? -4 : isLookingAtEachOther.value ? -4 : undefined))

const simpleLookX = computed(() => (visiblePassword.value ? -5 : undefined))
const simpleLookY = computed(() => (visiblePassword.value ? -4 : undefined))

const orangeStyle = computed(() => ({
  left: '0px',
  width: '240px',
  height: '200px',
  zIndex: 3,
  backgroundColor: '#FF9B6B',
  borderRadius: '120px 120px 0 0',
  transform: visiblePassword.value ? 'skewX(0deg)' : `skewX(${orangePos.value.bodySkew || 0}deg)`
}))

const orangeEyesStyle = computed(() => ({
  gap: '32px',
  left: visiblePassword.value ? '50px' : `${82 + (orangePos.value.faceX || 0)}px`,
  top: visiblePassword.value ? '85px' : `${90 + (orangePos.value.faceY || 0)}px`
}))

const yellowStyle = computed(() => ({
  left: '310px',
  width: '140px',
  height: '230px',
  backgroundColor: '#E8D754',
  borderRadius: '70px 70px 0 0',
  zIndex: 4,
  transform: visiblePassword.value ? 'skewX(0deg)' : `skewX(${yellowPos.value.bodySkew || 0}deg)`
}))

const yellowEyesStyle = computed(() => ({
  gap: '24px',
  left: visiblePassword.value ? '20px' : `${52 + (yellowPos.value.faceX || 0)}px`,
  top: visiblePassword.value ? '35px' : `${40 + (yellowPos.value.faceY || 0)}px`
}))

const yellowMouthStyle = computed(() => ({
  left: visiblePassword.value ? '10px' : `${40 + (yellowPos.value.faceX || 0)}px`,
  top: visiblePassword.value ? '88px' : `${88 + (yellowPos.value.faceY || 0)}px`
}))

watch(
  () => props.typing,
  (value) => {
    if (value) {
      isLookingAtEachOther.value = true
      later(() => {
        isLookingAtEachOther.value = false
      }, 800)
    } else {
      isLookingAtEachOther.value = false
    }
  }
)

let peekTimer = null
function schedulePeek() {
  clearTimeout(peekTimer)
  if (!visiblePassword.value) return
  peekTimer = setTimeout(() => {
    isPurplePeeking.value = true
    setTimeout(() => {
      isPurplePeeking.value = false
    }, 800)
    schedulePeek()
  }, Math.random() * 3000 + 2000)
}

watch(visiblePassword, (active) => {
  isPurplePeeking.value = false
  if (active) {
    schedulePeek()
  } else {
    clearTimeout(peekTimer)
  }
})

onMounted(() => {
  mouseHandler = (e) => {
    mouseX.value = e.clientX
    mouseY.value = e.clientY
  }
  window.addEventListener('mousemove', mouseHandler)
  startBlink(isPurpleBlinking)
  startBlink(isBlackBlinking)
})

onBeforeUnmount(() => {
  if (mouseHandler) window.removeEventListener('mousemove', mouseHandler)
  clearTimeout(peekTimer)
  clearTimers()
})
</script>

<style scoped>
.characters {
  position: relative;
  width: 550px;
  height: 400px;
  flex-shrink: 0;
}

.character {
  position: absolute;
  bottom: 0;
  transition: transform 0.7s ease-in-out, height 0.7s ease-in-out;
  transform-origin: bottom center;
}

.eyes {
  position: absolute;
  display: flex;
  align-items: center;
  transition: all 0.7s ease-in-out;
}

.orange .eyes,
.yellow .eyes {
  transition: all 0.2s ease-out;
}

.mouth {
  position: absolute;
  width: 80px;
  height: 4px;
  border-radius: 999px;
  background: #2d2d2d;
  transition: all 0.2s ease-out;
}
</style>
