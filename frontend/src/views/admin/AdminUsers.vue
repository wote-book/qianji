<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">查看账号元数据与使用规模，可禁用 / 启用账号或生成一次性临时密码。</p>
      </div>
      <el-button :loading="exporting" @click="exportCsv">
        <el-icon><Download /></el-icon>
        <span>导出 CSV</span>
      </el-button>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-input v-model="filters.keyword" placeholder="用户名 / 昵称" clearable style="width: 200px" @keyup.enter="search" />
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-select v-model="filters.role" placeholder="全部角色" clearable style="width: 130px">
          <el-option label="普通用户" value="USER" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
        <el-select v-model="filters.segment" placeholder="全部活跃分层" clearable style="width: 150px">
          <el-option v-for="(label, key) in segmentLabels" :key="key" :label="label" :value="key" />
        </el-select>
        <el-date-picker
          v-model="filters.range"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="注册开始"
          end-placeholder="注册结束"
          style="width: 250px"
        />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="users" v-loading="loading" empty-text="没有符合条件的用户">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="用户名" prop="username" min-width="120" />
        <el-table-column label="昵称" prop="nickname" min-width="110" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'primary'" effect="light" size="small" class="tag-soft">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="row.role === 'ADMIN'"
              inline-prompt
              active-text="启用"
              inactive-text="禁用"
              @change="(value) => toggleStatus(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="160">
          <template #default="{ row }">{{ datetime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="160">
          <template #default="{ row }">{{ datetime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="近 30 天活跃" width="120" align="center">
          <template #default="{ row }">{{ row.activeDays30 }} 天</template>
        </el-table-column>
        <el-table-column label="流水数" prop="recordCount" width="90" align="right" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button text type="warning" size="small" :disabled="row.role === 'ADMIN'" @click="resetPassword(row)">
              重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <span class="muted">共 {{ total }} 个账号</span>
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="用户详情" size="420px">
      <div v-if="detail" class="detail">
        <div class="detail-head">
          <div class="detail-avatar">{{ (detail.nickname || detail.username || 'U').slice(0, 1) }}</div>
          <div>
            <div class="detail-name">{{ detail.nickname || detail.username }}</div>
            <div class="muted">@{{ detail.username }} · {{ detail.role === 'ADMIN' ? '管理员' : '普通用户' }}</div>
          </div>
        </div>

        <div class="detail-group">
          <div class="group-title">基础资料</div>
          <div class="kv"><span>用户 ID</span><b>{{ detail.id }}</b></div>
          <div class="kv"><span>注册时间</span><b>{{ datetime(detail.createTime) }}</b></div>
          <div class="kv"><span>注册天数</span><b>{{ detail.registerDays }} 天</b></div>
          <div class="kv"><span>账号状态</span><b>{{ detail.status === 1 ? '启用' : '禁用' }}</b></div>
          <div class="kv"><span>最近登录</span><b>{{ datetime(detail.lastLoginTime) }}</b></div>
          <div class="kv"><span>最近登录 IP</span><b>{{ detail.lastLoginIp || '—' }}</b></div>
        </div>

        <div class="detail-group">
          <div class="group-title">数据规模（仅数量）</div>
          <div class="count-grid">
            <div class="count-item"><b>{{ detail.accountCount }}</b><span>账户</span></div>
            <div class="count-item"><b>{{ detail.recordCount }}</b><span>流水</span></div>
            <div class="count-item"><b>{{ detail.categoryCount }}</b><span>分类</span></div>
            <div class="count-item"><b>{{ detail.budgetCount }}</b><span>预算</span></div>
            <div class="count-item"><b>{{ detail.goalCount }}</b><span>目标</span></div>
            <div class="count-item"><b>{{ detail.briefCount }}</b><span>简报调用</span></div>
          </div>
        </div>

        <div class="detail-group">
          <div class="group-title">活跃概况</div>
          <div class="kv"><span>近 30 天活跃天数</span><b>{{ detail.activeDays30 }} 天</b></div>
          <div class="kv"><span>最后操作模块</span><b>{{ moduleLabels[detail.lastModule] || detail.lastModule || '—' }}</b></div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="resetVisible" title="重置密码" width="420px">
      <p class="reset-tip">已为「{{ resetResult.username }}」生成一次性临时密码，关闭后不会再次显示。</p>
      <div class="temp-password">{{ resetResult.tempPassword }}</div>
      <template #footer>
        <el-button @click="copyTemp">复制密码</el-button>
        <el-button type="primary" @click="resetVisible = false">我已记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, downloadBlob } from '@/api'

const route = useRoute()

