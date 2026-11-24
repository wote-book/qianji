<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">你好，{{ store.nickname }} 👋</h2>
        <p class="page-desc">这是 {{ month }} 的财务概览，所有数字均由实时流水聚合。</p>
      </div>
      <div class="inline-form">
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" :clearable="false" style="width: 150px" @change="load" />
        <el-button type="primary" :icon="Plus" @click="router.push('/records?create=1')">记一笔</el-button>
      </div>
    </div>

    <div class="card section-gap quick-card">
      <div class="card-title">
        <span>功能入口</span>
        <span class="sub">记账 · 规划 · 分析，一站完成</span>
      </div>
      <div class="quick-actions">
        <div class="quick-action" @click="router.push('/records?create=1')">
          <span class="icon"><el-icon><EditPen /></el-icon></span>
          <div><div class="name">记一笔</div><div class="desc">记录收入或支出</div></div>
        </div>
        <div class="quick-action" @click="router.push('/records')">
          <span class="icon"><el-icon><Tickets /></el-icon></span>
          <div><div class="name">收支流水</div><div class="desc">筛选、编辑、删除</div></div>
        </div>
        <div class="quick-action" @click="router.push('/accounts')">
          <span class="icon"><el-icon><CreditCard /></el-icon></span>
          <div><div class="name">账户与对账</div><div class="desc">余额一致性检查</div></div>
        </div>
        <div class="quick-action" @click="router.push('/budgets')">
          <span class="icon"><el-icon><Wallet /></el-icon></span>
          <div><div class="name">预算管理</div><div class="desc">总预算与分类预算</div></div>
        </div>
        <div class="quick-action" @click="router.push('/stats')">
          <span class="icon"><el-icon><PieChart /></el-icon></span>
          <div><div class="name">统计分析</div><div class="desc">趋势与分类占比</div></div>
        </div>
        <div class="quick-action" @click="router.push('/goals')">
          <span class="icon"><el-icon><Aim /></el-icon></span>
          <div><div class="name">理财目标</div><div class="desc">攒钱进度跟踪</div></div>
        </div>
        <div class="quick-action" @click="router.push('/reports')">
          <span class="icon"><el-icon><Download /></el-icon></span>
          <div><div class="name">报表与备份</div><div class="desc">Excel / PDF / 备份</div></div>
        </div>
        <div class="quick-action" @click="router.push('/brief')">
          <span class="icon"><el-icon><MagicStick /></el-icon></span>
          <div><div class="name">智能简报</div><div class="desc">一键生成月报</div></div>
        </div>
      </div>
    </div>

    <div class="grid grid-4">
      <div class="card stat-card">
        <div class="label">
          <span class="stat-icon"><el-icon><Top /></el-icon></span>
          本月收入
        </div>
        <div class="value money-income">¥ {{ money(summary.income) }}</div>
        <div class="foot">统计区间：{{ month }}</div>
      </div>
      <div class="card stat-card">
        <div class="label">
          <span class="stat-icon"><el-icon><Bottom /></el-icon></span>
          本月支出
        </div>
        <div class="value money-expense">¥ {{ money(summary.expense) }}</div>
        <div class="foot">共 {{ recent.length }} 笔最近流水可查看</div>
      </div>
      <div class="card stat-card">
        <div class="label">
          <span class="stat-icon"><el-icon><Coin /></el-icon></span>
          本月结余
        </div>
        <div class="value" :class="Number(summary.net) >= 0 ? 'money-income' : 'money-expense'">
          ¥ {{ money(summary.net) }}
        </div>
        <div class="foot">收入 − 支出</div>
      </div>
      <div class="card stat-card">
        <div class="label">
          <span class="stat-icon"><el-icon><Wallet /></el-icon></span>
          账户总资产
        </div>
        <div class="value">¥ {{ money(summary.totalBalance) }}</div>
        <div class="foot">全部账户余额之和</div>
      </div>
    </div>

    <div class="grid grid-2 section-gap">
      <div class="card">
        <div class="card-title">
          <span>预算执行</span>
          <el-button text type="primary" @click="router.push('/budgets')">去设置</el-button>
        </div>
        <template v-if="budget.hasBudget">
          <div class="budget-head">
            <div>
              <div class="muted">本月总预算</div>
              <div class="budget-value">¥ {{ money(budget.limitAmt) }}</div>
            </div>
            <el-tag :type="levelType(budget.level)" effect="light" class="tag-soft">{{ levelText(budget.level) }}</el-tag>
          </div>
          <div class="progress-line" style="margin: 16px 0 10px">
            <span :class="'level-' + budget.level" :style="{ width: Math.min(Number(budget.ratio || 0), 100) + '%' }"></span>
          </div>
          <div class="budget-foot">
            <span>已用 ¥ {{ money(budget.usedAmt) }}</span>
            <span>执行率 {{ percent(budget.ratio) }}</span>
            <span>剩余 ¥ {{ money(budget.remainAmt) }}</span>
          </div>
          <div class="budget-list" v-if="budget.budgets?.length">
            <div v-for="item in categoryBudgets" :key="item.id" class="budget-item">
              <span class="budget-name">{{ item.categoryName }}</span>
              <div class="progress-line mini">
                <span :class="'level-' + item.level" :style="{ width: Math.min(Number(item.ratio || 0), 100) + '%' }"></span>
              </div>
              <span class="budget-num">{{ percent(item.ratio) }}</span>
            </div>
          </div>
        </template>
        <div v-else class="empty">
          还没有设置 {{ month }} 的总预算
          <div style="margin-top: 12px">
            <el-button type="primary" plain size="small" @click="router.push('/budgets')">立即设置</el-button>
          </div>
        </div>
      </div>

      <div class="card quota-card">
        <div class="card-title">
          <span>每日可花额度</span>
          <el-tooltip content="按本月总预算减去已支出，再除以剩余天数计算" placement="top">
            <el-icon class="muted"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
        <template v-if="quota.hasBudget">
          <div class="quota-value">¥ {{ money(quota.dailyQuota) }}</div>
          <div class="quota-desc">{{ quota.message }}</div>
          <div class="quota-grid">
            <div>
              <span>本月剩余</span>
              <b>¥ {{ money(quota.remainAmt) }}</b>
            </div>
            <div>
              <span>剩余天数</span>
              <b>{{ quota.remainDays }} 天</b>
            </div>
            <div>
              <span>已支出</span>
              <b>¥ {{ money(quota.expense) }}</b>
            </div>
          </div>
        </template>
        <div v-else class="empty">{{ quota.message || '设置本月总预算后可用' }}</div>
      </div>
    </div>

    <div class="card section-gap">
      <div class="card-title">
        <span>近 6 个月收支趋势</span>
        <span class="sub">单位：元</span>
      </div>
      <EChart :option="trendOption" height="300px" />
    </div>

    <div class="grid grid-2 section-gap">
      <div class="card">
        <div class="card-title">
          <span>最近流水</span>
          <el-button text type="primary" @click="router.push('/records')">查看全部</el-button>
        </div>
        <el-table :data="recent" size="small" empty-text="本月还没有流水">
          <el-table-column label="日期" prop="recordDate" width="110" />
          <el-table-column label="分类" prop="categoryName" width="90" />
          <el-table-column label="账户" prop="accountName" width="110" show-overflow-tooltip />
          <el-table-column label="金额" align="right">
            <template #default="{ row }">
              <span :class="row.type === 'INCOME' ? 'money-income' : 'money-expense'">
                {{ row.type === 'INCOME' ? '+' : '-' }}{{ money(row.amount) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card">
        <div class="card-title">
          <span>理财目标</span>
          <el-button text type="primary" @click="router.push('/goals')">管理目标</el-button>
        </div>
        <template v-if="goals.length">
          <div v-for="goal in goals.slice(0, 3)" :key="goal.id" class="goal-row">
            <div class="goal-row-top">
              <span>{{ goal.name }}</span>
              <span class="muted">{{ percent(goal.percent) }}</span>
            </div>
            <div class="progress-line">
              <span :style="{ width: Math.min(Number(goal.percent || 0), 100) + '%', background: 'var(--el-color-primary)' }"></span>
            </div>
            <div class="goal-row-foot">
              <span>¥ {{ money(goal.savedAmt) }} / {{ money(goal.targetAmt) }}</span>
              <span v-if="goal.expectedDate">预计 {{ goal.expectedDate }}</span>
            </div>
          </div>
        </template>
        <div v-else class="empty">还没有理财目标，去建立一个吧</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { statsApi } from '@/api'
import { useUserStore } from '@/stores/user'
import EChart from '@/components/EChart.vue'
import { currentMonth, lastMonths, money, percent } from '@/utils/format'

const router = useRouter()
const store = useUserStore()
const month = ref(currentMonth())
const summary = ref({})
const quota = ref({})
const budget = ref({})
const goals = ref([])
const recent = ref([])
const trend = ref({ labels: [], income: [], expense: [] })

const categoryBudgets = computed(() => (budget.value.budgets || []).filter((b) => b.categoryId !== 0))

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['收入', '支出'], right: 0, top: 0 },
  grid: { left: 10, right: 10, top: 40, bottom: 10, containLabel: true },
  xAxis: { type: 'category', data: trend.value.labels, axisTick: { show: false }, axisLine: { lineStyle: { color: '#e0dbcf' } } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#efece3' } } },
  series: [
    {
      name: '收入',
      type: 'bar',
      barWidth: 14,
      itemStyle: { color: '#4f7c66', borderRadius: [2, 2, 0, 0] },
      data: trend.value.income
    },
    {
      name: '支出',
      type: 'line',
      smooth: false,
      symbolSize: 5,
      lineStyle: { width: 2, color: '#a8503c' },
      itemStyle: { color: '#a8503c' },
      areaStyle: { color: 'rgba(168,80,60,0.05)' },
      data: trend.value.expense
    }
  ]
}))

