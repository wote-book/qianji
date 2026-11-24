<template>
  <div class="auth-page">
    <aside class="auth-visual">
      <div class="visual-brand">
        <span class="visual-mark">
          <el-icon><MagicStick /></el-icon>
        </span>
        <span class="visual-name">钱迹</span>
      </div>

      <div class="visual-stage">
        <LoginCharacters
          :password-length="charPasswordLength"
          :show-password="charShowPassword"
          :typing="charTyping"
        />
      </div>

      <div class="visual-foot">
        <button type="button" class="foot-link" @click="openDoc('privacy')">隐私政策</button>
        <button type="button" class="foot-link" @click="openDoc('terms')">服务条款</button>
        <button type="button" class="foot-link" @click="openDoc('contact')">联系我们</button>
      </div>
    </aside>

    <main class="auth-panel">
      <div class="panel-inner">
        <div class="auth-head">
          <h1>{{ tab === 'login' ? '登录钱迹' : '创建账号' }}</h1>
          <p>{{ tab === 'login' ? '使用本地账号继续管理你的账本' : '注册后自动创建 12 个预设分类' }}</p>
        </div>

        <el-tabs v-model="tab" class="auth-tabs" @tab-change="resetForm">
          <el-tab-pane label="登录" name="login">
            <el-form ref="loginRef" :model="loginForm" :rules="loginRules" label-position="top" class="auth-form" @submit.prevent>
              <el-form-item label="用户名" prop="username">
                <el-input
                  v-model="loginForm.username"
                  size="large"
                  placeholder="请输入用户名"
                  :prefix-icon="User"
                  autocomplete="off"
                  @focus="typing = true"
                  @blur="typing = false"
                />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="loginForm.password"
                  size="large"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="请输入密码"
                  :prefix-icon="Lock"
                  autocomplete="off"
                  @focus="typing = true"
                  @blur="typing = false"
                  @keyup.enter="doLogin"
                >
                  <template #suffix>
                    <el-icon class="pw-toggle" @click.stop="showPassword = !showPassword">
                      <View v-if="!showPassword" />
                      <Hide v-else />
                    </el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <div class="form-row">
                <span class="hint">演示账号：demo / 123456</span>
                <el-link type="primary" class="forgot" @click="openForgot">忘记密码？</el-link>
              </div>

              <el-button class="btn-primary" :loading="loading" @click="doLogin">登录</el-button>
              <el-button class="btn-ghost" @click="loginAsDemo">使用演示账号登录</el-button>

              <div class="switch-line">
                还没有账号？
                <el-link class="switch-link" @click="tab = 'register'">立即注册</el-link>
              </div>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form ref="registerRef" :model="registerForm" :rules="registerRules" label-position="top" class="auth-form" @submit.prevent>
              <el-form-item label="用户名" prop="username">
                <el-input v-model="registerForm.username" size="large" placeholder="4~20 位字母、数字或下划线" :prefix-icon="User" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="registerForm.password" size="large" type="password" show-password placeholder="6~32 位" :prefix-icon="Lock" />
              </el-form-item>
              <el-form-item label="昵称">
                <el-input v-model="registerForm.nickname" size="large" placeholder="选填，默认与用户名一致" :prefix-icon="Avatar" />
              </el-form-item>
              <el-form-item label="安全问题（选填，用于找回密码）">
                <el-input v-model="registerForm.securityQuestion" size="large" placeholder="例如：我的第一只宠物名字？" />
              </el-form-item>
              <el-form-item label="安全问题答案（选填）">
                <el-input v-model="registerForm.securityAnswer" size="large" placeholder="设置后可通过安全问题重置密码" />
              </el-form-item>
              <el-button class="btn-primary" :loading="loading" @click="doRegister">创建账号</el-button>
              <div class="switch-line">
                已有账号？
                <el-link class="switch-link" @click="tab = 'login'">去登录</el-link>
              </div>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <div class="auth-foot">
          <div>钱迹 · 软件工程课程设计项目 · 数据仅存储于本地 MySQL</div>
          <div class="auth-foot-links">
            <button type="button" class="foot-link" @click="openDoc('privacy')">隐私政策</button>
            <button type="button" class="foot-link" @click="openDoc('terms')">服务条款</button>
            <button type="button" class="foot-link" @click="openDoc('contact')">联系我们</button>
          </div>
        </div>
      </div>
    </main>

    <el-dialog v-model="forgotVisible" title="通过安全问题重置密码" width="440px">
      <el-form :model="forgotForm" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="forgotForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="安全问题答案">
          <el-input v-model="forgotForm.securityAnswer" placeholder="注册时设置的安全问题答案" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="forgotForm.newPassword"
            :type="forgotShowPassword ? 'text' : 'password'"
            placeholder="6~32 位"
            @focus="forgotTyping = true"
            @blur="forgotTyping = false"
          >
            <template #suffix>
              <el-icon class="pw-toggle" @click.stop="forgotShowPassword = !forgotShowPassword">
                <View v-if="!forgotShowPassword" />
                <Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="doReset">重置密码</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="docVisible" :title="doc.title" width="540px" class="doc-dialog">
      <div class="doc-body">
        <div v-for="(sec, index) in doc.sections" :key="index" class="doc-sec">
          <h4>{{ sec.h }}</h4>
          <p v-for="(line, i) in sec.p" :key="i">{{ line }}</p>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="docVisible = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Avatar, Hide, Lock, MagicStick, User, View } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'
