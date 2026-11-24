<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">报表与备份</h2>
        <p class="page-desc">导出区间流水 Excel、生成月度 PDF 财务报表，也可以完成整库备份与恢复。</p>
      </div>
    </div>

    <div class="grid grid-2">
      <div class="card">
        <div class="card-title">
          <span>数据导出</span>
          <span class="sub">按日期区间导出</span>
        </div>
        <div class="inline-form" style="margin-bottom: 18px">
          <el-date-picker
            v-model="range"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 280px"
          />
        </div>
        <div class="export-actions">
          <div class="export-item">
            <div class="export-icon excel"><el-icon><Document /></el-icon></div>
            <div class="export-text">
              <b>流水明细 Excel</b>
              <span>包含日期、类型、分类、账户、金额、来源与备注</span>
            </div>
            <el-button type="primary" plain :loading="exporting === 'excel'" @click="exportExcel">导出</el-button>
          </div>
          <div class="export-item">
            <div class="export-icon pdf"><el-icon><Files /></el-icon></div>
            <div class="export-text">
              <b>月度财务 PDF</b>
              <span>收入、支出、结余、分类占比与预算执行摘要</span>
            </div>
            <el-button type="primary" plain :loading="exporting === 'pdf'" @click="exportPdf">生成</el-button>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-title">
          <span>数据备份</span>
          <span class="sub">JSON 全量五表</span>
        </div>
        <div class="export-actions">
          <div class="export-item">
            <div class="export-icon backup"><el-icon><Download /></el-icon></div>
            <div class="export-text">
              <b>导出备份</b>
              <span>导出账户、分类、流水、预算、目标（不含账号密码）</span>
            </div>
            <el-button type="primary" plain :loading="exporting === 'backup'" @click="exportBackup">下载</el-button>
          </div>
          <el-upload :auto-upload="false" :show-file-list="false" accept=".json,application/json" :on-change="handleImport">
            <div class="export-item clickable">
              <div class="export-icon restore"><el-icon><Upload /></el-icon></div>
              <div class="export-text">
                <b>导入备份</b>
                <span>按名称复用账户与分类，重复流水自动去重</span>
              </div>
              <el-button type="primary" plain :loading="importing">选择文件</el-button>
            </div>
          </el-upload>
        </div>
        <el-alert
          v-if="importResult"
          :title="`导入完成：新增账户 ${importResult.accountCreated}，新增分类 ${importResult.categoryCreated}，流水新增 ${importResult.recordInserted}、重复跳过 ${importResult.recordSkipped}、失败 ${importResult.recordFailed}；目标新增 ${importResult.goalCreated}、已存在 ${importResult.goalSkipped}`"
          type="success"
          show-icon
          :closable="false"
          style="margin-top: 14px"
        />
      </div>
    </div>

    <div class="grid grid-2 section-gap">
      <div class="card">
        <div class="card-title">导出说明</div>
        <ul class="notes">
          <li>Excel 使用 EasyExcel 流式写出，字段与列表页一致，来源标记帮助追溯数据。</li>
          <li>PDF 使用 OpenPDF 生成，内置中文字体，包含分类占比与预算执行摘要。</li>
          <li>导出范围包含起止日期当天，若起始晚于结束会提示错误。</li>
          <li>所有导出仅包含当前登录用户的数据，越权查询返回 404。</li>
        </ul>
      </div>

      <div class="card">
        <div class="card-title">备份说明</div>
        <ul class="notes">
          <li>备份文件为 JSON，可在另一账号恢复；账户与分类按名称复用，不会重复创建。</li>
          <li>流水恢复使用去重指纹，同一笔数据重复导入会被跳过。</li>
          <li>恢复后系统会按流水重算账户余额，保证与明细一致。</li>
          <li>用户表不参与备份，账号安全信息不会泄露。</li>
        </ul>
      </div>
    </div>

    <div class="card section-gap">
      <div class="card-title">
        <span>事务一致性演示</span>
        <span class="sub">仅在 demo profile 可用</span>
      </div>
      <div class="demo-row">
        <div>
          <b>余额不足时整笔回滚</b>
          <p class="muted">创建一个零余额账户并尝试支出 100 元，验证「流水不落库、余额不变」的同事务红线。</p>
        </div>
        <el-button :loading="demoLoading" @click="runDemo">运行演示</el-button>
      </div>
      <el-alert
        v-if="demoResult"
        :title="`${demoResult.message}｜余额 ${demoResult.balanceBefore} → ${demoResult.balanceAfter}，流水 ${demoResult.recordsBefore} → ${demoResult.recordsAfter}，回滚成功：${demoResult.rolledBack ? '是' : '否'}`"
        :type="demoResult.rolledBack ? 'success' : 'warning'"
        show-icon
        :closable="false"
        style="margin-top: 12px"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { backupApi, demoApi, downloadBlob, exportApi } from '@/api'
