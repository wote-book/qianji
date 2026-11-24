<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">预算管理</h2>
        <p class="page-desc">总预算与分类预算并行管理。可切换月份查看或设置其他月份预算，执行率基于该月实际支出计算。</p>
      </div>
      <div class="inline-form">
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" :clearable="false" style="width: 150px" @change="load" />
        <el-button type="primary" :icon="Plus" @click="openCreate">设置预算</el-button>
      </div>
    </div>

    <div class="grid grid-4">
      <div class="card stat-card">
        <div class="label">月度总预算</div>
        <div class="value">¥ {{ money(exec.limitAmt) }}</div>
        <div class="foot">{{ exec.hasBudget ? '已设置' : '尚未设置总预算' }}</div>
      </div>
      <div class="card stat-card">
        <div class="label">已支出</div>
        <div class="value money-expense">¥ {{ money(exec.usedAmt) }}</div>
        <div class="foot">{{ month }} 实际支出</div>
      </div>
      <div class="card stat-card">
        <div class="label">剩余可用</div>
        <div class="value" :class="Number(exec.remainAmt) >= 0 ? 'money-income' : 'money-expense'">
          ¥ {{ money(exec.remainAmt) }}
        </div>
        <div class="foot">预算 − 已支出</div>
      </div>
      <div class="card stat-card">
        <div class="label">执行率</div>
        <div class="value" :class="'text-' + (exec.level || 'GREEN').toLowerCase()">{{ percent(exec.ratio) }}</div>
        <div class="progress-line" style="margin-top: 12px">
          <span :class="'level-' + (exec.level || 'GREEN')" :style="{ width: Math.min(Number(exec.ratio || 0), 100) + '%' }"></span>
        </div>
        <div class="foot">{{ levelText(exec.level) }}</div>
      </div>
    </div>

    <div class="card section-gap">
      <div class="card-title">
        <span>{{ month }} 预算明细</span>
        <span class="sub">共 {{ budgets.length }} 条预算</span>
      </div>
      <el-table :data="budgets" empty-text="本月还没有预算">
        <el-table-column label="预算对象" min-width="140">
          <template #default="{ row }">
            <span v-if="row.categoryId === 0" class="total-badge">月度总预算</span>
            <span v-else>{{ row.categoryName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预算金额" align="right" width="130">
          <template #default="{ row }">¥ {{ money(row.limitAmt) }}</template>
        </el-table-column>
        <el-table-column label="已用" align="right" width="130">
          <template #default="{ row }"><span class="money-expense">¥ {{ money(row.usedAmt) }}</span></template>
        </el-table-column>
        <el-table-column label="执行率" min-width="200">
          <template #default="{ row }">
            <div class="ratio-cell">
              <div class="progress-line" style="flex: 1">
                <span :class="'level-' + row.level" :style="{ width: Math.min(Number(row.ratio || 0), 100) + '%' }"></span>
              </div>
              <span class="ratio-text">{{ percent(row.ratio) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="预警阈值" width="96" align="center">
          <template #default="{ row }">{{ row.warnThreshold }}%</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)" effect="light" size="small" class="tag-soft">{{ levelText(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openEdit(row)">调整</el-button>
            <el-button text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '调整预算' : '设置预算'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="统计月份" prop="month">
          <el-date-picker
            v-model="form.month"
            type="month"
            value-format="YYYY-MM"
            :clearable="false"
            :disabled="!!editing"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="预算对象" prop="categoryId">
          <el-select v-model="form.categoryId" :disabled="!!editing" style="width: 100%">
            <el-option label="月度总预算" :value="0" />
            <el-option v-for="item in expenseCategories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算金额" prop="limitAmt">
          <el-input-number v-model="form.limitAmt" :min="0" :precision="2" :step="100" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预警阈值">
          <el-slider v-model="form.warnThreshold" :min="1" :max="100" show-input />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { budgetApi, categoryApi } from '@/api'
import { currentMonth, money, percent } from '@/utils/format'

const month = ref(currentMonth())
const exec = ref({})
const budgets = ref([])
const expenseCategories = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const formRef = ref()

const form = reactive({ month: currentMonth(), categoryId: 0, limitAmt: 1000, warnThreshold: 80 })
const rules = {
  month: [{ required: true, message: '请选择统计月份', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择预算对象', trigger: 'change' }],
  limitAmt: [{ required: true, message: '请输入预算金额', trigger: 'blur' }]
}

function levelType(level) {
  return { GREEN: 'success', YELLOW: 'warning', RED: 'danger' }[level] || 'info'
}

function levelText(level) {
  return { GREEN: '安全', YELLOW: '接近上限', RED: '已超支' }[level] || '未设置'
}

async function load() {
  const [execution, list] = await Promise.all([
    budgetApi.execution(month.value),
    budgetApi.list(month.value)
  ])
  exec.value = execution
  budgets.value = list
}

function openCreate() {
  editing.value = null
  Object.assign(form, { month: month.value, categoryId: 0, limitAmt: 1000, warnThreshold: 80 })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, {
    month: row.month || month.value,
    categoryId: row.categoryId,
    limitAmt: Number(row.limitAmt),
    warnThreshold: row.warnThreshold
  })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await budgetApi.upsert({
      month: form.month,
      categoryId: form.categoryId,
      limitAmt: Number(form.limitAmt),
      warnThreshold: Number(form.warnThreshold)
    })
    ElMessage.success('预算已保存')
    dialogVisible.value = false
    month.value = form.month
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除「${row.categoryId === 0 ? '月度总预算' : row.categoryName}」？`, '删除预算', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await budgetApi.remove(row.id)
  ElMessage.success('预算已删除')
  await load()
}

onMounted(async () => {
  expenseCategories.value = await categoryApi.list('EXPENSE')
  await load()
})
</script>

<style scoped>
.total-badge {
  font-weight: 600;
  color: var(--text-main);
}

.ratio-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ratio-text {
  width: 58px;
  text-align: right;
  font-variant-numeric: tabular-nums;
  color: var(--text-sub);
  font-size: 12px;
}

.text-green {
  color: var(--money-income);
}

.text-yellow {
  color: var(--el-color-warning);
}

.text-red {
  color: var(--money-expense);
}
</style>
