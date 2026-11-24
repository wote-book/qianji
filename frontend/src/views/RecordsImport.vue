<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">CSV 批量导入</h2>
        <p class="page-desc">下载模板填写后上传预览，系统会自动匹配账户、分类并按备注关键词智能归类，重复数据自动跳过。</p>
      </div>
      <el-button @click="router.push('/records')">返回流水</el-button>
    </div>

    <div class="grid grid-3">
      <div class="card step-card">
        <div class="step-no">01</div>
        <div class="step-title">下载模板</div>
        <div class="step-desc">模板含表头与示例，支持 UTF-8 与 GBK 编码。</div>
        <el-button type="primary" plain :icon="Download" @click="downloadTemplate">下载 CSV 模板</el-button>
      </div>
      <div class="card step-card">
        <div class="step-no">02</div>
        <div class="step-title">上传文件</div>
        <div class="step-desc">单次最多 5000 行，表头需包含日期、金额。</div>
        <el-upload
          drag
          :auto-upload="false"
          :show-file-list="false"
          accept=".csv,text/csv"
          :on-change="handleFile"
        >
          <div class="upload-box">
            <el-icon><UploadFilled /></el-icon>
            <div>拖拽 CSV 到此处，或点击选择</div>
          </div>
        </el-upload>
      </div>
      <div class="card step-card">
        <div class="step-no">03</div>
        <div class="step-title">预览与确认</div>
        <div class="step-desc">逐行检查账户与分类，可手动修正后再导入。</div>
        <el-button type="primary" :disabled="!rows.length" :loading="committing" :icon="Check" @click="commit">
          确认导入 {{ validRows.length }} 条
        </el-button>
      </div>
    </div>

    <div v-if="summary" class="card section-gap">
      <div class="result-bar">
        <el-icon class="ok"><CircleCheckFilled /></el-icon>
        <div>
          <b>解析完成</b>
          <span class="muted">共 {{ summary.total }} 行，可导入 {{ summary.validCount }} 行，需修正 {{ summary.invalidCount }} 行</span>
        </div>
      </div>
    </div>

    <div v-if="rows.length" class="card section-gap">
      <div class="card-title">
        <span>导入预览</span>
        <span class="sub">账户与分类可逐行修改，红色行不会导入</span>
      </div>
      <div class="table-wrap">
        <el-table :data="rows" size="small" max-height="520" :row-class-name="rowClass">
          <el-table-column label="行号" prop="rowNo" width="64" />
          <el-table-column label="日期" width="130">
            <template #default="{ row }">
              <el-date-picker v-model="row.date" type="date" value-format="YYYY-MM-DD" size="small" style="width: 118px" />
            </template>
          </el-table-column>
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-select v-model="row.type" size="small" style="width: 90px" @change="row.categoryId = null">
                <el-option label="支出" value="EXPENSE" />
                <el-option label="收入" value="INCOME" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.amount" :min="0.01" :precision="2" :controls="false" size="small" style="width: 110px" />
            </template>
          </el-table-column>
          <el-table-column label="账户" width="180">
            <template #default="{ row }">
              <el-select v-model="row.accountId" size="small" filterable placeholder="选择账户" style="width: 160px">
                <el-option v-for="item in accounts" :key="item.id" :label="item.accName" :value="item.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="分类" width="170">
            <template #default="{ row }">
              <el-select v-model="row.categoryId" size="small" filterable placeholder="选择分类" style="width: 150px">
                <el-option v-for="item in categoriesByType(row.type)" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.remark" size="small" placeholder="选填" />
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="200">
            <template #default="{ row }">
              <span :class="isValid(row) ? 'money-income' : 'money-expense'" class="row-msg">
                {{ row.message }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="preview-foot">
        <span class="muted">可导入 {{ validRows.length }} 条，待修正 {{ rows.length - validRows.length }} 条</span>
        <div>
          <el-button @click="clear">清空</el-button>
          <el-button type="primary" :loading="committing" @click="commit">确认导入</el-button>
        </div>
      </div>
    </div>

    <div v-if="result" class="card section-gap">
      <div class="result-bar">
        <el-icon class="ok"><CircleCheckFilled /></el-icon>
        <div>
          <b>导入完成</b>
          <span class="muted">共 {{ result.total }} 条，成功 {{ result.inserted }} 条，重复跳过 {{ result.skipped }} 条</span>
        </div>
        <el-button text type="primary" @click="router.push('/records')">查看流水</el-button>
      </div>
    </div>

    <div class="card section-gap">
      <div class="card-title">字段说明</div>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="日期">必填，支持 2026-06-01 / 2026/6/1 格式</el-descriptions-item>
        <el-descriptions-item label="类型">收入或支出，留空默认支出</el-descriptions-item>
        <el-descriptions-item label="分类">可留空，系统按关键词自动归类</el-descriptions-item>
        <el-descriptions-item label="账户">账户名称，未匹配时默认第一个账户</el-descriptions-item>
        <el-descriptions-item label="金额">必填且大于 0，支持千分位与 ¥ 符号</el-descriptions-item>
        <el-descriptions-item label="备注">选填，参与重复数据指纹计算</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Download, UploadFilled } from '@element-plus/icons-vue'