import LoginCharacters from '@/components/LoginCharacters.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const tab = ref(route.meta.register ? 'register' : 'login')
const loading = ref(false)
const loginRef = ref()
const registerRef = ref()
const showPassword = ref(false)
const typing = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  securityQuestion: '',
  securityAnswer: ''
})
const forgotVisible = ref(false)
const forgotShowPassword = ref(false)
const forgotTyping = ref(false)
const forgotForm = reactive({ username: '', securityAnswer: '', newPassword: '' })

const docVisible = ref(false)
const doc = reactive({ title: '', sections: [] })

const DOCS = {
  privacy: {
    title: '隐私政策',
    sections: [
      {
        h: '一、我们收集哪些数据',
        p: [
          '账号信息：用户名、昵称，以及经 BCrypt 加密后的登录密码与安全问题答案。',
          '记账数据：账户、分类、收支流水、预算、理财目标与智能简报记录。',
          '使用统计：登录时间、最近访问模块、功能使用次数等运行所需的元数据。'
        ]
      },
      {
        h: '二、数据如何存储',
        p: [
          '所有数据存储于本项目部署所在环境的本地 MySQL 数据库，不上传至任何云端存储。',
          '密码与安全问题答案均以不可逆加密形式保存，系统内任何角色都无法查看明文。'
        ]
      },
      {
        h: '三、数据如何使用',
        p: [
          '数据仅用于向你本人展示账本、统计图表与智能简报，不会用于广告投放或对外销售。',
          '管理员仅能查看账号元数据与使用规模的数量、聚合指标，无法查看具体金额与明细。'
        ]
      },
      {
        h: '四、第三方服务',
        p: [
          '智能简报与 AI 记账在启用时会将必要的文本片段发送至所配置的大模型接口用于生成结果。',
          '接口密钥仅保存在服务端配置中，不会出现在页面或浏览器可访问的位置。'
        ]
      },
      {
        h: '五、你的权利',
        p: [
          '你可以随时修改昵称、密码与安全问题，导出或删除自己的记录，并注销不再使用的账号。',
          '如需彻底清除数据，可联系管理员在后端数据库中删除对应账号记录。'
        ]
      }
    ]
  },
  terms: {
    title: '服务条款',
    sections: [
      {
        h: '一、服务说明',
        p: [
          '钱迹是软件工程课程设计项目，用于演示个人记账系统的完整功能，不构成商业服务。',
          '项目可能随课程进度调整功能或暂停对外访问，恕不另行个别通知。'
        ]
      },
      {
        h: '二、账号与安全',
        p: [
          '请妥善保管账号密码，因个人保管不当造成的泄露由使用者自行承担。',
          '忘记密码时可通过注册时设置的安全问题自助重置；管理员也可为普通用户生成一次性临时密码。',
          '管理员有权对违反使用规范的账号进行禁用处理，禁用后该账号将立即退出登录。'
        ]
      },
      {
        h: '三、使用规范',
        p: [
          '不得利用本系统从事任何违法违规活动，不得上传含有恶意内容的数据。',
          '不得尝试越权访问他人数据或绕过身份校验，系统会按账号隔离全部业务数据。'
        ]
      },
      {
        h: '四、数据与备份',
        p: [
          '建议定期通过「报表」页面导出 CSV 备份重要账目。',
          '因误删除、设备故障或数据库重建造成的数据丢失，本项目不承担赔偿责任。'
        ]
      },
      {
        h: '五、免责声明',
        p: [
          '系统提供的统计、预算与简报结果仅供参考，不构成任何投资或理财建议。',
          '因不可抗力或第三方服务（如大模型接口）异常导致的功能不可用，本项目不承担责任。'
        ]
      }
    ]
  },
  contact: {
    title: '联系我们',
    sections: [
      {
        h: '项目信息',
        p: ['钱迹 · 个人理财管理系统', '软件工程课程设计项目 · 数据仅存储于本地 MySQL']
      },
      {
        h: '反馈渠道',
        p: [
          '功能建议与缺陷反馈：请通过课程项目仓库的 Issue 提交，或直接联系项目成员。',
          '账号相关问题（如无法登录、需要清除数据）：请联系系统管理员处理。'
        ]
      },
      {
        h: '联系邮箱',
        p: ['qianji.project@example.com']
      },
      {
        h: '响应说明',
        p: ['课程周期内一般在 1~2 个工作日内回复；寒暑假期间可能有所延迟，敬请谅解。']
      }
    ]
  }
}

