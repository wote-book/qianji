<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">运营总览</h2>
        <p class="page-desc">平台级用户增长、活跃与功能使用概览，不含任何用户账本明细。</p>
      </div>
      <el-radio-group v-model="days" @change="load">
        <el-radio-button :value="7">近 7 天</el-radio-button>
        <el-radio-button :value="30">近 30 天</el-radio-button>
        <el-radio-button :value="90">近 90 天</el-radio-button>
      </el-radio-group>
    </div>

    <div class="grid grid-3" v-loading="loading">
      <div class="card stat-card">
        <div class="label">用户总数</div>
        <div class="value">{{ num(data.totalUsers) }}</div>
        <div class="foot">启用 {{ num(data.enabledUsers) }} · 禁用 {{ num(data.disabledUsers) }}</div>
      </div>
      <div class="card stat-card">
        <div class="label">今日活跃用户</div>
        <div class="value">{{ num(data.todayActiveUsers) }}</div>
        <div class="foot">当日登录或产生任意操作</div>
      </div>
      <div class="card stat-card">
        <div class="label">近 7 日新增用户</div>
        <div class="value">{{ num(data.newUsers7d) }}</div>
        <div class="foot">含今日注册</div>
      </div>
      <div class="card stat-card">
        <div class="label">本月流水笔数</div>
        <div class="value">{{ num(data.monthRecordCount) }}</div>
        <div class="foot">仅统计笔数，不开放明细</div>
      </div>
      <div class="card stat-card">
        <div class="label">本月智能简报调用</div>
        <div class="value">{{ num(data.monthBriefCount) }}</div>
        <div class="foot">AI 简报生成次数</div>
      </div>
      <div class="card stat-card">
        <div class="label">本月活跃率</div>
        <div class="value">{{ percent(data.monthActiveRate) }}</div>
        <div class="foot">活跃用户 / 启用用户</div>
      </div>
    </div>

    <div class="card section-gap">
      <div class="card-title">
        <span>用户趋势</span>
        <span class="sub">活跃用户与新增用户 · 近 {{ days }} 天</span>
      </div>
      <EChart :option="trendOption" height="330px" />
    </div>

    <div class="grid grid-2 section-gap">
      <div class="card">
        <div class="card-title">
          <span>功能使用 TOP</span>
          <span class="sub">按事件数排序</span>
        </div>
        <EChart v-if="data.modules?.length" :option="moduleOption" height="320px" />
        <div v-else class="empty">暂无功能使用数据</div>
      </div>

      <div class="card">
        <div class="card-title">
          <span>用户分层</span>
          <span class="sub">近 7 / 14 天活跃口径</span>
        </div>
        <EChart v-if="segmentTotal > 0" :option="segmentOption" height="320px" />
        <div v-else class="empty">暂无用户分层数据</div>
      </div>
    </div>

    <div class="card section-gap">
      <div class="card-title">
        <span>最近登录记录</span>
        <span class="sub">最新 5 条</span>
      </div>
      <el-table :data="data.recentLogins || []" size="small" empty-text="暂无登录记录">
        <el-table-column label="时间" min-width="170">
          <template #default="{ row }">{{ datetime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="用户名" prop="username" min-width="120" />
        <el-table-column label="IP" prop="ip" min-width="130" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" effect="light" size="small" class="tag-soft">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '@/api'
import EChart from '@/components/EChart.vue'

const loading = ref(false)
const days = ref(30)
const data = ref({})

const segmentTotal = computed(() => (data.value.segments || []).reduce((sum, item) => sum + Number(item.count || 0), 0))

const trendOption = computed(() => {
  const trend = data.value.trend || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['活跃用户', '新增用户'], right: 0 },
    grid: { left: 10, right: 18, top: 44, bottom: 10, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.map((item) => item.date.slice(5)),
      axisTick: { show: false },
      axisLabel: { fontSize: 11 }
    },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f2f4f7' } } },
    series: [
      {
        name: '活跃用户',
        type: 'line',
        smooth: true,
        symbolSize: 5,
        lineStyle: { width: 2, color: '#2b4257' },
        itemStyle: { color: '#2b4257' },
        areaStyle: { color: 'rgba(43,66,87,0.06)' },
        data: trend.map((item) => item.active)
      },
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        symbolSize: 5,
        lineStyle: { width: 2, color: '#4f7c66' },
        itemStyle: { color: '#4f7c66' },
        data: trend.map((item) => item.newUsers)
      }
    ]
  }
})

const moduleOption = computed(() => {
  const modules = [...(data.value.modules || [])].slice(0, 8).reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 30, top: 10, bottom: 10, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#f2f4f7' } } },
    yAxis: {
      type: 'category',
      data: modules.map((item) => item.label || item.module),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { fontSize: 12 }
    },
    series: [
      {
        type: 'bar',
        barWidth: 16,
        itemStyle: { color: '#2b4257', borderRadius: [0, 2, 2, 0] },
        label: { show: true, position: 'right', fontSize: 11, color: '#6f6a61' },
        data: modules.map((item) => item.events)
      }
    ]
  }
})

const segmentOption = computed(() => {
  const palette = { HIGH: '#2b4257', NORMAL: '#b08948', DORMANT: '#8a8578', INACTIVE: '#ddd6c8' }
  return {
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 人（{d}%）' },
    legend: { bottom: 0, icon: 'circle', textStyle: { fontSize: 12 } },
    series: [
      {
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 8 },
        label: { show: false },
        emphasis: { label: { show: true, fontWeight: 'bold', formatter: '{b}\n{d}%' } },
        data: (data.value.segments || []).map((item) => ({
          name: item.label,
          value: item.count,
          itemStyle: { color: palette[item.key] || '#b08948' }
        }))
      }
    ]
  }
})

function num(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function percent(value) {
  return `${Number(value || 0).toFixed(1)}%`
}

function datetime(value) {
  return value ? String(value).replace('T', ' ') : '—'
}

async function load() {
  loading.value = true
  try {
    data.value = await adminApi.overview(days.value)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.grid > .card {
  margin-top: 0;
}
</style>