const segmentLabels = {
  HIGH: '高活跃',
  NORMAL: '普通',
  DORMANT: '沉睡',
  INACTIVE: '未激活'
}

const moduleLabels = {
  RECORD: '收支流水',
  ACCOUNT: '账户与对账',
  CATEGORY: '分类管理',
  BUDGET: '预算管理',
  STATS: '统计分析',
  GOAL: '理财目标',
  BRIEF: '智能简报',
  REPORT: '报表导出',
  IMPORT: 'CSV 导入',
  BACKUP: '备份恢复'
}

const filters = reactive({
  keyword: '',
  status: '',
  role: '',
  segment: route.query.segment || '',
  range: []
})
const users = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const exporting = ref(false)

const detailVisible = ref(false)
const detail = ref(null)
const resetVisible = ref(false)
const resetResult = reactive({ username: '', tempPassword: '' })

function queryParams() {
  return {
    keyword: filters.keyword || undefined,
    status: filters.status === '' ? undefined : filters.status,
    role: filters.role || undefined,
    segment: filters.segment || undefined,
    start: filters.range?.[0] || undefined,
    end: filters.range?.[1] || undefined
  }
}

async function load() {
  loading.value = true
  try {
    const data = await adminApi.users({
      ...queryParams(),
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    users.value = data.list || []
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
  filters.keyword = ''
  filters.status = ''
  filters.role = ''
  filters.segment = ''
  filters.range = []
  pageNum.value = 1
  load()
}

function onSizeChange(size) {
  pageSize.value = size
  pageNum.value = 1
  load()
}

async function toggleStatus(row, value) {
  const action = value ? '启用' : '禁用'
  await ElMessageBox.confirm(`确认${action}用户「${row.username}」？禁用后该账号将立即退出登录。`, `${action}账号`, {
    type: 'warning',
    confirmButtonText: action,
    cancelButtonText: '取消'
  })
  await adminApi.setStatus(row.id, value ? 1 : 0)
  ElMessage.success(`已${action}`)
  await load()
}

async function openDetail(row) {
  detail.value = await adminApi.userDetail(row.id)
  detailVisible.value = true
}

async function resetPassword(row) {
  await ElMessageBox.confirm(`确认重置「${row.username}」的密码？原密码将立即失效。`, '重置密码', {
    type: 'warning',
    confirmButtonText: '重置',
    cancelButtonText: '取消'
  })
  const data = await adminApi.resetPassword(row.id)
  resetResult.username = data.username
  resetResult.tempPassword = data.tempPassword
  resetVisible.value = true
}

async function copyTemp() {
  try {
    await navigator.clipboard.writeText(resetResult.tempPassword)
    ElMessage.success('临时密码已复制')
  } catch (e) {
    ElMessage.warning('复制失败，请手动记录')
  }
}

async function exportCsv() {
  exporting.value = true
  try {
    const blob = await adminApi.exportUsers(queryParams())
    downloadBlob(blob, '用户列表.csv')
    ElMessage.success('已导出 CSV')
  } finally {
    exporting.value = false
  }
}

function datetime(value) {
  return value ? String(value).replace('T', ' ') : '—'
}

watch(
  () => route.query.segment,
  (value) => {
    if (value !== undefined) {
      filters.segment = value || ''
      pageNum.value = 1
      load()
    }
  }
)

onMounted(load)
</script>

<style scoped>
.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
}

.detail {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.detail-avatar {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: #a8843f;
  color: #fffdf8;
  font-weight: 600;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
}

.detail-group {
  border-top: 1px solid var(--border);
  padding-top: 16px;
}

.group-title {
  font-size: 13px;
  font-weight: 600;
  color: #475467;
  margin-bottom: 12px;
}

.kv {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  font-size: 13px;
  padding: 6px 0;
}

.kv span {
  color: var(--text-sub);
}

.kv b {
  color: var(--text-main);
  font-weight: 600;
  text-align: right;
}

.count-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.count-item {
  background: var(--raised-bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px 10px;
  text-align: center;
}

.count-item b {
  display: block;
  font-size: 18px;
  color: var(--text-main);
}

.count-item span {
  font-size: 11.5px;
  color: var(--text-sub);
}

.reset-tip {
  font-size: 13px;
  color: var(--text-sub);
  margin: 0 0 14px;
}

.temp-password {
  font-family: 'Consolas', 'Menlo', monospace;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 3px;
  text-align: center;
  color: var(--text-main);
  background: var(--raised-bg);
  border: 1px dashed var(--border-strong);
  border-radius: 8px;
  padding: 18px 12px;
}
</style>
