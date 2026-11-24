<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">个人设置</h2>
        <p class="page-desc">维护昵称、安全问答与登录密码。修改密码后当前会话会立即失效。</p>
      </div>
    </div>

    <div class="grid grid-2">
      <div class="card">
        <div class="card-title">基本信息</div>
        <div class="profile-head">
          <div class="avatar">{{ initial }}</div>
          <div>
            <div class="profile-name">{{ profile.nickname || profile.username }}</div>
            <div class="muted">{{ profile.username }} · {{ profile.role === 'ADMIN' ? '管理员' : '普通用户' }}</div>
          </div>
        </div>
        <el-form label-position="top" style="margin-top: 20px">
          <el-form-item label="用户名">
            <el-input :model-value="profile.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="nickname" maxlength="30" placeholder="展示在系统右上角" />
          </el-form-item>
          <el-form-item label="注册时间">
            <el-input :model-value="(profile.createTime || '').replace('T', ' ')" disabled />
          </el-form-item>
          <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存基本信息</el-button>
        </el-form>
      </div>

      <div class="card">
        <div class="card-title">修改密码</div>
        <el-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" label-position="top">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6~32 位" />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirm">
            <el-input v-model="passwordForm.confirm" type="password" show-password placeholder="再次输入新密码" />
          </el-form-item>
          <el-button type="primary" :loading="savingPassword" @click="changePassword">修改密码</el-button>
        </el-form>
      </div>
    </div>

    <div class="card section-gap">
      <div class="card-title">
        <span>安全问题</span>
        <span class="sub">{{ profile.securityQuestion ? `当前问题：${profile.securityQuestion}` : '尚未设置' }}</span>
      </div>
      <el-alert
        title="设置安全问题后，忘记密码时可在登录页自助重置。答案将以 BCrypt 加密存储，管理员也无法查看。"
        type="info"
        show-icon
        :closable="false"
        style="margin-bottom: 18px"
      />
      <el-form label-position="top" class="security-form">
        <el-form-item label="安全问题">
          <el-input v-model="security.question" maxlength="100" placeholder="例如：我的第一只宠物名字？" />
        </el-form-item>
        <el-form-item label="安全问题答案">
          <el-input v-model="security.answer" maxlength="100" placeholder="设置新答案，留空则清除安全问题" />
        </el-form-item>
        <el-button type="primary" plain :loading="savingSecurity" @click="saveSecurity">保存安全问题</el-button>
      </el-form>
    </div>

    <div class="card section-gap">
      <div class="card-title">账号安全建议</div>
      <ul class="notes">
        <li>密码建议包含字母、数字与符号，避免与其它网站重复。</li>
        <li>会话默认 30 分钟无操作过期，公共电脑使用后请主动退出。</li>
        <li>系统所有业务数据按 user_id 隔离，任何越权访问都会返回 404。</li>
        <li>如账号被管理员禁用，登录时会收到明确提示。</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const store = useUserStore()
const profile = ref({})
const nickname = ref('')
const security = reactive({ question: '', answer: '' })
const savingProfile = ref(false)
const savingPassword = ref(false)
const savingSecurity = ref(false)
const passwordRef = ref()

const passwordForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '新密码长度需在 6~32 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const initial = computed(() => (profile.value.nickname || profile.value.username || 'U').slice(0, 1).toUpperCase())

async function load() {
  profile.value = await userApi.profile()
  nickname.value = profile.value.nickname
  security.question = ''
  security.answer = ''
}

async function saveProfile() {
  if (!nickname.value.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  savingProfile.value = true
  try {
    await userApi.updateProfile({ nickname: nickname.value.trim() })
    ElMessage.success('昵称已更新')
    await load()
    store.setUser({ ...store.user, nickname: nickname.value.trim() })
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  const valid = await passwordRef.value.validate().catch(() => false)
  if (!valid) return
  savingPassword.value = true
  try {
    await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码已修改，请重新登录')
    store.setUser(null)
    router.replace('/login')
  } finally {
    savingPassword.value = false
  }
}

async function saveSecurity() {
  if (!security.question.trim() && security.answer.trim()) {
    ElMessage.warning('设置答案时必须填写安全问题')
    return
  }
  savingSecurity.value = true
  try {
    await userApi.updateSecurity({
      question: security.question.trim(),
      answer: security.answer.trim()
    })
    ElMessage.success('安全问题已更新')
    await load()
  } finally {
    savingSecurity.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.profile-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  background: var(--raised-bg);
  border: 1px solid var(--border);
  border-radius: 8px;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: var(--el-color-primary);
  color: #fffdf8;
  font-size: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-name {
  font-size: 17px;
  font-weight: 600;
}

.security-form {
  max-width: 560px;
}

.notes {
  margin: 0;
  padding-left: 18px;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 2;
}
</style>