function openDoc(key) {
  const target = DOCS[key]
  if (!target) return
  doc.title = target.title
  doc.sections = target.sections
  docVisible.value = true
}

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,20}$/, message: '用户名为 4~20 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6~32 位', trigger: 'blur' }
  ]
}

const redirect = computed(() => route.query.redirect || '/dashboard')

// 角色动画跟随当前正在编辑的密码框，避免弹窗内查看/输入密码时角色无反应
const charPasswordLength = computed(() =>
  forgotVisible.value ? forgotForm.newPassword.length : loginForm.password.length
)
const charShowPassword = computed(() => (forgotVisible.value ? forgotShowPassword.value : showPassword.value))
const charTyping = computed(() => (forgotVisible.value ? forgotTyping.value : typing.value))

function openForgot() {
  forgotShowPassword.value = false
  forgotTyping.value = false
  forgotVisible.value = true
}

function resetForm() {
  typing.value = false
  showPassword.value = false
  loginRef.value?.clearValidate()
  registerRef.value?.clearValidate()
}

async function doLogin() {
  const valid = await loginRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await store.login({ ...loginForm })
    ElMessage.success('登录成功')
    router.replace(store.isAdmin ? '/admin/overview' : redirect.value)
  } finally {
    loading.value = false
  }
}

async function loginAsDemo() {
  loginForm.username = 'demo'
  loginForm.password = '123456'
  showPassword.value = false
  await doLogin()
}

async function doRegister() {
  const valid = await registerRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await store.register({ ...registerForm })
    ElMessage.success('注册成功，已自动登录')
    router.replace('/onboarding')
  } finally {
    loading.value = false
  }
}

async function doReset() {
  if (!forgotForm.username || !forgotForm.securityAnswer || !forgotForm.newPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }
  loading.value = true
  try {
    await authApi.resetPassword({ ...forgotForm })
    ElMessage.success('密码已重置，请使用新密码登录')
    forgotVisible.value = false
    tab.value = 'login'
    loginForm.username = forgotForm.username
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  background: #fff;
}

.auth-visual {
  position: relative;
  flex: 1;
  min-width: 0;
  background: #f4f4f5;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 44px 48px;
  overflow: hidden;
}

.visual-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 600;
  color: #18181b;
}