import { currentMonth, monthRange } from '@/utils/format'

const { start, end } = monthRange(currentMonth())
const range = ref([start, end])
const exporting = ref('')
const importing = ref(false)
const importResult = ref(null)
const demoLoading = ref(false)
const demoResult = ref(null)

function ensureRange() {
  if (!range.value?.[0] || !range.value?.[1]) {
    ElMessage.warning('请选择日期区间')
    return null
  }
  return range.value
}

async function exportExcel() {
  const value = ensureRange()
  if (!value) return
  exporting.value = 'excel'
  try {
    const blob = await exportApi.recordsExcel(value[0], value[1])
    downloadBlob(blob, `流水明细_${value[0]}_${value[1]}.xlsx`)
    ElMessage.success('Excel 已开始下载')
  } finally {
    exporting.value = ''
  }
}

async function exportPdf() {
  const value = ensureRange()
  if (!value) return
  exporting.value = 'pdf'
  try {
    const [year, month] = value[0].split('-')
    const blob = await exportApi.reportPdf(value[0], value[1])
    downloadBlob(blob, `财务报表_${year}${month}.pdf`)
    ElMessage.success('PDF 已开始下载')
  } finally {
    exporting.value = ''
  }
}

async function exportBackup() {
  exporting.value = 'backup'
  try {
    const data = await backupApi.exportData()
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    downloadBlob(blob, `理财数据备份_${new Date().toISOString().slice(0, 10)}.json`)
    ElMessage.success('备份已下载')
  } finally {
    exporting.value = ''
  }
}

async function handleImport(file) {
  if (!file.raw) return
  importing.value = true
  try {
    const text = await file.raw.text()
    let payload
    try {
      payload = JSON.parse(text)
    } catch (e) {
      ElMessage.error('备份文件不是合法 JSON')
      return
    }
    importResult.value = await backupApi.importData(payload)
    ElMessage.success('备份导入完成')
  } finally {
    importing.value = false
  }
}

async function runDemo() {
  demoLoading.value = true
  try {
    demoResult.value = await demoApi.transactionFailure()
  } catch (e) {
    ElMessage.warning('当前不是 demo 运行环境，事务演示接口未启用')
  } finally {
    demoLoading.value = false
  }
}
</script>

<style scoped>
.export-actions {
  display: grid;
  gap: 14px;
}

.export-item {
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
  background: var(--raised-bg);
}

.export-item.clickable {
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.export-item.clickable:hover {
  border-color: var(--border-strong);
  background: #f2eee3;
}

.export-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  background: var(--card-bg);
  border: 1px solid var(--border);
  color: var(--text-sub);
}

.export-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.export-text b {
  font-size: 14px;
}

.export-text span {
  font-size: 12px;
  color: var(--text-sub);
  line-height: 1.6;
}

.notes {
  margin: 0;
  padding-left: 18px;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 2;
}

.demo-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.demo-row p {
  margin: 6px 0 0;
}
</style>
