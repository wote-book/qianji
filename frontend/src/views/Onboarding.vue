<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">新手引导</h2>
        <p class="page-desc">完成五步，搭建属于你的理财工作台。每一步都可以随时返回补充。</p>
      </div>
      <el-button type="primary" @click="complete">完成引导，进入看板</el-button>
    </div>

    <div class="card">
      <div class="card-title">
        <span>准备进度</span>
        <span class="sub">{{ doneCount }} / {{ steps.length }} 已完成</span>
      </div>
      <el-progress :percentage="progress" :stroke-width="12" :show-text="false" />
      <div class="grid grid-3" style="margin-top: 20px">
        <div v-for="(step, index) in steps" :key="step.key" class="step-card" :class="{ done: step.done }">
          <div class="step-index">
            <el-icon v-if="step.done"><CircleCheckFilled /></el-icon>
            <span v-else>{{ index + 1 }}</span>
          </div>
          <div class="step-body">
            <div class="step-title">{{ step.title }}</div>
            <div class="step-desc">{{ descriptions[step.key] }}</div>
          </div>
          <el-button text type="primary" @click="go(step.key)">
            {{ step.done ? '查看' : '去完成' }}
          </el-button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-title">使用建议</div>
      <div class="tips">
        <div class="tip-item">
          <el-icon><Wallet /></el-icon>
          <div>
            <b>先建账户，再记账</b>
            <p>账户是余额的唯一载体。新建账户时填写的初始余额会作为期初值，之后只由流水驱动。</p>
          </div>
        </div>
        <div class="tip-item">
          <el-icon><Histogram /></el-icon>
          <div>
            <b>预算只看支出</b>
            <p>月度总预算与分类预算并行，执行率实时按当月支出聚合，红黄绿三色自动分级。</p>
          </div>
        </div>
        <div class="tip-item">
          <el-icon><MagicStick /></el-icon>
          <div>
            <b>简报可离线降级</b>
            <p>智能简报优先调用模型；未配置密钥或调用失败时自动使用规则模板，功能始终可用。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { onboardingApi } from '@/api'

const router = useRouter()
const steps = ref([])

const descriptions = {
  account: '新建至少一个现金、银行卡或第三方支付账户',
  category: '注册时已自动创建 12 个预设分类，也可自行增删',
  record: '记录第一笔收入或支出，余额会自动更新',
  budget: '设置本月总预算或分类预算，掌握消费节奏',
  goal: '建立一个储蓄目标，记录手工存入进度'
}

const routes = {
  account: '/accounts',
  category: '/categories',
  record: '/records',
  budget: '/budgets',
  goal: '/goals'
}

const doneCount = computed(() => steps.value.filter((s) => s.done).length)
const progress = computed(() => (steps.value.length ? Math.round((doneCount.value / steps.value.length) * 100) : 0))

function go(key) {
  router.push(routes[key] || '/dashboard')
}

async function load() {
  const data = await onboardingApi.status()
  steps.value = data.steps || []
}

async function complete() {
  await onboardingApi.complete()
  ElMessage.success('引导已完成，开始记账吧')
  router.push('/dashboard')
}

onMounted(load)
</script>

<style scoped>
.step-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--card-bg);
  transition: border-color 0.15s;
}

.step-card.done {
  background: var(--raised-bg);
  border-color: var(--border-strong);
}

.step-card:hover {
  border-color: var(--border-strong);
}

.step-index {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.step-card.done .step-index {
  background: #e8f0ea;
  color: var(--money-income);
  font-size: 18px;
}

.step-body {
  flex: 1;
  min-width: 0;
}

.step-title {
  font-weight: 600;
  font-size: 14px;
}

.step-desc {
  font-size: 12px;
  color: var(--text-sub);
  margin-top: 4px;
  line-height: 1.6;
}

.tips {
  display: grid;
  gap: 16px;
}

.tip-item {
  display: flex;
  gap: 14px;
  padding: 16px;
  background: var(--raised-bg);
  border: 1px solid var(--border);
  border-radius: 8px;
}

.tip-item > .el-icon {
  font-size: 18px;
  color: var(--text-sub);
  margin-top: 2px;
}

.tip-item b {
  font-size: 14px;
}

.tip-item p {
  margin: 4px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--text-sub);
}
</style>
