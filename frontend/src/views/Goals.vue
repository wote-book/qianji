<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">理财目标</h2>
        <p class="page-desc">目标采用手工记录口径：存入只更新目标进度，不产生流水、不影响账户余额与收支统计。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建目标</el-button>
    </div>

    <div class="grid grid-3">
      <div v-for="goal in goals" :key="goal.id" class="card goal-card">
        <div class="goal-top">
          <div class="goal-name">{{ goal.name }}</div>
          <el-tag :type="goal.status === 'DONE' ? 'success' : 'primary'" effect="light" size="small" class="tag-soft">
            {{ goal.status === 'DONE' ? '已达成' : '进行中' }}
          </el-tag>
        </div>
        <div class="goal-amount">
          <b>¥ {{ money(goal.savedAmt) }}</b>
          <span>/ ¥ {{ money(goal.targetAmt) }}</span>
        </div>
        <div class="progress-line">
          <span :style="{ width: Math.min(Number(goal.percent || 0), 100) + '%', background: goal.status === 'DONE' ? 'var(--money-income)' : 'var(--el-color-primary)' }"></span>
        </div>
        <div class="goal-meta">
          <span>完成度 {{ percent(goal.percent) }}</span>
          <span>开始于 {{ goal.startDate }}</span>
        </div>
        <div class="goal-estimate">
          <template v-if="goal.status === 'DONE'">目标已达成，继续保持储蓄习惯</template>
          <template v-else-if="goal.expectedDate">按当前日均储蓄，预计 {{ goal.expectedDate }} 达成（还需 {{ goal.remainDays }} 天）</template>
          <template v-else>存入第一笔后即可预测达成时间</template>
        </div>
        <div class="goal-actions">
          <el-button type="primary" plain size="small" :disabled="goal.status === 'DONE'" @click="openDeposit(goal)">
            存入一笔
          </el-button>
          <el-dropdown trigger="click" @command="(cmd) => onCommand(cmd, goal)">
            <el-button size="small">更多<el-icon style="margin-left: 4px"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">编辑目标</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除目标</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div v-if="!goals.length" class="card empty-card">
        <el-icon><Aim /></el-icon>
        <div>还没有理财目标，先给未来定一个小目标</div>
        <el-button type="primary" plain size="small" @click="openCreate">新建目标</el-button>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑目标' : '新建目标'" width="420px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="目标名称" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="例如：旅行基金、应急备用金" />
        </el-form-item>
        <el-form-item label="目标金额" prop="targetAmt">
          <el-input-number v-model="form.targetAmt" :min="1" :precision="2" :step="1000" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="!editing" label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="depositVisible" title="存入目标" width="400px">
      <div class="deposit-goal">
        <div class="muted">目标</div>
        <b>{{ depositGoal?.name }}</b>
        <div class="muted" style="margin-top: 6px">
          还差 ¥ {{ money(Number(depositGoal?.targetAmt || 0) - Number(depositGoal?.savedAmt || 0)) }}
        </div>
      </div>
      <el-form label-width="72px" style="margin-top: 16px">
        <el-form-item label="存入金额">
          <el-input-number v-model="depositAmount" :min="0.01" :precision="2" :step="100" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <div class="muted">提示：目标储蓄为手工记录，不产生收支流水。</div>
      <template #footer>
        <el-button @click="depositVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doDeposit">确认存入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { goalApi } from '@/api'
import { money, percent, today } from '@/utils/format'

const goals = ref([])
const dialogVisible = ref(false)
const depositVisible = ref(false)
const editing = ref(null)
const depositGoal = ref(null)
const depositAmount = ref(1000)
const saving = ref(false)
const formRef = ref()

const form = reactive({ name: '', targetAmt: 10000, startDate: today() })
const rules = {
  name: [{ required: true, message: '请输入目标名称', trigger: 'blur' }],
  targetAmt: [{ required: true, message: '请输入目标金额', trigger: 'blur' }]
}

async function load() {
  goals.value = await goalApi.list()
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', targetAmt: 10000, startDate: today() })
  dialogVisible.value = true
}

function openEdit(goal) {
  editing.value = goal
  Object.assign(form, { name: goal.name, targetAmt: Number(goal.targetAmt), startDate: goal.startDate })
  dialogVisible.value = true
}

function openDeposit(goal) {
  depositGoal.value = goal
  depositAmount.value = 1000
  depositVisible.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { name: form.name, targetAmt: Number(form.targetAmt), startDate: form.startDate }
    if (editing.value) {
      await goalApi.update(editing.value.id, payload)
      ElMessage.success('目标已更新')
    } else {
      await goalApi.create(payload)
      ElMessage.success('目标已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function doDeposit() {
  if (!depositAmount.value || depositAmount.value <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }
  saving.value = true
  try {
    await goalApi.deposit(depositGoal.value.id, Number(depositAmount.value))
    ElMessage.success('已存入目标')
    depositVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onCommand(command, goal) {
  if (command === 'edit') {
    openEdit(goal)
    return
  }
  await ElMessageBox.confirm(`确认删除目标「${goal.name}」？`, '删除目标', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await goalApi.remove(goal.id)
  ElMessage.success('目标已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.goal-amount {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.goal-amount b {
  font-size: 26px;
  letter-spacing: -0.5px;
}

.goal-amount span {
  font-size: 12.5px;
  color: var(--text-sub);
}

.goal-estimate {
  font-size: 12px;
  line-height: 1.7;
  color: #475467;
  background: var(--raised-bg);
  border-radius: 10px;
  padding: 10px 12px;
  min-height: 62px;
  display: flex;
  align-items: center;
}

.goal-actions {
  display: flex;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 6px;
}

.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 240px;
  color: var(--text-sub);
  font-size: 13px;
}

.empty-card .el-icon {
  font-size: 32px;
  color: var(--text-muted);
}

.deposit-goal {
  background: var(--raised-bg);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 14px 16px;
}
</style>
