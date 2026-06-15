<template>
  <div class="scroll-picker" ref="pickerRef">
    <div class="picker-columns">
      <!-- Year -->
      <div
        class="picker-col"
        ref="yearColRef"
        @wheel="(e) => onWheel(e, 'year')"
        @pointerdown="(e) => onPointerDown(e, 'year')"
        @pointermove="(e) => onPointerMove(e, 'year')"
        @pointerup="(e) => onPointerUp(e, 'year')"
        @pointercancel="(e) => onPointerUp(e, 'year')"
      >
        <div class="picker-list" ref="yearListRef">
          <div
            v-for="y in yearOptions"
            :key="'y' + y"
            class="picker-item"
            :class="{ active: y === selYear }"
          >{{ y }}</div>
        </div>
      </div>
      <!-- Month -->
      <div
        class="picker-col"
        ref="monthColRef"
        @wheel="(e) => onWheel(e, 'month')"
        @pointerdown="(e) => onPointerDown(e, 'month')"
        @pointermove="(e) => onPointerMove(e, 'month')"
        @pointerup="(e) => onPointerUp(e, 'month')"
        @pointercancel="(e) => onPointerUp(e, 'month')"
      >
        <div class="picker-list" ref="monthListRef">
          <div
            v-for="m in monthOptions"
            :key="'m' + m"
            class="picker-item"
            :class="{ active: m === selMonth }"
          >{{ pad(m) }}</div>
        </div>
      </div>
      <!-- Day -->
      <div
        class="picker-col"
        ref="dayColRef"
        @wheel="(e) => onWheel(e, 'day')"
        @pointerdown="(e) => onPointerDown(e, 'day')"
        @pointermove="(e) => onPointerMove(e, 'day')"
        @pointerup="(e) => onPointerUp(e, 'day')"
        @pointercancel="(e) => onPointerUp(e, 'day')"
      >
        <div class="picker-list" ref="dayListRef">
          <div
            v-for="d in dayOptions"
            :key="'d' + d"
            class="picker-item"
            :class="{ active: d === selDay }"
          >{{ pad(d) }}</div>
        </div>
      </div>
    </div>
    <!-- Selection indicator: exactly one-row tall, centered. -->
    <div class="picker-indicator">
      <div class="indicator-line"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' } // yyyy-MM-dd
})
const emit = defineEmits(['update:modelValue'])

const ITEM_HEIGHT = 36
const VISIBLE_ROWS = 5
const PICKER_HEIGHT = ITEM_HEIGHT * VISIBLE_ROWS
const LIST_PADDING = (PICKER_HEIGHT - ITEM_HEIGHT) / 2 // top and bottom padding

const pickerRef = ref(null)
const yearColRef = ref(null)
const monthColRef = ref(null)
const dayColRef = ref(null)

const yearListRef = ref(null)
const monthListRef = ref(null)
const dayListRef = ref(null)

const today = new Date()
const selYear = ref(today.getFullYear())
const selMonth = ref(today.getMonth() + 1)
const selDay = ref(today.getDate())

// Flag: currently performing a programmatic scroll per column.
// While true, ignore the scroll-event → state sync for that column.
const progScrolling = { year: false, month: false, day: false }

const pad = (n) => String(n).padStart(2, '0')

const parseDate = (val) => {
  if (!val || !/^\d{4}-\d{2}-\d{2}$/.test(val)) return
  const [y, m, d] = val.split('-').map(Number)
  if (!y || !m || !d) return
  selYear.value = y
  selMonth.value = m
  selDay.value = d
}

watch(() => props.modelValue, parseDate, { immediate: true })

const syncValue = () => {
  const y = selYear.value
  const m = pad(selMonth.value)
  const d = pad(selDay.value)
  emit('update:modelValue', `${y}-${m}-${d}`)
}

// -------- options --------
const yearOptions = computed(() => {
  const now = new Date().getFullYear()
  const arr = []
  for (let y = now; y >= 1990; y--) arr.push(y)
  return arr
})

const monthOptions = computed(() => {
  const arr = []
  for (let m = 1; m <= 12; m++) arr.push(m)
  return arr
})

const dayOptions = computed(() => {
  const days = new Date(selYear.value, selMonth.value, 0).getDate()
  const arr = []
  for (let d = 1; d <= days; d++) arr.push(d)
  return arr
})

// -------- helpers --------
const getColRefs = (col) => {
  if (col === 'year')  return { col: yearColRef.value,  list: yearListRef.value,  options: yearOptions.value,  state: 'year' }
  if (col === 'month') return { col: monthColRef.value, list: monthListRef.value, options: monthOptions.value, state: 'month' }
  if (col === 'day')   return { col: dayColRef.value,   list: dayListRef.value,   options: dayOptions.value,   state: 'day' }
  return null
}

// Scroll a column so that item at `idx` is centered inside the blue indicator.
const scrollColToIndex = (col, idx, smooth = true) => {
  const refs = getColRefs(col)
  if (!refs || !refs.col || !refs.list) return
  const clamped = Math.max(0, Math.min(idx, refs.options.length - 1))
  const target = clamped * ITEM_HEIGHT
  progScrolling[refs.state] = true
  refs.col.scrollTo({ top: target, behavior: smooth ? 'smooth' : 'auto' })
  // Release the programmatic-scroll lock once scrolling settles.
  // Use two timeouts: short one for "auto" instant, long one for smooth.
  const waitMs = smooth ? 320 : 50
  if (scrollColToIndex._ts && scrollColToIndex._ts[refs.state]) {
    clearTimeout(scrollColToIndex._ts[refs.state])
  }
  scrollColToIndex._ts = scrollColToIndex._ts || {}
  scrollColToIndex._ts[refs.state] = setTimeout(() => {
    progScrolling[refs.state] = false
  }, waitMs)
}

