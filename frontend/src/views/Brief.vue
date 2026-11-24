<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">智能财务简报</h2>
        <p class="page-desc">系统先在本地聚合本月与上月收支、今日与昨日支出、预算、目标与异常，再将脱敏摘要交给模型生成自然语言总结；失败时自动降级为规则简报。</p>
      </div>
      <div class="inline-form">
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" :clearable="false" style="width: 150px" />
        <el-button type="primary" :icon="MagicStick" :loading="loading" @click="generate">
          {{ loading ? '生成中…' : '生成简报' }}
        </el-button>
      </div>
    </div>

    <div class="grid grid-3">
      <div class="card feature">
        <div class="feature-icon"><el-icon><TrendCharts /></el-icon></div>
        <b>本地聚合</b>
        <span>本月与上月收支、今日与昨日支出、储蓄率、TOP3 分类、预算执行率、目标进度与异常笔数</span>
      </div>
      <div class="card feature">
        <div class="feature-icon"><el-icon><Lock /></el-icon></div>
        <b>白名单脱敏</b>
        <span>仅发送聚合后的数字与分类名，不发送账号、账户名与备注原文</span>
      </div>
      <div class="card feature">
        <div class="feature-icon"><el-icon><RefreshLeft /></el-icon></div>
        <b>稳定降级</b>
        <span>3 秒超时、未配置密钥或输出校验失败时，自动切换规则模板</span>
      </div>
    </div>

    <div v-if="brief" class="card section-gap brief-card">
      <div class="brief-head">
        <div>
          <div class="brief-title">{{ brief.period || month }} 财务简报</div>
          <div class="muted">生成时间：{{ generatedAt }}</div>
        </div>
        <div class="brief-tags">
          <el-tag :type="brief.source === 'MODEL' ? 'success' : 'info'" effect="light" class="tag-soft">
            {{ brief.source === 'MODEL' ? '模型生成' : '规则生成' }}
          </el-tag>
          <el-tag v-if="brief.degraded" type="warning" effect="light" class="tag-soft">已降级</el-tag>
        </div>
      </div>
      <el-divider />
      <div class="brief-content">{{ brief.content }}</div>
      <div class="brief-foot muted">
        简报基于当前账号在 {{ brief.period || month }} 的真实数据生成，仅作为个人理财参考，不构成投资建议。
      </div>
    </div>

    <div v-else class="card section-gap empty-card">
      <el-icon><MagicStick /></el-icon>
      <div class="empty-title">{{ loading ? '正在分析你的财务数据…' : '点击右上角生成简报' }}</div>
      <div class="muted">建议在每月月末生成，便于复盘当月消费结构与预算执行情况。</div>
    </div>

    <div class="card section-gap">
      <div class="card-title">简报包含什么</div>
      <div class="grid grid-2">
        <div class="include-item">
          <span class="dot"></span>
          <div><b>收支与储蓄</b><p>本月收入、支出、结余（存下多少钱）与储蓄率，并和上月对比增减。</p></div>
        </div>
        <div class="include-item">
          <span class="dot"></span>
          <div><b>今日与昨日</b><p>今天和昨天的支出金额与变化幅度，今天没记账会提醒你。</p></div>
        </div>
        <div class="include-item">
          <span class="dot"></span>
          <div><b>消费结构</b><p>支出 TOP3 分类及占比，识别主要花销方向。</p></div>
        </div>
        <div class="include-item">
          <span class="dot"></span>
          <div><b>预算、目标与建议</b><p>预算执行率与红黄绿分级、目标完成度、异常支出提示，末尾给 2 条具体建议。</p></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { briefApi } from '@/api'
import { currentMonth } from '@/utils/format'

const month = ref(currentMonth())
const brief = ref(null)
const loading = ref(false)
const generatedAt = ref('')

async function generate() {
  loading.value = true
  try {
    brief.value = await briefApi.generate(month.value)
    generatedAt.value = new Date().toLocaleString('zh-CN')
    if (brief.value.degraded) {
      ElMessage.info('模型不可用，已自动使用规则简报')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.feature {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feature b {
  font-size: 14px;
}

.feature span {
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--text-sub);
}

.feature-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  margin-bottom: 4px;
  background: var(--raised-bg);
  border: 1px solid var(--border);
  color: var(--text-sub);
}

.brief-card {
  padding: 26px 30px;
  background: var(--card-bg);
}

.brief-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.brief-title {
  font-size: 18px;
  font-weight: 600;
}

.brief-tags {
  display: flex;
  gap: 8px;
}

.brief-content {
  white-space: pre-line;
  line-height: 1.9;
  font-size: 14px;
}

.brief-foot {
  margin-top: 22px;
  padding-top: 14px;
  border-top: 1px dashed var(--border);
  font-size: 11.5px;
}

.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 220px;
  text-align: center;
}

.empty-card .el-icon {
  font-size: 32px;
  color: var(--text-muted);
}

.empty-title {
  font-size: 15px;
  font-weight: 600;
}

.include-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
}

.include-item .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-muted);
  margin-top: 8px;
  flex-shrink: 0;
}

.include-item b {
  font-size: 13.5px;
}

.include-item p {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: var(--text-sub);
  line-height: 1.7;
}
</style>
