export function formatNumber(value) {
  if (value === null || value === undefined) return '¥0.00'
  const num = Number(value)
  if (num >= 0) {
    return '¥' + num.toLocaleString('zh-CN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })
  } else {
    return '-' + '¥' + Math.abs(num).toLocaleString('zh-CN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })
  }
}

export function formatProfit(value) {
  if (value === null || value === undefined) return '+0.00'
  const num = Number(value)
  return (num >= 0 ? '+' : '') + num.toFixed(2)
}

export function formatPercent(value, { nullDisplay = '0.00%' } = {}) {
  if (value === null || value === undefined) return nullDisplay
  const num = Number(value)
  return (num >= 0 ? '+' : '') + num.toFixed(2) + '%'
}

export function getProfitClass(value, { prefix = 'profit-', zeroClass = true } = {}) {
  if (value === null || value === undefined) return ''
  const num = Number(value)
  if (num === 0) return zeroClass ? `${prefix}zero` : ''
  return num > 0 ? `${prefix}positive` : `${prefix}negative`
}
