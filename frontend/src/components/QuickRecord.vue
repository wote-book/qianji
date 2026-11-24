<template>
  <button class="quick-fab" type="button" title="记一笔（Alt+N）" @click="open('manual')">
    <el-icon><Plus /></el-icon>
    <span>记一笔</span>
  </button>

  <el-dialog v-model="visible" width="580px" :close-on-click-modal="false" destroy-on-close class="quick-record-dialog">
    <template #header>
      <div class="qr-header">
        <span class="qr-title">记一笔</span>
        <span class="qr-hint">Alt + N 随时打开</span>
      </div>
    </template>

    <el-tabs v-model="tab">
      <el-tab-pane label="手动记账" name="manual">
        <el-form ref="manualFormRef" :model="manualForm" :rules="manualRules" label-width="64px">
          <el-form-item label="类型">
            <el-radio-group v-model="manualForm.type" @change="onManualTypeChange">
              <el-radio-button value="EXPENSE">支出</el-radio-button>
              <el-radio-button value="INCOME">收入</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="金额" prop="amount">
            <el-input-number v-model="manualForm.amount" :min="0.01" :max="1000000" :precision="2" :step="1" style="width: 100%" placeholder="0.00" />
          </el-form-item>
          <el-form-item label="日期" prop="recordDate">
            <el-date-picker v-model="manualForm.recordDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" placeholder="选择日期" />
          </el-form-item>
          <el-form-item label="账户" prop="accountId">
            <el-select v-model="manualForm.accountId" placeholder="选择账户" style="width: 100%">
              <el-option v-for="acc in accounts" :key="acc.id" :label="acc.accName" :value="acc.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="manualForm.categoryId" placeholder="选择分类" style="width: 100%">
              <el-option v-for="cat in manualCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="manualForm.remark" maxlength="200" placeholder="选填，例如：午饭" />
          </el-form-item>
        </el-form>
        <div class="qr-actions">
          <el-button :loading="saving" @click="saveManual(true)">保存并继续</el-button>
          <el-button type="primary" :loading="saving" @click="saveManual(false)">保存</el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="AI 记账" name="ai">
        <div class="ai-box">
          <el-input
            v-model="aiText"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            resize="none"
            placeholder="用一句话描述，例如：今天午饭25，打车18，用支付宝"
            @keydown.ctrl.enter="parse"
          />
          <div class="ai-examples">
            <span class="muted">试试：</span>
            <button v-for="example in examples" :key="example" type="button" class="example-chip" @click="aiText = example">{{ example }}</button>
          </div>
          <div class="ai-parse-row">
            <el-button type="primary" :icon="MagicStick" :loading="parsing" @click="parse">解析</el-button>
            <span class="muted ai-shortcut">Ctrl + Enter</span>
          </div>
        </div>

        <el-alert v-if="aiMessage" :title="aiMessage" :type="degraded ? 'warning' : 'info'" :closable="false" show-icon class="ai-alert" />

        <div v-if="aiItems.length" class="ai-items">
          <div v-for="(item, index) in aiItems" :key="index" class="ai-item">
            <div class="ai-item-head">
              <el-radio-group v-model="item.type" size="small" @change="onItemTypeChange(item)">
                <el-radio-button value="EXPENSE">支出</el-radio-button>
                <el-radio-button value="INCOME">收入</el-radio-button>
              </el-radio-group>
              <el-button text type="danger" size="small" :icon="Delete" @click="removeItem(index)">删除</el-button>
            </div>
            <div class="ai-item-grid">
              <el-input-number v-model="item.amount" :min="0.01" :max="1000000" :precision="2" size="small" style="width: 100%" />
              <el-date-picker v-model="item.recordDate" type="date" value-format="YYYY-MM-DD" size="small" style="width: 100%" />
              <el-select v-model="item.accountId" size="small" placeholder="账户" style="width: 100%">
                <el-option v-for="acc in accounts" :key="acc.id" :label="acc.accName" :value="acc.id" />
              </el-select>
              <el-select v-model="item.categoryId" size="small" placeholder="分类" style="width: 100%">
                <el-option v-for="cat in categoryOptions(item.type)" :key="cat.id" :label="cat.name" :value="cat.id" />
              </el-select>
            </div>
            <el-input v-model="item.remark" size="small" maxlength="200" placeholder="备注" />
            <div v-if="item.warn" class="ai-warn">{{ item.warn }}</div>
          </div>
          <div class="ai-total muted">共 {{ aiItems.length }} 笔，合计 {{ totalAmount }} 元</div>
        </div>

        <div v-if="!aiItems.length && !parsing" class="ai-empty muted">
          AI 会把一句话拆成多笔，解析后可以逐笔修改，确认后才写入账本。
        </div>

        <div class="qr-actions">
          <el-button v-if="aiItems.length" :disabled="parsing" @click="parse">重新解析</el-button>
          <el-button type="primary" :loading="committing" :disabled="!aiItems.length" @click="commit">
            确认入账（{{ aiItems.length }} 笔）
          </el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, MagicStick, Plus } from '@element-plus/icons-vue'
