<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">统计分析</h2>
        <p class="page-desc">分类占比、收支趋势与规则洞察。点击饼图扇区可直接下钻到对应流水。</p>
      </div>
      <div class="inline-form">
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" :clearable="false" style="width: 150px" @change="onMonthChange" />
        <el-radio-group v-model="pieType" @change="loadCategory">
          <el-radio-button value="EXPENSE">支出</el-radio-button>
          <el-radio-button value="INCOME">收入</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="grid grid-3">
      <div class="card stat-card">
        <div class="label">本月收入</div>
        <div class="value money-income">¥ {{ money(summary.income) }}</div>
      </div>
      <div class="card stat-card">
        <div class="label">本月支出</div>
        <div class="value money-expense">¥ {{ money(summary.expense) }}</div>
      </div>
      <div class="card stat-card">
        <div class="label">本月结余</div>
        <div class="value" :class="Number(summary.net) >= 0 ? 'money-income' : 'money-expense'">¥ {{ money(summary.net) }}</div>
      </div>
    </div>

    <div class="grid grid-2 section-gap">
      <div class="card">
        <div class="card-title">
          <span>{{ pieType === 'EXPENSE' ? '支出' : '收入' }}分类占比</span>
          <span class="sub">总计 ¥ {{ money(category.total) }}</span>
        </div>
        <EChart v-if="category.list?.length" :option="pieOption" height="340px" @click="onPieClick" />
        <div v-else class="empty">本月暂无{{ pieType === 'EXPENSE' ? '支出' : '收入' }}数据</div>
      </div>

      <div class="card">
        <div class="card-title">
          <span>分类明细</span>
          <span class="sub">占比合计 100%</span>
        </div>
        <el-table :data="category.list || []" size="small" empty-text="暂无数据">
          <el-table-column label="分类" prop="categoryName" />
          <el-table-column label="金额" align="right" width="130">
            <template #default="{ row }">¥ {{ money(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="占比" width="90" align="right">
            <template #default="{ row }">{{ percent(row.ratio) }}</template>
          </el-table-column>
          <el-table-column label="" width="86" align="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="drill(row)">流水</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="card section-gap">
      <div class="card-title">
        <span>收支趋势</span>
        <div class="inline-form">
          <el-radio-group v-model="granularity" size="small" @change="loadTrend">
            <el-radio-button value="day">按日</el-radio-button>
            <el-radio-button value="month">按月</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-model="range"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 250px"
            @change="loadTrend"
          />
        </div>
      </div>
      <EChart :option="trendOption" height="330px" />
    </div>

    <div class="grid grid-2 section-gap">
      <div class="card">
        <div class="card-title">
          <span>异常大额支出</span>
          <span class="sub">近 6 个月均值 + 2σ</span>
        </div>
        <el-table :data="anomalies" size="small" empty-text="本月未发现异常大额支出">
          <el-table-column label="日期" prop="recordDate" width="108" />
          <el-table-column label="分类" prop="categoryName" width="88" />
          <el-table-column label="金额" align="right" width="110">
            <template #default="{ row }"><span class="money-expense">¥ {{ money(row.amount) }}</span></template>
          </el-table-column>
          <el-table-column label="均值" align="right" width="100">
            <template #default="{ row }">¥ {{ money(row.average) }}</template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" show-overflow-tooltip />
        </el-table>
      </div>

      <div class="card">
        <div class="card-title">
          <span>疑似固定订阅</span>
          <span class="sub">≥3 个月、金额一致、日期偏差 ≤3 天</span>
        </div>
        <el-table :data="subscriptions" size="small" empty-text="未识别到固定订阅">
          <el-table-column label="分类" prop="categoryName" width="100" />
          <el-table-column label="金额" align="right" width="110">
            <template #default="{ row }">¥ {{ money(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="连续月数" width="90" align="center">
            <template #default="{ row }">{{ row.months }} 个月</template>
          </el-table-column>
          <el-table-column label="建议" prop="suggestion" show-overflow-tooltip />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { insightApi, statsApi } from '@/api'
import EChart from '@/components/EChart.vue'
import { currentMonth, money, monthRange, percent } from '@/utils/format'

const router = useRouter()
const month = ref(currentMonth())
const pieType = ref('EXPENSE')
const summary = ref({})
const category = ref({ list: [] })
const granularity = ref('day')
const range = ref([monthRange(currentMonth()).start, monthRange(currentMonth()).end])
const trend = ref({ labels: [], income: [], expense: [] })
const anomalies = ref([])
const subscriptions = ref([])

const palette = ['#2b4257', '#a8503c', '#4f7c66', '#b08948', '#6b8299', '#8a6f5a', '#7d8a7a', '#b3603f', '#5f6b73', '#96755f']

const pieOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>¥{c}（{d}%）' },
  legend: { type: 'scroll', bottom: 0, left: 'center', icon: 'circle', textStyle: { fontSize: 11 } },
  series: [
    {
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 2, borderColor: '#fffdf8', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 13, fontWeight: '600', formatter: '{b}\n{d}%' } },
      data: (category.value.list || []).map((item, index) => ({
        name: item.categoryName,
        value: Number(item.amount),
        categoryId: item.categoryId,
        itemStyle: { color: palette[index % palette.length] }
      }))
    }
  ]
}))

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['收入', '支出'], right: 0 },
  grid: { left: 10, right: 16, top: 44, bottom: 10, containLabel: true },
  xAxis: {
    type: 'category',
    data: trend.value.labels,
    axisTick: { show: false },
    axisLabel: { rotate: granularity.value === 'day' ? 45 : 0, fontSize: 11 }
  },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#efece3' } } },
  series: [
    { name: '收入', type: 'line', smooth: false, symbolSize: 4, lineStyle: { width: 2, color: '#4f7c66' }, itemStyle: { color: '#4f7c66' }, areaStyle: { color: 'rgba(79,124,102,0.06)' }, data: trend.value.income },
    { name: '支出', type: 'line', smooth: false, symbolSize: 4, lineStyle: { width: 2, color: '#a8503c' }, itemStyle: { color: '#a8503c' }, areaStyle: { color: 'rgba(168,80,60,0.05)' }, data: trend.value.expense }
  ]
}))

async function loadSummary() {
  summary.value = await statsApi.summary(month.value)
}

async function loadCategory() {
  category.value = await statsApi.category(month.value, pieType.value)
}

async function loadTrend() {
  if (!range.value?.[0] || !range.value?.[1]) return
  trend.value = await statsApi.trend({
    start: range.value[0],
    end: range.value[1],
    granularity: granularity.value
  })
}

async function loadInsights() {
  ;[anomalies.value, subscriptions.value] = await Promise.all([
    insightApi.anomalies(month.value),
    insightApi.subscriptions()
  ])
}

function onMonthChange() {
  const { start, end } = monthRange(month.value)
  range.value = [start, end]
  loadSummary()
  loadCategory()
  loadTrend()
  loadInsights()
}

function onPieClick(params) {
  if (params.data?.categoryId) {
    drill({ categoryId: params.data.categoryId })
  }
}

function drill(row) {
  const { start, end } = monthRange(month.value)
  router.push({
    path: '/records',
    query: {
      categoryId: row.categoryId,
      type: pieType.value,
      start,
      end
    }
  })
}

onMounted(async () => {
  await Promise.all([loadSummary(), loadCategory(), loadTrend(), loadInsights()])
})
</script>
