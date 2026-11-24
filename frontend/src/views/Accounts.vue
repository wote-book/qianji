<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">账户与对账</h2>
        <p class="page-desc">账户余额只随流水原子变化；对账用于检查「期初 + 流水」与实际余额是否一致。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建账户</el-button>
    </div>

    <div class="grid grid-3">
      <div v-for="item in accounts" :key="item.id" class="card account-card">
        <div class="acc-top">
          <div class="acc-icon" :class="'type-' + item.type">
            <el-icon><CreditCard /></el-icon>
          </div>
          <el-dropdown trigger="click" @command="(cmd) => onCommand(cmd, item)">
            <el-icon class="more"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">编辑账户</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除账户</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="acc-name">{{ item.accName }}</div>
        <div class="acc-type">
          {{ accountTypeLabel(item.type) }}
          <el-tag v-if="item.allowOverdraft === 1" size="small" type="warning" effect="light" class="tag-soft">允许透支</el-tag>
        </div>
        <div class="acc-balance">¥ {{ money(item.balance) }}</div>
        <div class="acc-foot">
          <span>期初 ¥ {{ money(item.initBalance) }}</span>
          <span>余额实时</span>
        </div>
      </div>
      <div v-if="!accounts.length" class="card empty-card">
        <el-icon><Wallet /></el-icon>
        <div>还没有账户，先创建一个开始记账</div>
        <el-button type="primary" plain size="small" @click="openCreate">新建账户</el-button>
      </div>
    </div>

    <div class="card section-gap">
      <div class="card-title">
        <span>余额一致性对账</span>
        <div class="inline-form">
          <el-button :loading="checking" @click="reconcile">检查对账</el-button>
          <el-button type="primary" plain :disabled="!hasDiff" :loading="fixing" @click="fix">一键修复</el-button>
        </div>
      </div>
      <div class="muted" style="margin-bottom: 14px">
        重算口径：期初余额 + 收入 − 支出。正常情况下差异应为 0；如出现差异可一键修复并记录日志。
      </div>
      <el-table :data="diffs" empty-text="点击「检查对账」查看结果">
        <el-table-column label="账户" prop="accName" min-width="140" />
        <el-table-column label="期初余额" align="right">
          <template #default="{ row }">¥ {{ money(row.initBalance) }}</template>
        </el-table-column>
        <el-table-column label="当前余额" align="right">
          <template #default="{ row }">¥ {{ money(row.balance) }}</template>
        </el-table-column>
        <el-table-column label="重算余额" align="right">
          <template #default="{ row }">¥ {{ money(row.calc) }}</template>
        </el-table-column>
        <el-table-column label="差异" align="right">
          <template #default="{ row }">
            <b :class="Number(row.diff) === 0 ? 'money-income' : 'money-expense'">¥ {{ money(row.diff) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="Number(row.diff) === 0 ? 'success' : 'danger'" effect="light" size="small" class="tag-soft">
              {{ Number(row.diff) === 0 ? '一致' : '不一致' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card section-gap">
      <div class="card-title">余额规则说明</div>
      <div class="rules">
        <div><el-icon><Lock /></el-icon>余额不可直接编辑，只能通过流水或对账修复改变。</div>
        <div><el-icon><CircleCheck /></el-icon>支出使用条件更新：余额不足且不允许透支时整笔失败，不产生半条数据。</div>
        <div><el-icon><RefreshLeft /></el-icon>修改流水会先回滚旧影响，再应用新影响；跨账户修改自动迁移余额。</div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑账户' : '新建账户'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="账户名称" prop="accName">
          <el-input v-model="form.accName" maxlength="50" placeholder="例如：招商银行储蓄卡" />
        </el-form-item>
        <el-form-item label="账户类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="银行卡" value="BANK" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!editing" label="初始余额">
          <el-input-number v-model="form.initBalance" :min="0" :precision="2" :step="100" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="允许透支">
          <el-switch v-model="form.allowOverdraft" />
          <span class="muted" style="margin-left: 10px">关闭时支出不能超过余额</span>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { accountApi } from '@/api'
import { accountTypeLabel, money } from '@/utils/format'

const accounts = ref([])
const diffs = ref([])
const checking = ref(false)
const fixing = ref(false)
const hasDiff = computed(() => diffs.value.some((d) => Number(d.diff) !== 0))
const dialogVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const formRef = ref()

const form = reactive({
  accName: '',
  type: 'BANK',
  initBalance: 0,
  allowOverdraft: false
})

const rules = {
  accName: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择账户类型', trigger: 'change' }]
}

async function load() {
  accounts.value = await accountApi.list()
}

function openCreate() {
  editing.value = null
  Object.assign(form, { accName: '', type: 'BANK', initBalance: 0, allowOverdraft: false })
  dialogVisible.value = true
}

function openEdit(item) {
  editing.value = item
  Object.assign(form, {
    accName: item.accName,
    type: item.type,
    initBalance: Number(item.initBalance),
    allowOverdraft: item.allowOverdraft === 1
  })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form, initBalance: Number(form.initBalance) }
    if (editing.value) {
      await accountApi.update(editing.value.id, payload)
      ElMessage.success('账户已更新')
    } else {
      await accountApi.create(payload)
      ElMessage.success('账户已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onCommand(command, item) {
  if (command === 'edit') {
    openEdit(item)
    return
  }
  await ElMessageBox.confirm(`确认删除账户「${item.accName}」？存在流水的账户无法删除。`, '删除账户', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await accountApi.remove(item.id)
  ElMessage.success('账户已删除')
  await load()
}

async function reconcile() {
  checking.value = true
  try {
    diffs.value = await accountApi.reconcile()
    const bad = diffs.value.filter((d) => Number(d.diff) !== 0).length
    if (bad === 0) {
      ElMessage.success('对账通过，所有账户余额一致')
    } else {
      ElMessage.warning(`${bad} 个账户存在差异，可一键修复`)
    }
  } finally {
    checking.value = false
  }
}

async function fix() {
  fixing.value = true
  try {
    const res = await accountApi.fix()
    ElMessage.success(`已修复 ${res.fixed} 个账户`)
    await Promise.all([load(), reconcile()])
  } finally {
    fixing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.account-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.acc-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.acc-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  background: var(--raised-bg);
  border: 1px solid var(--border);
  color: var(--text-sub);
}

.more {
  cursor: pointer;
  color: var(--text-muted);
}

.acc-name {
  font-size: 15px;
  font-weight: 600;
}

.acc-type {
  font-size: 12px;
  color: var(--text-sub);
  display: flex;
  align-items: center;
  gap: 8px;
}

.acc-balance {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.5px;
  margin: 10px 0 4px;
  font-variant-numeric: tabular-nums;
}

.acc-foot {
  display: flex;
  justify-content: space-between;
  font-size: 11.5px;
  color: var(--text-sub);
}

.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--text-sub);
  font-size: 13px;
  min-height: 180px;
}

.empty-card .el-icon {
  font-size: 28px;
  color: var(--text-muted);
}

.rules {
  display: grid;
  gap: 12px;
  font-size: 13px;
  color: var(--text-sub);
}

.rules > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rules .el-icon {
  color: var(--text-sub);
}
</style>
