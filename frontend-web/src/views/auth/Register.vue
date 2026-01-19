<template>
  <div class="register-container">
    <!-- 左侧品牌展示 -->
    <div class="register-left">
      <div class="brand-content">
        <h1 class="brand-title">加入Vibe</h1>
        <p class="brand-subtitle">开启你的社交之旅</p>
        <div class="stats">
          <div class="stat-item">
            <h2>100K+</h2>
            <p>注册用户</p>
          </div>
          <div class="stat-item">
            <h2>1M+</h2>
            <p>每日互动</p>
          </div>
          <div class="stat-item">
            <h2>500+</h2>
            <p>活跃社区</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧注册表单 -->
    <div class="register-right">
      <div class="register-box">
        <h2 class="register-title">创建账号</h2>
        <p class="register-subtitle">填写信息，快速注册</p>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
        >
          <el-form-item prop="phone">
            <el-input
              v-model="registerForm.phone"
              placeholder="手机号"
              prefix-icon="Phone"
              size="large"
              clearable
            />
          </el-form-item>

          <el-form-item prop="code">
            <div class="code-input-wrapper">
              <el-input
                v-model="registerForm.code"
                placeholder="验证码"
                prefix-icon="Message"
                size="large"
                clearable
                style="flex: 1;"
              />
              <el-button
                :disabled="countdown > 0"
                size="large"
                class="code-button"
                @click="handleSendCode"
              >
                {{ countdown > 0 ? `${countdown}秒` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="设置密码（6-20位）"
              prefix-icon="Lock"
              size="large"
              show-password
              clearable
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="确认密码"
              prefix-icon="Lock"
              size="large"
              show-password
              clearable
            />
          </el-form-item>

          <el-form-item>
            <el-checkbox v-model="agreeTerms">
              我已阅读并同意
              <a href="#" class="terms-link">《用户协议》</a>
              和
              <a href="#" class="terms-link">《隐私政策》</a>
            </el-checkbox>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="register-button"
              :loading="loading"
              :disabled="!agreeTerms"
              @click="handleRegister"
            >
              注册
            </el-button>
          </el-form-item>

          <div class="login-tip">
            已有账号？
            <router-link to="/login" class="login-link">立即登录</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { register, sendCode } from '@/api/user'

const router = useRouter()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const countdown = ref(0)
const agreeTerms = ref(false)

const registerForm = reactive({
  phone: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const validatePhone = (rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const registerRules: FormRules = {
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 发送验证码
const handleSendCode = async () => {
  if (!registerForm.phone) {
    ElMessage.warning('请先输入手机号')
    return
  }

  if (!/^1[3-9]\d{9}$/.test(registerForm.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  try {
    await sendCode(registerForm.phone)
    ElMessage.success('验证码已发送')
    
    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value === 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('Send code error:', error)
  }
}

// 注册
const handleRegister = async () => {
  if (!registerFormRef.value) return

  if (!agreeTerms.value) {
    ElMessage.warning('请阅读并同意用户协议和隐私政策')
    return
  }

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true
        await register(registerForm)
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (error) {
        console.error('Register error:', error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
.register-container {
  display: flex;
  min-height: 100vh;
  background-color: #f0f2f5;

  // 左侧品牌展示区
  .register-left {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 60px;

    .brand-content {
      max-width: 600px;
      color: #fff;
      text-align: center;

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

      .stats {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 40px;

        .stat-item {
          h2 {
            margin: 0 0 8px;
            font-size: 36px;
            font-weight: bold;
          }

          p {
            margin: 0;
            font-size: 16px;
            opacity: 0.8;
          }
        }
      }
    }
  }

  // 右侧注册表单区
  .register-right {
    width: 520px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #fff;
    padding: 60px;

    .register-box {
      width: 100%;

      .register-title {
        margin: 0 0 12px;
        font-size: 28px;
        font-weight: bold;
        color: #333;
      }

      .register-subtitle {
        margin: 0 0 40px;
        font-size: 14px;
        color: #666;
      }

      .register-form {
        .code-input-wrapper {
          display: flex;
          gap: 12px;
          width: 100%;

          .code-button {
            flex-shrink: 0;
            width: 120px;
          }
        }

        .terms-link {
          color: #1890ff;
          text-decoration: none;

          &:hover {
            text-decoration: underline;
          }
        }

        .register-button {
          width: 100%;
          margin-top: 8px;
        }

        .login-tip {
          margin-top: 24px;
          text-align: center;
          font-size: 14px;
          color: #666;

          .login-link {
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
  .register-container {
    .register-left {
      display: none;
    }

    .register-right {
      flex: 1;
      width: 100%;
    }
  }
}
</style>
