<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">分类管理</h2>
        <p class="page-desc">分类是统计与预算的基础。预设分类不可删除，但可以停用；自定义分类在未被引用时可删除。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate('common')">新增分类</el-button>
    </div>

    <div class="card">
      <el-tabs v-model="type" @tab-change="load">
        <el-tab-pane label="支出分类" name="EXPENSE" />
        <el-tab-pane label="收入分类" name="INCOME" />
      </el-tabs>

      <div class="grid grid-4">
        <div v-for="item in categories" :key="item.id" class="cat-card" :class="{ disabled: item.status === 0 }">
          <div class="cat-top">
            <div class="cat-icon" :class="type === 'INCOME' ? 'income' : 'expense'">
              <el-icon><component :is="type === 'INCOME' ? Top : Bottom" /></el-icon>
            </div>
            <el-switch :model-value="item.status === 1" size="small" @change="(v) => toggle(item, v)" />
          </div>
          <div class="cat-name">{{ item.name }}</div>
          <div class="cat-meta">
            <el-tag v-if="item.isPreset === 1" size="small" effect="plain" class="tag-soft">预设</el-tag>
            <span>{{ item.recordCount || 0 }} 条流水</span>
          </div>
          <div class="cat-actions">
            <el-button text type="primary" size="small" @click="openEdit(item)">重命名</el-button>
            <el-button text type="danger" size="small" :disabled="item.isPreset === 1" @click="remove(item)">删除</el-button>
          </div>
        </div>

        <div v-if="!categories.length" class="empty-block">
          <el-icon><Grid /></el-icon>
          <div class="empty-title">还没有{{ type === 'INCOME' ? '收入' : '支出' }}分类</div>
          <div class="empty-desc">可以从常用分类中一键添加，也可以自定义分类名称</div>
          <div class="empty-actions">
            <el-button type="primary" :icon="Plus" @click="openCreate('common')">添加常用分类</el-button>
            <el-button @click="openCreate('custom')">自定义添加</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑分类' : '新增分类'" width="560px" destroy-on-close>
      <template v-if="editing">
        <el-form ref="editFormRef" :model="form" :rules="rules" label-width="72px">
          <el-form-item label="类型">
            <el-radio-group v-model="form.type" disabled>
              <el-radio-button value="EXPENSE">支出</el-radio-button>
              <el-radio-button value="INCOME">收入</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="名称" prop="name">
            <el-input v-model="form.name" maxlength="30" placeholder="例如：宠物、旅行" />
          </el-form-item>
        </el-form>
      </template>

      <template v-else>
        <el-form label-width="72px">
          <el-form-item label="类型">
            <el-radio-group v-model="form.type" @change="onTypeChange">
              <el-radio-button value="EXPENSE">支出</el-radio-button>
              <el-radio-button value="INCOME">收入</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>

        <el-tabs v-model="createMode">
          <el-tab-pane label="常用分类" name="common">
            <div class="lib-head">
              <span class="muted">点击选择，可多选；已添加的分类会置灰</span>
              <span class="lib-tools">
                <el-button text type="primary" size="small" :disabled="!selectableCount" @click="selectAll">全选</el-button>
                <el-button text size="small" :disabled="!selected.length" @click="selected = []">清空</el-button>
              </span>
            </div>
            <div class="lib-grid">
              <button
                v-for="name in library[form.type]"
                :key="name"
                type="button"
                class="lib-item"
                :class="{ active: selected.includes(name), existing: isExisting(name) }"
                :disabled="isExisting(name)"
                @click="toggleSelect(name)"
              >
                <el-icon v-if="selected.includes(name)" class="lib-check"><Check /></el-icon>
                <span>{{ name }}</span>
                <span v-if="isExisting(name)" class="lib-added">已添加</span>
              </button>
            </div>
            <div class="lib-foot muted">已选 {{ selected.length }} 个</div>
          </el-tab-pane>

          <el-tab-pane label="自定义分类" name="custom">
            <el-form ref="customFormRef" :model="form" :rules="rules" label-width="72px">
              <el-form-item label="名称" prop="name">
                <el-input v-model="form.name" maxlength="30" placeholder="例如：宠物、旅行" @keyup.enter="save" />
              </el-form-item>
            </el-form>
            <div class="muted" style="padding-left: 72px">自定义分类添加后可在未被流水引用时删除。</div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="!editing && createMode === 'common'" type="primary" :loading="saving" :disabled="!selected.length" @click="saveBatch">
          添加所选（{{ selected.length }}）
        </el-button>
        <el-button v-else type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Grid, Plus, Top, Bottom } from '@element-plus/icons-vue'
import { categoryApi } from '@/api'

const LIBRARY = {
  EXPENSE: [
    '餐饮', '交通', '购物', '居住', '娱乐', '医疗', '教育', '通讯',
    '服饰', '日用', '美容', '旅行', '健身', '宠物', '数码', '人情',
    '办公', '税费', '保险', '其他支出'
  ],
  INCOME: [
    '工资', '奖金', '兼职', '理财收益', '红包', '报销', '退款', '礼金', '其他收入'
  ]
}

