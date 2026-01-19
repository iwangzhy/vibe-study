<template>
  <div class="login-container">
    <!-- 左侧品牌展示 -->
    <div class="login-left">
      <div class="brand-content">
        <h1 class="brand-title">Vibe Social</h1>
        <p class="brand-subtitle">连接世界，分享生活</p>
        <div class="features">
          <div class="feature-item">
            <el-icon :size="32" color="#1890ff"><ChatDotRound /></el-icon>
            <h3>即时通讯</h3>
            <p>与好友实时聊天互动</p>
          </div>
          <div class="feature-item">
            <el-icon :size="32" color="#1890ff"><PictureFilled /></el-icon>
            <h3>动态分享</h3>
            <p>记录美好时刻</p>
          </div>
          <div class="feature-item">
            <el-icon :size="32" color="#1890ff"><Star /></el-icon>
            <h3>兴趣社区</h3>
            <p>发现志同道合的人</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="login-right">
      <div class="login-box">
        <h2 class="login-title">账号登录</h2>
        <p class="login-subtitle">欢迎回来！请登录您的账号</p>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
        >
          <el-form-item prop="account">
            <el-input
              v-model="loginForm.account"
              placeholder="手机号/邮箱/用户名"
              prefix-icon="User"
              size="large"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              prefix-icon="Lock"
              size="large"
              show-password
              clearable
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item class="form-options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <a href="#" class="forgot-link">忘记密码？</a>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-button"
              :loading="loading"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>

          <div class="register-tip">
            还没有账号？
            <router-link to="/register" class="register-link">立即注册</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import { login } from '@/api/user'
import { ChatDotRound, PictureFilled, Star } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(true)

const loginForm = reactive({
  account: '',
  password: ''
})

const loginRules: FormRules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ]
}

// 登录
const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true
        const res = await login(loginForm)
        
        // 保存登录信息
        userStore.setLoginInfo(res.data)
        
        ElMessage.success('登录成功')
        
        // 跳转
        const redirect = route.query.redirect as string
        router.push(redirect || '/home')
      } catch (error) {
        console.error('Login error:', error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  min-height: 100vh;
  background-color: #f0f2f5;

  // 左侧品牌展示区
  .login-left {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 60px;

    .brand-content {
      max-width: 600px;
      color: #fff;

      .brand-title {
        margin: 0 0 16px;
        font-size: 48px;
        font-weight: bold;
      }

      .brand-subtitle {
        margin: 0 0 80px;
        font-size: 24px;
        opacity: 0.9;
      }

      .features {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 40px;

        .feature-item {
          text-align: center;

          h3 {
            margin: 16px 0 8px;
            font-size: 18px;
            font-weight: 600;
          }

          p {
            margin: 0;
            font-size: 14px;
            opacity: 0.8;
          }
        }
      }
    }
  }

  // 右侧登录表单区
  .login-right {
    width: 480px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #fff;
    padding: 60px;

    .login-box {
      width: 100%;

      .login-title {
        margin: 0 0 12px;
        font-size: 28px;
        font-weight: bold;
        color: #333;
      }

      .login-subtitle {
        margin: 0 0 40px;
        font-size: 14px;
        color: #666;
      }

      .login-form {
        .form-options {
          display: flex;
          align-items: center;
          justify-content: space-between;

          .forgot-link {
            color: #1890ff;
            font-size: 14px;
            text-decoration: none;

            &:hover {
              text-decoration: underline;
            }
          }
        }

        .login-button {
          width: 100%;
          margin-top: 8px;
        }

        .register-tip {
          margin-top: 24px;
          text-align: center;
          font-size: 14px;
          color: #666;

          .register-link {
            color: #1890ff;
            text-decoration: none;
            font-weight: 500;

            &:hover {
              text-decoration: underline;
            }
          }
        }
      }
    }
  }
}

// 响应式设计
@media (max-width: 1024px) {
  .login-container {
    .login-left {
      display: none;
    }

    .login-right {
      flex: 1;
      width: 100%;
    }
  }
}
</style>
