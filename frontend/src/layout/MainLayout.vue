<template>
  <el-container class="layout">
    <el-aside width="240px" class="aside">
      <div class="brand">
        <div class="brand-mark">Q</div>
        <div>
          <div class="brand-name">钱迹</div>
          <div class="brand-sub">个人记账 · 本地数据</div>
        </div>
      </div>
      <div class="aside-actions">
        <el-button type="primary" class="quick-btn" :icon="Plus" @click="openRecord('manual')">记一笔</el-button>
        <el-button class="ai-btn" :icon="MagicStick" @click="openRecord('ai')">AI 记账</el-button>
      </div>
      <el-menu
        ref="menuRef"
        :default-active="active"
        :default-openeds="activeGroup ? [activeGroup] : []"
        class="menu"
        background-color="transparent"
        text-color="#9ca3af"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item v-for="item in leafMenus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
        <el-sub-menu v-for="group in groupMenus" :key="group.title" :index="group.title">
          <template #title>
            <el-icon><component :is="group.icon" /></el-icon>
            <span>{{ group.title }}</span>
          </template>
          <el-menu-item v-for="item in group.children" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="header-title">{{ pageTitle }}</div>
          <el-tag v-if="store.isAdmin" size="small" effect="plain" type="warning" class="admin-tag">管理员</el-tag>
        </div>
        <div class="header-right">
          <el-button text class="header-action" @click="router.push('/onboarding')">
            <el-icon><Compass /></el-icon>
            <span>引导</span>
          </el-button>
          <el-dropdown trigger="click" @command="onCommand">
            <div class="user-chip">
              <div class="avatar">{{ initial }}</div>
              <div class="user-meta">
                <div class="user-name">{{ store.nickname }}</div>
                <div class="user-role">{{ store.isAdmin ? '系统管理员' : '个人用户' }}</div>
              </div>
              <el-icon class="chev"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>个人设置
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <QuickRecord />
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { MagicStick, Plus } from '@element-plus/icons-vue'
import QuickRecord from '@/components/QuickRecord.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const menuRef = ref()

const leafMenus = [
  { path: '/dashboard', title: '数据看板', icon: 'DataLine' }
]

const groupMenus = [
  {
    title: '账本',
    icon: 'Collection',
    children: [
      { path: '/records', title: '收支流水', icon: 'Tickets' },
      { path: '/accounts', title: '账户与对账', icon: 'CreditCard' },
      { path: '/categories', title: '分类管理', icon: 'Grid' }
    ]
  },
  {
    title: '规划',
    icon: 'Aim',
    children: [
      { path: '/budgets', title: '预算管理', icon: 'Wallet' },
      { path: '/goals', title: '理财目标', icon: 'Flag' }
    ]
  },
  {
    title: '分析',
    icon: 'TrendCharts',
    children: [
      { path: '/stats', title: '统计分析', icon: 'PieChart' },
      { path: '/brief', title: '智能简报', icon: 'MagicStick' },
      { path: '/reports', title: '报表与备份', icon: 'Download' }
    ]
  }
]

const active = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '数据看板')
const initial = computed(() => (store.nickname || 'U').slice(0, 1).toUpperCase())

const activeGroup = computed(() => {
  const group = groupMenus.find((g) =>
    g.children.some((c) => route.path === c.path || route.path.startsWith(c.path + '/'))
  )
  return group ? group.title : ''
})

watch(
  () => route.path,
  () => {
    if (activeGroup.value) {
      menuRef.value?.open(activeGroup.value)
    }
  }
)

onMounted(() => {
  if (activeGroup.value) {
    menuRef.value?.open(activeGroup.value)
  }
})

function openRecord(mode) {
  window.dispatchEvent(new CustomEvent('finance:open-record', { detail: { mode } }))
}

async function onCommand(command) {
  if (command === 'settings') {
    router.push('/settings')
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前账号？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await store.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
  background: var(--page-bg);
}

.aside {
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  color: #e9e5dc;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px 16px;
}

.aside-actions {
  display: flex;
  gap: 8px;
  padding: 0 14px 14px;
}

.aside-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.aside-actions .quick-btn {
  flex: 1;
  border-radius: 6px;
}

.aside-actions .ai-btn {
  flex: 1;
  border-radius: 6px;
  background: transparent;
  border-color: rgba(255, 255, 255, 0.16);
  color: #d8d2c6;
}

.aside-actions .ai-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.28);
  color: #fffdf8;
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 17px;
  color: #e9e5dc;
}

.brand-name {
  font-size: 17px;
  font-weight: 600;
  color: #fffdf8;
  letter-spacing: 0.5px;
}

.brand-sub {
  font-size: 11px;
  letter-spacing: 0;
  color: #7d776d;
  margin-top: 2px;
}

.menu {
  border-right: none;
  padding: 10px 12px;
  flex: 1;
  overflow-y: auto;
}

.menu :deep(.el-menu-item) {
  height: 36px;
  border-radius: 6px;
  margin-bottom: 2px;
  font-size: 13px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.menu :deep(.el-menu-item.is-active) {
  background: var(--sidebar-surface);
  color: #fffdf8;
  box-shadow: inset 2px 0 0 0 #8fa6b8;
}

.menu :deep(.el-sub-menu__title) {
  height: 36px;
  border-radius: 6px;
  margin-bottom: 2px;
  font-size: 13px;
}

.menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.menu :deep(.el-sub-menu .el-menu) {
  background: transparent;
}

.menu :deep(.el-sub-menu .el-menu-item) {
  height: 32px;
  min-width: 0;
  padding-left: 38px !important;
  font-size: 12.5px;
}

.menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #fff;
}

.header {
  height: 64px;
  background: var(--card-bg);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.admin-tag {
  border-radius: 4px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-action {
  color: var(--text-sub);
  font-size: 13px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px 6px 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.user-chip:hover {
  background: #f2eee3;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--sidebar-active);
  color: #fffdf8;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.user-meta {
  line-height: 1.2;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
}

.user-role {
  font-size: 11px;
  color: var(--text-sub);
  margin-top: 2px;
}

.chev {
  color: var(--text-muted);
  font-size: 12px;
}

.main {
  padding: 0;
  overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