const type = ref('EXPENSE')
const categories = ref([])
const allCategories = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const createMode = ref('common')
const selected = ref([])
const saving = ref(false)
const editFormRef = ref()
const customFormRef = ref()

const library = LIBRARY
const form = reactive({ name: '', type: 'EXPENSE' })
const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const existingNames = computed(() => {
  const list = editing.value ? categories.value : allCategories.value
  return new Set(list.filter((c) => c.type === form.type).map((c) => c.name))
})

const selectableCount = computed(() => library[form.type].filter((n) => !isExisting(n)).length)

async function load() {
  categories.value = await categoryApi.list(type.value)
}

async function openCreate(mode = 'common') {
  editing.value = null
  createMode.value = mode
  selected.value = []
  Object.assign(form, { name: '', type: type.value })
  if (mode === 'common') {
    allCategories.value = await categoryApi.list()
  }
  dialogVisible.value = true
}

function openEdit(item) {
  editing.value = item
  Object.assign(form, { name: item.name, type: item.type })
  dialogVisible.value = true
}

function onTypeChange() {
  selected.value = []
}

function isExisting(name) {
  return existingNames.value.has(name)
}

function toggleSelect(name) {
  if (isExisting(name)) return
  const index = selected.value.indexOf(name)
  if (index >= 0) {
    selected.value.splice(index, 1)
  } else {
    selected.value.push(name)
  }
}

function selectAll() {
  selected.value = library[form.type].filter((name) => !isExisting(name))
}

async function saveBatch() {
  if (!selected.value.length) return
  saving.value = true
  try {
    const res = await categoryApi.batchCreate({ type: form.type, names: selected.value })
    const skipped = res.skipped?.length || 0
    ElMessage.success(skipped
      ? `已添加 ${res.added} 个分类，跳过 ${skipped} 个已存在的分类`
      : `已添加 ${res.added} 个分类`)
    dialogVisible.value = false
    if (type.value !== form.type) {
      type.value = form.type
    }
    await load()
  } finally {
    saving.value = false
  }
}

async function save() {
  const valid = await (editing.value ? editFormRef.value : customFormRef.value).validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await categoryApi.update(editing.value.id, { ...form })
      ElMessage.success('分类已更新')
    } else {
      await categoryApi.create({ ...form })
      ElMessage.success('分类已创建')
      if (type.value !== form.type) {
        type.value = form.type
      }
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggle(item, value) {
  await categoryApi.setStatus(item.id, value ? 1 : 0)
  ElMessage.success(value ? '分类已启用' : '分类已停用')
  await load()
}

async function remove(item) {
  await ElMessageBox.confirm(`确认删除分类「${item.name}」？`, '删除分类', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await categoryApi.remove(item.id)
  ElMessage.success('分类已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.cat-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
  background: var(--card-bg);
  transition: border-color 0.15s, box-shadow 0.15s;
}

.cat-card:hover {
  border-color: var(--border-strong);
  box-shadow: 0 2px 6px rgba(35, 33, 29, 0.06);
}

.cat-card.disabled {
  opacity: 0.55;
  background: var(--raised-bg);
}

.cat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cat-icon {
  width: 36px;
  height: 36px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
}

.cat-icon.expense {
  background: #fef2f2;
  color: #dc2626;
}

.cat-icon.income {
  background: #ecfdf5;
  color: #059669;
}

.cat-name {
  font-size: 15px;
  font-weight: 600;
  margin: 12px 0 8px;
}

.cat-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  color: var(--text-sub);
}

.cat-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
  border-top: 1px dashed var(--border);
  padding-top: 8px;
}

.empty-block {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 54px 0;
  color: var(--text-sub);
}

.empty-block .el-icon {
  font-size: 32px;
  color: var(--text-muted);
}

.empty-title {
  margin-top: 12px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
}

.empty-desc {
  margin-top: 6px;
  font-size: 12.5px;
}

.empty-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.lib-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 10px;
}

.lib-tools {
  display: flex;
  align-items: center;
}

.lib-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
  padding: 2px;
}

.lib-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 36px;
  padding: 0 6px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: #fff;
  font-size: 13px;
  color: #475467;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}

.lib-item:hover:not(:disabled) {
  border-color: var(--border-strong);
  color: var(--text-main);
  background: var(--raised-bg);
}

.lib-item.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 600;
}

.lib-item.existing {
  background: var(--raised-bg);
  color: var(--text-muted);
  border-style: dashed;
  cursor: not-allowed;
}

.lib-check {
  font-size: 13px;
}

.lib-added {
  position: absolute;
  right: 5px;
  top: 2px;
  font-size: 9px;
  color: var(--text-muted);
  transform: scale(0.9);
}

.lib-foot {
  margin-top: 12px;
  font-size: 12px;
}
</style>