const scrollToSelected = (smooth = true) => {
  scrollColToIndex('year',  yearOptions.value.indexOf(selYear.value),   smooth)
  scrollColToIndex('month', monthOptions.value.indexOf(selMonth.value), smooth)
  scrollColToIndex('day',   dayOptions.value.indexOf(selDay.value),     smooth)
}

// -------- wheel: take full control, one tick per scroll increment --------
const onWheel = (e, col) => {
  e.preventDefault()
  // Negative deltaY => scroll up => previous index. Positive => next.
  const dir = e.deltaY > 0 ? 1 : -1
  stepBy(col, dir)
}

const stepBy = (col, dir) => {
  const refs = getColRefs(col)
  if (!refs || !refs.col || !refs.list) return
  const curIdx = Math.round(refs.col.scrollTop / ITEM_HEIGHT)
  const nextIdx = Math.max(0, Math.min(refs.options.length - 1, curIdx + dir))
  scrollColToIndex(col, nextIdx, true)
  // Immediately update selection so highlight stays in sync with the scroll target.
  const value = refs.options[nextIdx]
  if (col === 'year'  && selYear.value  !== value) { selYear.value  = value; syncValue() }
  if (col === 'month' && selMonth.value !== value) { selMonth.value = value; syncValue() }
  if (col === 'day'   && selDay.value   !== value) { selDay.value   = value; syncValue() }
}

// -------- drag / pointer support: let users drag the column, then snap --------
const dragState = { active: false, startY: 0, startScroll: 0, col: null }

const onPointerDown = (e, col) => {
  dragState.active = true
  dragState.col = col
  dragState.startY = e.clientY
  const refs = getColRefs(col)
  dragState.startScroll = refs?.col?.scrollTop || 0
  try { e.target.setPointerCapture && e.target.setPointerCapture(e.pointerId) } catch (_) {}
}

const onPointerMove = (e, col) => {
  if (!dragState.active || dragState.col !== col) return
  const refs = getColRefs(col)
  if (!refs || !refs.col) return
  const dy = e.clientY - dragState.startY
  refs.col.scrollTop = dragState.startScroll - dy
}

const onPointerUp = (e, col) => {
  if (!dragState.active) return
  dragState.active = false
  dragState.col = null
  // After a drag, snap to the nearest item.
  const refs = getColRefs(col)
  if (!refs || !refs.col) return
  const idx = Math.round(refs.col.scrollTop / ITEM_HEIGHT)
  const clamped = Math.max(0, Math.min(refs.options.length - 1, idx))
  scrollColToIndex(col, clamped, true)
  const value = refs.options[clamped]
  if (col === 'year'  && selYear.value  !== value) { selYear.value  = value; syncValue() }
  if (col === 'month' && selMonth.value !== value) { selMonth.value = value; syncValue() }
  if (col === 'day'   && selDay.value   !== value) { selDay.value   = value; syncValue() }
}

// -------- reactive watch: keep day list in bounds when year / month changes. --------
watch([selYear, selMonth], () => {
  nextTick(() => {
    const max = dayOptions.value.length
    if (selDay.value > max) selDay.value = max
    if (selDay.value < 1) selDay.value = 1
    nextTick(() => scrollToSelected(true))
  })
})

// When the parent updates modelValue externally, re-scroll to the new value.
watch(() => props.modelValue, () => {
  nextTick(() => scrollToSelected(true))
})

// -------- mount / unmount --------
onMounted(async () => {
  await nextTick()
  // Initial scroll to currently-selected date.
  scrollToSelected(false)
})

// (No global listeners to clean up; everything uses per-col scoped listeners.)
onBeforeUnmount(() => {})
</script>

<style scoped>
.scroll-picker {
  position: relative;
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  user-select: none;
  overflow: hidden;
  height: 180px; /* = ITEM_HEIGHT * VISIBLE_ROWS */
}

.picker-columns {
  display: flex;
  height: 100%;
  padding: 0 8px;
}

.picker-col {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: none;
  -ms-overflow-style: none;
  cursor: grab;
  overscroll-behavior: contain;
  scroll-behavior: auto; /* We drive position ourselves; default smooth fights us. */
}

.picker-col::-webkit-scrollbar { display: none; }
.picker-col:first-child { flex: 1.15; }

.picker-list {
  /* Padding above/below lets the first & last items scroll into the center. */
  padding: 72px 0; /* = (180 - 36) / 2 */
}

.picker-item {
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-variant-numeric: tabular-nums;
  color: #94a3b8;
  transition: color 0.15s ease, font-weight 0.15s ease, font-size 0.15s ease;
  pointer-events: none;
  line-height: 36px;
  box-sizing: border-box;
}

.picker-item.active {
  color: #0f172a;
  font-weight: 700;
  font-size: 17px;
}

/* Center highlight: one item tall, positioned at pixel center of the picker. */
.picker-indicator {
  position: absolute;
  left: 12px;
  right: 12px;
  height: 36px;
  /* Center vertically: (180 - 36) / 2 = 72 from top */
  top: 72px;
  pointer-events: none;
}

.indicator-line {
  width: 100%;
  height: 36px;
  border-top: 1px solid #cbd5e1;
  border-bottom: 1px solid #cbd5e1;
  background: rgba(22, 119, 255, 0.06);
  border-radius: 6px;
}
</style>
