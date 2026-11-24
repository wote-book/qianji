<template>
  <el-container class="admin-layout">
    <el-aside width="240px" class="aside">
      <div class="brand">
        <div class="brand-mark">Q</div>
        <div>
          <div class="brand-name">钱迹</div>
          <div class="brand-sub">管理控制台</div>
        </div>
      </div>

      <el-menu
        :default-active="active"
        class="menu"
        background-color="transparent"
        text-color="#a8a29a"
        active-text-color="#fffdf8"
        router
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-foot">
        <div class="dot" />
        <span>运营数据每日实时统计</span>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="header-title">{{ pageTitle }}</div>
          <span class="admin-tag">管理员</span>
        </div>
        <div class="header-right">
          <div class="user-meta">
            <div class="user-name">{{ store.nickname }}</div>
            <div class="user-role">平台管理员</div>
          </div>
          <el-button class="logout-btn" text @click="onLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出登录</span>
          </el-button>
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
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const menus = [
  { path: '/admin/overview', title: '运营总览', icon: 'DataAnalysis' },
  { path: '/admin/users', title: '用户管理', icon: 'UserFilled' },
  { path: '/admin/usage', title: '使用分析', icon: 'TrendCharts' },
  { path: '/admin/logs', title: '系统日志', icon: 'Document' }
]

const active = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '运营总览')

async function onLogout() {
  await ElMessageBox.confirm('确认退出管理控制台？', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await store.logout()
  router.replace('/login')
}
</script>

<style scoped>
.admin-layout {
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
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
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
  flex-shrink: 0;
}

.brand-name {
  font-size: 15px;
  font-weight: 600;
  color: #fffdf8;
  white-space: nowrap;
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
  font-weight: 500;
  box-shadow: inset 2px 0 0 0 #c9a24d;
}

.aside-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 11.5px;
  color: #7d776d;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-success);
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
  color: var(--text-main);
}

.admin-tag {
  font-size: 11px;
  font-weight: 500;
  color: #7a5c1f;
  background: #f6efe0;
  border: 1px solid #e8d9b8;
  border-radius: 4px;
  padding: 2px 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-meta {
  text-align: right;
  line-height: 1.25;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
}

.user-role {
  font-size: 11px;
  color: var(--text-sub);
}

.logout-btn {
  color: var(--text-sub);
  font-size: 13px;
  gap: 4px;
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
