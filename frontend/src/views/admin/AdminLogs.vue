<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">系统日志</h2>
        <p class="page-desc">登录审计与功能使用事件，用于排查异常登录与了解操作路径。</p>
      </div>
    </div>

    <div class="card">
      <el-tabs v-model="tab" @tab-change="onTabChange">
        <el-tab-pane label="登录日志" name="login">
          <div class="filter-bar">
            <el-input v-model="loginFilters.username" placeholder="用户名" clearable style="width: 180px" @keyup.enter="searchLogin" />
            <el-select v-model="loginFilters.success" placeholder="全部结果" clearable style="width: 130px">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
            <el-date-picker
              v-model="loginFilters.range"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 250px"
            />
            <el-button type="primary" @click="searchLogin">查询</el-button>
            <el-button @click="resetLogin">重置</el-button>
          </div>

          <el-table :data="loginLogs" v-loading="loginLoading" size="small" empty-text="暂无登录日志">
            <el-table-column label="时间" min-width="160">
              <template #default="{ row }">{{ datetime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="用户名" prop="username" min-width="120" />
            <el-table-column label="IP" prop="ip" min-width="130" />
            <el-table-column label="设备" prop="userAgent" min-width="260" show-overflow-tooltip />
            <el-table-column label="结果" width="90">
              <template #default="{ row }">
                <el-tag :type="row.success === 1 ? 'success' : 'danger'" effect="light" size="small" class="tag-soft">
                  {{ row.success === 1 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <span class="muted">共 {{ loginTotal }} 条</span>
            <el-pagination
              v-model:current-page="loginPage"
              :page-size="pageSize"
              :total="loginTotal"
              layout="prev, pager, next"
              background
              @current-change="loadLogin"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="使用事件" name="events">
          <div class="filter-bar">
            <el-input v-model="eventFilters.username" placeholder="用户名" clearable style="width: 180px" @keyup.enter="searchEvent" />
            <el-select v-model="eventFilters.module" placeholder="全部模块" clearable style="width: 160px">
              <el-option v-for="(label, key) in moduleLabels" :key="key" :label="label" :value="key" />
            </el-select>
            <el-date-picker
              v-model="eventFilters.range"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 250px"
            />
            <el-button type="primary" @click="searchEvent">查询</el-button>
            <el-button @click="resetEvent">重置</el-button>
          </div>

          <el-table :data="events" v-loading="eventLoading" size="small" empty-text="暂无使用事件">
            <el-table-column label="时间" min-width="160">
              <template #default="{ row }">{{ datetime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="用户名" prop="username" min-width="120" />
            <el-table-column label="模块" width="130">
              <template #default="{ row }">
                <el-tag effect="plain" size="small">{{ moduleLabels[row.module] || row.module }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="动作" width="110">
              <template #default="{ row }">{{ actionLabels[row.action] || row.action }}</template>
            </el-table-column>
            <el-table-column label="对象 ID" width="100">
              <template #default="{ row }">{{ row.refId || '—' }}</template>
            </el-table-column>
            <el-table-column label="IP" prop="ip" min-width="130" />
          </el-table>

          <div class="pager">
            <span class="muted">共 {{ eventTotal }} 条</span>
            <el-pagination
              v-model:current-page="eventPage"
              :page-size="pageSize"
              :total="eventTotal"
              layout="prev, pager, next"
              background
              @current-change="loadEvents"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminApi } from '@/api'

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

const actionLabels = {
  VIEW: '查看',
  CREATE: '新增',
  UPDATE: '修改',
  DELETE: '删除',
  GENERATE: '生成',
  EXPORT: '导出'
}

const tab = ref('login')
const pageSize = 15

const loginFilters = reactive({ username: '', success: '', range: [] })
const loginLogs = ref([])
const loginTotal = ref(0)
const loginPage = ref(1)
const loginLoading = ref(false)

const eventFilters = reactive({ username: '', module: '', range: [] })
const events = ref([])
const eventTotal = ref(0)
const eventPage = ref(1)
const eventLoading = ref(false)

async function loadLogin() {
  loginLoading.value = true
  try {
    const data = await adminApi.loginLogs({
      username: loginFilters.username || undefined,
      success: loginFilters.success === '' ? undefined : loginFilters.success,
      start: loginFilters.range?.[0] || undefined,
      end: loginFilters.range?.[1] || undefined,
      pageNum: loginPage.value,
      pageSize
    })
    loginLogs.value = data.list || []
    loginTotal.value = data.total || 0
  } finally {
    loginLoading.value = false
  }
}

async function loadEvents() {
  eventLoading.value = true
  try {
    const data = await adminApi.usageEvents({
      username: eventFilters.username || undefined,
      module: eventFilters.module || undefined,
      start: eventFilters.range?.[0] || undefined,
      end: eventFilters.range?.[1] || undefined,
      pageNum: eventPage.value,
      pageSize
    })
    events.value = data.list || []
    eventTotal.value = data.total || 0
  } finally {
    eventLoading.value = false
  }
}

function searchLogin() {
  loginPage.value = 1
  loadLogin()
}

function resetLogin() {
  loginFilters.username = ''
  loginFilters.success = ''
  loginFilters.range = []
  searchLogin()
}

function searchEvent() {
  eventPage.value = 1
  loadEvents()
}

function resetEvent() {
  eventFilters.username = ''
  eventFilters.module = ''
  eventFilters.range = []
  searchEvent()
}

function onTabChange(name) {
  if (name === 'events' && !events.value.length) {
    loadEvents()
  }
}

function datetime(value) {
  return value ? String(value).replace('T', ' ') : '—'
}

onMounted(() => {
  loadLogin()
  loadEvents()
})
</script>

<style scoped>
.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
}
</style>
