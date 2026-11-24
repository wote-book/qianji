<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">使用分析</h2>
        <p class="page-desc">按模块、活跃窗口与注册 cohort 观察平台使用情况。数据均为聚合结果。</p>
      </div>
      <el-radio-group v-model="trendDays" @change="loadTrend">
        <el-radio-button :value="30">近 30 天</el-radio-button>
        <el-radio-button :value="60">近 60 天</el-radio-button>
        <el-radio-button :value="90">近 90 天</el-radio-button>
      </el-radio-group>
    </div>

    <div class="grid grid-2">
      <div class="card">
        <div class="card-title">
          <span>模块使用率</span>
          <span class="sub">近 30 天去重用户 / 启用用户</span>
        </div>
        <EChart v-if="modules.length" :option="moduleOption" height="340px" />
        <div v-else class="empty">暂无使用事件</div>
      </div>

      <div class="card">
        <div class="card-title">
          <span>活跃趋势</span>
          <span class="sub">DAU / WAU / MAU 滚动窗口</span>
        </div>
        <EChart :option="trendOption" height="340px" />
      </div>
    </div>

    <div class="card section-gap">
      <div class="card-title">
        <span>注册 cohort 留存</span>
        <span class="sub">次日 / 7 日 / 30 日产生任意使用事件</span>
      </div>
      <el-table :data="retention" size="small" empty-text="暂无足够注册数据">
        <el-table-column label="注册日期" prop="cohortDate" width="130" />
        <el-table-column label="新增用户" prop="size" width="100" align="right" />
        <el-table-column label="次日留存" min-width="160">
          <template #default="{ row }">
            <div class="rate-line">
              <div class="progress-line"><span class="bar-gold" :style="{ width: barWidth(row.d1) }" /></div>
              <b>{{ rateText(row.d1) }}</b>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="7 日留存" min-width="160">
          <template #default="{ row }">
            <div class="rate-line">
              <div class="progress-line"><span class="bar-gold" :style="{ width: barWidth(row.d7) }" /></div>
              <b>{{ rateText(row.d7) }}</b>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="30 日留存" min-width="160">
          <template #default="{ row }">
            <div class="rate-line">
              <div class="progress-line"><span class="bar-gold" :style="{ width: barWidth(row.d30) }" /></div>
              <b>{{ rateText(row.d30) }}</b>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card section-gap">
      <div class="card-title">
        <span>用户分层</span>
        <span class="sub">点击分层查看对应账号</span>
      </div>
      <div class="segment-grid">
        <div
          v-for="item in segments"
          :key="item.key"
          class="segment-card"
          :class="item.key.toLowerCase()"
          @click="openSegment(item)"
        >
          <div class="segment-label">{{ item.label }}</div>
          <div class="segment-count">{{ item.count }}<small> 人</small></div>
          <div class="segment-ratio">占比 {{ percent(item.ratio) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '@/api'
import EChart from '@/components/EChart.vue'

const router = useRouter()
const modules = ref([])
const trend = ref({ labels: [], dau: [], wau: [], mau: [] })
const retention = ref([])
const segments = ref([])
const trendDays = ref(30)

const moduleOption = computed(() => {
  const list = [...modules.value].reverse()
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = list[params[0].dataIndex]
        return `${item.label}<br/>使用率 ${item.rate}%<br/>去重用户 ${item.users} 人<br/>事件数 ${item.events}`
      }
    },
    grid: { left: 10, right: 46, top: 10, bottom: 10, containLabel: true },
    xAxis: {
      type: 'value',
      max: 100,
      axisLabel: { formatter: '{value}%', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f2f4f7' } }
    },
    yAxis: {
      type: 'category',
      data: list.map((item) => item.label || item.module),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { fontSize: 12 }
    },
    series: [
      {
        type: 'bar',
        barWidth: 16,
        itemStyle: { color: '#2b4257', borderRadius: [0, 2, 2, 0] },
        label: { show: true, position: 'right', formatter: '{c}%', fontSize: 11, color: '#6f6a61' },
        data: list.map((item) => item.rate)
      }
    ]
  }
})

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['DAU', 'WAU', 'MAU'], right: 0 },
  grid: { left: 10, right: 16, top: 44, bottom: 10, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: (trend.value.labels || []).map((label) => label.slice(5)),
    axisTick: { show: false },
    axisLabel: { fontSize: 11 }
  },
  yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#efece3' } } },
  series: [
    { name: 'DAU', type: 'line', smooth: false, symbolSize: 4, lineStyle: { width: 2, color: '#2b4257' }, itemStyle: { color: '#2b4257' }, data: trend.value.dau },
    { name: 'WAU', type: 'line', smooth: false, symbolSize: 4, lineStyle: { width: 2, color: '#b08948' }, itemStyle: { color: '#b08948' }, data: trend.value.wau },
    { name: 'MAU', type: 'line', smooth: false, symbolSize: 4, lineStyle: { width: 2, color: '#8a8578' }, itemStyle: { color: '#8a8578' }, data: trend.value.mau }
  ]
}))

function percent(value) {
  return `${Number(value || 0).toFixed(1)}%`
}

function rateText(value) {
  return value === null || value === undefined ? '—' : `${Number(value).toFixed(1)}%`
}

function barWidth(value) {
  return `${Math.min(Number(value || 0), 100)}%`
}

function openSegment(item) {
  router.push({ path: '/admin/users', query: { segment: item.key } })
}

async function loadTrend() {
  trend.value = await adminApi.usageTrend(trendDays.value)
}

async function loadAll() {
  const [moduleData, retentionData, segmentData] = await Promise.all([
    adminApi.usageModules(30),
    adminApi.retention(),
    adminApi.segments(),
    loadTrend()
  ])
  modules.value = moduleData || []
  retention.value = retentionData || []
  segments.value = segmentData || []
}

onMounted(loadAll)
</script>

<style scoped>
.rate-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rate-line .progress-line {
  flex: 1;
}

.rate-line b {
  width: 52px;
  text-align: right;
  font-size: 12.5px;
  color: var(--text-main);
  font-variant-numeric: tabular-nums;
}

.bar-gold {
  background: #b08948;
}

.segment-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.segment-card {
  border-radius: 8px;
  padding: 18px;
  cursor: pointer;
  border: 1px solid var(--border);
  background: var(--card-bg);
  transition: border-color 0.15s;
}

.segment-card:hover {
  border-color: var(--border-strong);
}

.segment-label {
  font-size: 13px;
  color: var(--text-sub);
}

.segment-count {
  font-size: 26px;
  font-weight: 600;
  color: var(--text-main);
  margin-top: 8px;
  font-variant-numeric: tabular-nums;
}

.segment-count small {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-muted);
}

.segment-ratio {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.segment-card.high {
  border-left: 2px solid #2b4257;
}

.segment-card.normal {
  border-left: 2px solid #b08948;
}

.segment-card.dormant {
  border-left: 2px solid #8a8578;
}

.segment-card.inactive {
  border-left: 2px solid #ddd6c8;
}

@media (max-width: 1100px) {
  .segment-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.grid > .card {
  margin-top: 0;
}
</style>