import { accountApi, aiRecordApi, categoryApi, recordApi } from '@/api'

const visible = ref(false)
const tab = ref('manual')
const accounts = ref([])
const allCategories = ref([])
const saving = ref(false)
const parsing = ref(false)
const committing = ref(false)

const manualFormRef = ref()
const manualForm = reactive({
  type: 'EXPENSE',
  amount: null,
  recordDate: today(),
  accountId: null,
  categoryId: null,
  remark: ''
})
const manualRules = {
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  recordDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const aiText = ref('')
const aiItems = ref([])
const aiMessage = ref('')
const degraded = ref(false)
const examples = ['今天午饭25，打车18', '昨天超市120，用支付宝', '发工资8000']

const manualCategories = computed(() =>
  allCategories.value.filter((c) => c.type === manualForm.type && c.status === 1)
)
const totalAmount = computed(() =>
  aiItems.value.reduce((sum, item) => sum + (Number(item.amount) || 0), 0).toFixed(2)
)

function today() {
  const date = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

async function loadOptions() {
  const [accountList, categoryList] = await Promise.all([accountApi.list(), categoryApi.list()])
  accounts.value = accountList || []
  allCategories.value = categoryList || []
  if (!manualForm.accountId && accounts.value.length) {
    manualForm.accountId = accounts.value[0].id
  }
  if (!manualForm.categoryId && manualCategories.value.length) {
    manualForm.categoryId = manualCategories.value[0].id
  }
}

async function open(mode = 'manual') {
  tab.value = mode
  visible.value = true
  await loadOptions()
}

function onManualTypeChange() {
  manualForm.categoryId = manualCategories.value.length ? manualCategories.value[0].id : null
}

function categoryOptions(type) {
  return allCategories.value.filter((c) => c.type === type && c.status === 1)
}

function onItemTypeChange(item) {
  const options = categoryOptions(item.type)
  item.categoryId = options.length ? options[0].id : null
}

function removeItem(index) {
  aiItems.value.splice(index, 1)
}

async function saveManual(continueAfter) {
  const valid = await manualFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await recordApi.create({
      accountId: manualForm.accountId,
      categoryId: manualForm.categoryId,
      amount: manualForm.amount,
      type: manualForm.type,
      recordDate: manualForm.recordDate,
      remark: manualForm.remark
    })
    ElMessage.success('已记一笔')
    window.dispatchEvent(new CustomEvent('finance:records-changed'))
    if (continueAfter) {
      manualForm.amount = null
      manualForm.remark = ''
    } else {
      visible.value = false
    }
  } finally {
    saving.value = false
  }
}

async function parse() {
  if (!aiText.value.trim()) {
    ElMessage.warning('先描述一下这笔收支')
    return
  }
  parsing.value = true
  aiMessage.value = ''
  degraded.value = false
  try {
    const result = await aiRecordApi.parse({
      text: aiText.value.trim(),
      defaultAccountId: accounts.value[0]?.id || null,
      defaultDate: today()
    })
    aiItems.value = (result.items || []).map((item) => ({ ...item }))
    degraded.value = !!result.degraded
    aiMessage.value = result.message || (result.source === 'RULE' ? '已使用规则解析，请核对结果' : '')
  } finally {
    parsing.value = false
  }
}

async function commit() {
  let missing = 0
  for (const item of aiItems.value) {
    const lacks = []
    if (!item.amount) lacks.push('金额')
    if (!item.accountId) lacks.push('账户')
    if (!item.categoryId) lacks.push('分类')
    if (!item.recordDate) lacks.push('日期')
    item.warn = lacks.length ? `请补全${lacks.join('、')}` : null
    if (lacks.length) missing++
  }
  if (missing) {
    ElMessage.warning(`有 ${missing} 笔记录信息不完整，请补全后再入账`)
    return
  }
  committing.value = true
  try {
    const result = await aiRecordApi.commit({
      items: aiItems.value.map((item) => ({
        accountId: item.accountId,
        categoryId: item.categoryId,
        amount: item.amount,
        type: item.type,
        recordDate: item.recordDate,
        remark: item.remark || ''
      }))
    })
    ElMessage.success(`已通过 AI 入账 ${result.count} 笔`)
    window.dispatchEvent(new CustomEvent('finance:records-changed'))
    aiItems.value = []
    aiText.value = ''
    aiMessage.value = ''
    visible.value = false
  } finally {
    committing.value = false
  }
}

function onKeydown(event) {
  if (event.altKey && !event.ctrlKey && !event.metaKey && (event.key === 'n' || event.key === 'N')) {
    event.preventDefault()
    open(tab.value)
  }
}

function onOpenEvent(event) {
  open(event.detail?.mode === 'ai' ? 'ai' : 'manual')
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('finance:open-record', onOpenEvent)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('finance:open-record', onOpenEvent)
})
</script>