import { accountApi, categoryApi, exportApi, recordApi, downloadBlob } from '@/api'

const router = useRouter()
const rows = ref([])
const summary = ref(null)
const result = ref(null)
const accounts = ref([])
const categories = ref([])
const committing = ref(false)

const validRows = computed(() => rows.value.filter(isValid))

function categoriesByType(type) {
  return categories.value.filter((c) => c.type === type)
}

function isValid(row) {
  return !!(
    row.date &&
    row.type &&
    Number(row.amount) > 0 &&
    row.accountId &&
    row.categoryId &&
    categories.value.find((c) => c.id === row.categoryId && c.type === row.type)
  )
}

function rowClass({ row }) {
  return isValid(row) ? '' : 'invalid-row'
}

async function downloadTemplate() {
  const blob = await exportApi.template()
  downloadBlob(blob, '流水导入模板.csv')
}

async function handleFile(file) {
  if (!file.raw) return
  if (!file.raw.name.toLowerCase().endsWith('.csv')) {
    ElMessage.warning('请选择 .csv 文件')
    return
  }
  const formData = new FormData()
  formData.append('file', file.raw)
  const data = await recordApi.importPreview(formData)
  summary.value = data
  result.value = null
  rows.value = (data.rows || []).map((row) => ({ ...row }))
}

async function commit() {
  if (!validRows.value.length) {
    ElMessage.warning('没有可导入的数据')
    return
  }
  committing.value = true
  try {
    const payload = validRows.value.map((row) => ({
      date: row.date,
      type: row.type,
      amount: Number(row.amount),
      accountId: row.accountId,
      categoryId: row.categoryId,
      remark: row.remark || ''
    }))
    result.value = await recordApi.importCommit(payload)
    ElMessage.success(`成功导入 ${result.value.inserted} 条`)
    rows.value = []
    summary.value = null
  } finally {
    committing.value = false
  }
}

function clear() {
  rows.value = []
  summary.value = null
  result.value = null
}

onMounted(async () => {
  ;[accounts.value, categories.value] = await Promise.all([accountApi.list(), categoryApi.list()])
})
</script>

<style scoped>
.step-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-start;
}

.step-no {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-sub);
  letter-spacing: 0;
}

.step-title {
  font-size: 16px;
  font-weight: 600;
}

.step-desc {
  font-size: 12.5px;
  color: var(--text-sub);
  line-height: 1.7;
  flex: 1;
}

.upload-box {
  padding: 16px 8px;
  color: var(--text-sub);
  font-size: 12.5px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-box .el-icon {
  font-size: 26px;
  color: var(--text-muted);
}

.result-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-bar > div {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ok {
  color: var(--money-income);
  font-size: 24px;
}

.preview-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
}

.row-msg {
  font-size: 12px;
}

:deep(.invalid-row) {
  background: #fff5f5;
}
</style>