function levelType(level) {
  return { GREEN: 'success', YELLOW: 'warning', RED: 'danger' }[level] || 'info'
}

function levelText(level) {
  return { GREEN: '安全', YELLOW: '接近上限', RED: '已超支' }[level] || '未设置'
}

async function load() {
  const data = await statsApi.dashboard(month.value)
  summary.value = data.summary || {}
  quota.value = data.quota || {}
  budget.value = data.budget || {}
  goals.value = data.goals || []
  recent.value = data.recent || []

  const months = lastMonths(6)
  trend.value = await statsApi.trend({
    start: `${months[0]}-01`,
    end: new Date().toISOString().slice(0, 10),
    granularity: 'month'
  })
}

onMounted(load)
onMounted(() => window.addEventListener('finance:records-changed', load))
onBeforeUnmount(() => window.removeEventListener('finance:records-changed', load))
</script>

<style scoped>
.quick-card .quick-actions {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 1200px) {
  .quick-card .quick-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.budget-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.budget-value {
  font-size: 26px;
  font-weight: 700;
  margin-top: 6px;
}

.budget-foot {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-sub);
}

.budget-list {
  margin-top: 18px;
  display: grid;
  gap: 10px;
}

.budget-item {
  display: grid;
  grid-template-columns: 90px 1fr 56px;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-sub);
}

.progress-line.mini {
  height: 6px;
}

.budget-num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.quota-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-main);
  letter-spacing: 0;
  margin-top: 8px;
  font-variant-numeric: tabular-nums;
}

.quota-desc {
  font-size: 12px;
  color: var(--text-sub);
  margin-top: 4px;
}

.quota-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 26px;
}

.quota-grid > div {
  background: var(--raised-bg);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quota-grid span {
  font-size: 11.5px;
  color: var(--text-sub);
}

.quota-grid b {
  font-size: 14px;
}

.goal-row {
  padding: 12px 0;
  border-bottom: 1px dashed var(--border);
}

.goal-row:last-child {
  border-bottom: none;
}

.goal-row-top {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.goal-row-foot {
  display: flex;
  justify-content: space-between;
  font-size: 11.5px;
  color: var(--text-sub);
  margin-top: 6px;
}
</style>