<style scoped>
.quick-fab {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 42px;
  padding: 0 18px;
  border: 1px solid rgba(35, 33, 29, 0.08);
  border-radius: 8px;
  background: var(--el-color-primary);
  color: #fffdf8;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(35, 33, 29, 0.16);
  transition: background 0.15s, box-shadow 0.15s;
}

.quick-fab:hover {
  background: #223546;
  box-shadow: 0 8px 20px rgba(35, 33, 29, 0.2);
}

.quick-fab .el-icon {
  font-size: 16px;
}

.qr-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.qr-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
}

.qr-hint {
  font-size: 11px;
  color: var(--text-sub);
}

.qr-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.ai-box {
  background: var(--raised-bg);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
}

.ai-examples {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
  font-size: 12px;
}

.example-chip {
  border: 1px solid var(--border);
  background: var(--card-bg);
  color: #57534a;
  border-radius: 4px;
  padding: 3px 10px;
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}

.example-chip:hover {
  border-color: var(--border-strong);
  color: var(--el-color-primary);
}

.ai-parse-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
}

.ai-shortcut {
  font-size: 11px;
}

.ai-alert {
  margin-top: 12px;
}

.ai-items {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 300px;
  overflow-y: auto;
}

.ai-item {
  border: 1px solid var(--border);
  border-left: 2px solid var(--el-color-primary-light-5);
  border-radius: 6px;
  padding: 10px;
  background: var(--card-bg);
}

.ai-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.ai-item-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 8px;
}

.ai-warn {
  margin-top: 6px;
  font-size: 11.5px;
  color: #d97706;
}

.ai-total {
  text-align: right;
  font-size: 12px;
}

.ai-empty {
  margin: 18px 0 4px;
  text-align: center;
  font-size: 12.5px;
}

@media (max-width: 640px) {
  .quick-fab {
    right: 18px;
    bottom: 18px;
    width: 52px;
    height: 52px;
    padding: 0;
    justify-content: center;
    border-radius: 50%;
  }

  .quick-fab span {
    display: none;
  }
}
</style>
