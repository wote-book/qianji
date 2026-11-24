<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">收支流水</h2>
        <p class="page-desc">所有流水按时间倒序展示，新增、修改、删除都会实时联动账户余额。</p>
      </div>
      <div class="inline-form">
        <el-button :icon="Upload" @click="router.push('/records/import')">CSV 导入</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">记一笔</el-button>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-date-picker
          v-model="filters.range"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
          @change="search"
        />
        <el-select v-model="filters.type" placeholder="收支类型" clearable style="width: 120px" @change="search">
          <el-option label="收入" value="INCOME" />
          <el-option label="支出" value="EXPENSE" />
        </el-select>
        <el-select v-model="filters.categoryId" placeholder="全部分类" clearable filterable style="width: 150px" @change="search">
          <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="filters.accountId" placeholder="全部账户" clearable style="width: 160px" @change="search">
          <el-option v-for="item in accounts" :key="item.id" :label="item.accName" :value="item.id" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="搜索备注" clearable style="width: 180px" @keyup.enter="search" />
        <el-button type="primary" plain @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <div class="table-wrap">
        <el-table :data="rows" v-loading="loading" empty-text="暂无流水记录" :span-method="spanMethod">
          <el-table-column label="日期" prop="recordDate" width="158">
            <template #default="{ row }">
              <div class="day-cell">
                <b>{{ row.recordDate }}</b>
                <span class="day-sum">
                  <i v-if="dayStat(row.recordDate).income > 0" class="income">收 ¥{{ money(dayStat(row.recordDate).income) }}</i>
                  <i v-if="dayStat(row.recordDate).expense > 0" class="expense">支 ¥{{ money(dayStat(row.recordDate).expense) }}</i>
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="82">
            <template #default="{ row }">
              <el-tag :type="row.type === 'INCOME' ? 'success' : 'danger'" effect="light" class="tag-soft" size="small">
                {{ typeLabel(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="分类" prop="categoryName" width="110" show-overflow-tooltip />
          <el-table-column label="账户" prop="accountName" width="150" show-overflow-tooltip />
          <el-table-column label="金额" align="right" width="140">
            <template #default="{ row }">
              <b :class="row.type === 'INCOME' ? 'money-income' : 'money-expense'">
                {{ row.type === 'INCOME' ? '+' : '-' }}¥{{ money(row.amount) }}
              </b>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="100">
            <template #default="{ row }">
              <span class="muted">{{ sourceLabel(row.source) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button text type="danger" size="small" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next"
          background
          @change="load"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑流水' : '记一笔'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="收支类型" prop="type">
          <el-radio-group v-model="form.type" @change="form.categoryId = null">
            <el-radio-button value="EXPENSE">支出</el-radio-button>
            <el-radio-button value="INCOME">收入</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="10" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日期" prop="recordDate">
          <el-date-picker v-model="form.recordDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="账户" prop="accountId">
          <el-select v-model="form.accountId" placeholder="请选择账户" style="width: 100%">
            <el-option v-for="item in accounts" :key="item.id" :label="`${item.accName}（¥${money(item.balance)}）`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" filterable style="width: 100%">
            <el-option v-for="item in formCategories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="200" show-word-limit placeholder="选填，最多 200 字" />
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
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import { accountApi, categoryApi, recordApi } from '@/api'
import { money, sourceLabel, today, typeLabel } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const accounts = ref([])
const categories = ref([])

const filters = reactive({
  range: route.query.start && route.query.end ? [route.query.start, route.query.end] : [],
  type: route.query.type || '',
  categoryId: route.query.categoryId ? Number(route.query.categoryId) : '',
  accountId: '',
  keyword: ''
})

const dialogVisible = ref(false)
const editing = ref(null)
const formRef = ref()
const form = reactive({
  type: 'EXPENSE',
  amount: 0,
  recordDate: today(),
  accountId: null,
  categoryId: null,
  remark: ''
})

const rules = {
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  recordDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const categoryOptions = computed(() => categories.value.filter((c) => !filters.type || c.type === filters.type))
const formCategories = computed(() => categories.value.filter((c) => c.type === form.type))

const dayStats = computed(() => {
  const map = {}
  for (const row of rows.value) {
    const key = row.recordDate
    if (!map[key]) map[key] = { income: 0, expense: 0 }
    if (row.type === 'INCOME') {
      map[key].income += Number(row.amount)
    } else {
      map[key].expense += Number(row.amount)
    }
  }
  return map
})

function dayStat(date) {
  return dayStats.value[date] || { income: 0, expense: 0 }
}

const dateSpans = computed(() => {
  const spans = {}
  let start = 0
  while (start < rows.value.length) {
    const date = rows.value[start].recordDate
    let end = start + 1
    while (end < rows.value.length && rows.value[end].recordDate === date) end++
    spans[start] = end - start
    for (let i = start + 1; i < end; i++) spans[i] = 0
    start = end
  }
  return spans
})

function spanMethod({ rowIndex, column }) {
  if (column.property === 'recordDate') {
    return { rowspan: dateSpans.value[rowIndex] ?? 1, colspan: 1 }
  }
  return { rowspan: 1, colspan: 1 }
}

async function load() {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      type: filters.type || undefined,
      categoryId: filters.categoryId || undefined,
      accountId: filters.accountId || undefined,
      keyword: filters.keyword || undefined,
      start: filters.range?.[0],
      end: filters.range?.[1]
    }
    const data = await recordApi.page(params)
    rows.value = data.list || data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  load()
}

function reset() {
  filters.range = []
  filters.type = ''
  filters.categoryId = ''
  filters.accountId = ''
  filters.keyword = ''
  pageNum.value = 1
  router.replace('/records')
  load()
}

function openCreate() {
  editing.value = null
  Object.assign(form, {
    type: 'EXPENSE',
    amount: 0,
    recordDate: today(),
    accountId: accounts.value[0]?.id || null,
    categoryId: null,
    remark: ''
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, {
    type: row.type,
    amount: Number(row.amount),
    recordDate: row.recordDate,
    accountId: row.accountId,
    categoryId: row.categoryId,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form, amount: Number(form.amount) }
    if (editing.value) {
      await recordApi.update(editing.value.id, payload)
      ElMessage.success('流水已更新')
    } else {
      await recordApi.create(payload)
      ElMessage.success('已记录一笔')
    }
    dialogVisible.value = false
    await Promise.all([load(), loadAccounts()])
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除 ${row.recordDate} 的「${row.categoryName}」流水？`, '删除流水', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await recordApi.remove(row.id)
  ElMessage.success('已删除')
  await Promise.all([load(), loadAccounts()])
}

async function loadAccounts() {
  accounts.value = await accountApi.list()
}

async function loadCategories() {
  categories.value = await categoryApi.list()
}

onMounted(async () => {
  await Promise.all([loadAccounts(), loadCategories()])
  await load()
  window.addEventListener('finance:records-changed', load)
  if (route.query.create === '1' || route.query.create === 'true') {
    openCreate()
    router.replace({ path: '/records', query: {} })
  }
})

onBeforeUnmount(() => window.removeEventListener('finance:records-changed', load))
</script>

<style scoped>
.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
}

.day-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 2px 0;
}

.day-cell b {
  font-weight: 600;
  color: #344054;
  font-variant-numeric: tabular-nums;
}

.day-sum {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11px;
  line-height: 1.5;
}

.day-sum i {
  font-style: normal;
  font-variant-numeric: tabular-nums;
}

.day-sum .income {
  color: #12b76a;
}

.day-sum .expense {
  color: #f04438;
}
</style>
