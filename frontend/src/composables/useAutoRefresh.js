import { onMounted, onUnmounted } from 'vue'

export function useAutoRefresh(callback, intervalMs = 30000, shouldRun = null) {
  let timer = null

  const stop = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  const start = () => {
    stop()
    callback()
    timer = setInterval(() => {
      if (shouldRun && !shouldRun()) {
        stop()
        return
      }
      callback()
    }, intervalMs)
  }

  onMounted(() => start())
  onUnmounted(() => stop())

  return { start, stop }
}

export function isTradingHours() {
  const now = new Date()
  const beijingOffset = 8 * 60
  const localOffset = now.getTimezoneOffset()
  const beijingMinutes = (now.getUTCHours() * 60 + now.getUTCMinutes()) + beijingOffset
  const adjustedMinutes = (beijingMinutes + 24 * 60) % (24 * 60)

  const beijingDayOfWeek = new Date(now.getTime() + (beijingOffset - localOffset) * 60000).getUTCDay()
  if (beijingDayOfWeek === 0 || beijingDayOfWeek === 6) return false

  const tradingStart = 9 * 60 + 30
  const tradingEnd = 15 * 60

  return adjustedMinutes >= tradingStart && adjustedMinutes <= tradingEnd
}