.visual-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(24, 24, 27, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #18181b;
  font-size: 16px;
}

.visual-stage {
  flex: 1;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  min-height: 0;
  padding-bottom: 32px;
}

.visual-foot {
  display: flex;
  gap: 28px;
  font-size: 13px;
  color: #a1a1aa;
}

.foot-link {
  padding: 0;
  border: 0;
  background: none;
  font: inherit;
  color: inherit;
  cursor: pointer;
  transition: color 0.2s;
}

.foot-link:hover,
.foot-link:focus-visible {
  color: #18181b;
}

.foot-link:focus-visible {
  outline: 1px solid #a1a1aa;
  outline-offset: 4px;
  border-radius: 2px;
}

.doc-body {
  max-height: 62vh;
  overflow-y: auto;
  padding-right: 4px;
}

.doc-sec + .doc-sec {
  margin-top: 18px;
}

.doc-sec h4 {
  margin: 0 0 6px;
  font-size: 13.5px;
  font-weight: 600;
  color: #18181b;
}

.doc-sec p {
  margin: 0 0 6px;
  font-size: 13px;
  line-height: 1.85;
  color: #52525b;
}

.auth-panel {
  width: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 48px;
  background: #fff;
}

.panel-inner {
  width: 100%;
  max-width: 400px;
}

.auth-head {
  text-align: center;
  margin-bottom: 28px;
}

.auth-head h1 {
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 0;
  color: #18181b;
  margin-bottom: 8px;
}

.auth-head p {
  font-size: 13.5px;
  color: #71717a;
}

.auth-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #e4e4e7;
}

.auth-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  color: #71717a;
}

.auth-tabs :deep(.el-tabs__item.is-active) {
  color: #18181b;
  font-weight: 600;
}

.auth-tabs :deep(.el-tabs__active-bar) {
  background: #18181b;
}

.auth-form {
  margin-top: 20px;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.auth-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 500;
  color: #3f3f46;
  padding-bottom: 6px;
}

.auth-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 4px 14px;
  box-shadow: 0 0 0 1px #e4e4e7 inset;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #18181b inset;
}

.auth-form :deep(.el-input__inner) {
  height: 38px;
}

.pw-toggle {
  cursor: pointer;
  color: #a1a1aa;
  transition: color 0.2s;
}

.pw-toggle:hover {
  color: #18181b;
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 16px;
  font-size: 12px;
}

.hint {
  color: #a1a1aa;
}

.forgot {
  font-size: 12px;
}

.btn-primary {
  width: 100%;
  height: 44px;
  border-radius: 6px;
  border-color: #18181b;
  background: #18181b;
  color: #fff;
  font-size: 14.5px;
  font-weight: 600;
  margin: 0;
}

.btn-primary:hover,
.btn-primary:focus {
  background: #27272a;
  border-color: #27272a;
  color: #fff;
}

.btn-ghost {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  border-color: #e4e4e7;
  background: #fff;
  color: #3f3f46;
  font-size: 14px;
  font-weight: 500;
  margin: 10px 0 0;
}

.btn-ghost:hover,
.btn-ghost:focus {
  border-color: #a1a1aa;
  background: #fafafa;
  color: #18181b;
}

.switch-line {
  margin-top: 22px;
  text-align: center;
  font-size: 13px;
  color: #71717a;
}

.switch-link {
  font-size: 13px;
  font-weight: 600;
  color: #18181b;
}

.auth-foot {
  margin-top: 28px;
  text-align: center;
  font-size: 11.5px;
  color: #a1a1aa;
}

.auth-foot-links {
  display: none;
  justify-content: center;
  gap: 18px;
  margin-top: 10px;
  font-size: 12px;
}

@media (max-width: 1080px) {
  .auth-visual {
    display: none;
  }

  .auth-panel {
    width: 100%;
  }

  .auth-foot-links {
    display: flex;
  }
}

@media (max-width: 1400px) {
  .visual-stage {
    transform: scale(0.92);
  }
}
</style>
