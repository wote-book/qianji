import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const loading = ref(false)

  const isLogin = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const nickname = computed(() => user.value?.nickname || user.value?.username || '')

  async function fetchMe() {
    loading.value = true
    try {
      user.value = await authApi.me()
      return user.value
    } finally {
      loading.value = false
    }
  }

  async function login(form) {
    user.value = await authApi.login(form)
    return user.value
  }

  async function register(form) {
    user.value = await authApi.register(form)
    return user.value
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch (e) {
      // 忽略退出异常
    }
    user.value = null
  }

  function setUser(data) {
    user.value = data
  }

  return { user, loading, isLogin, isAdmin, nickname, fetchMe, login, register, logout, setUser }
})
