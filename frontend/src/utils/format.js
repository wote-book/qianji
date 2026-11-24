export function money(value, digits = 2) {
  const number = Number(value || 0)
  return number.toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  })
}

export function percent(value) {
  return `${Number(value || 0).toFixed(1)}%`
}

export function currentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

export function today() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

export function monthRange(month) {
  const [year, mon] = month.split('-').map(Number)
  const start = `${month}-01`
  const lastDay = new Date(year, mon, 0).getDate()
  return { start, end: `${month}-${String(lastDay).padStart(2, '0')}` }
}

export function lastMonths(count = 6) {
  const list = []
  const now = new Date()
  for (let i = count - 1; i >= 0; i--) {
    const date = new Date(now.getFullYear(), now.getMonth() - i, 1)
    list.push(`${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`)
  }
  return list
}

export function typeLabel(type) {
  return type === 'INCOME' ? '收入' : '支出'
}

export function accountTypeLabel(type) {
  return {
    CASH: '现金',
    BANK: '银行卡',
    ALIPAY: '支付宝',
    WECHAT: '微信',
    OTHER: '其他'
  }[type] || type
}

export function sourceLabel(source) {
  return {
    MANUAL: '手工录入',
    CSV: 'CSV导入',
    RESTORE: '备份恢复',
    AI: 'AI 记账'
  }[source] || '手工录入'
}
