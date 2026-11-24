<template>
  <div class="not-found">
    <div class="code">404</div>
    <h2>页面走丢了</h2>
    <p class="muted">你访问的地址不存在，或者该资源不属于当前账号。</p>
    <div class="actions">
      <el-button type="primary" @click="goHome">返回看板</el-button>
      <el-button @click="goLogin">去登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/dashboard')
}

function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: var(--page-bg);
  text-align: center;
}

.code {
  font-size: 84px;
  font-weight: 600;
  letter-spacing: -2px;
  color: var(--text-main);
}

h2 {
  margin: 0;
  font-size: 22px;
}

.actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
</style>
