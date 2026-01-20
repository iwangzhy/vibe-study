<template>
  <div class="settings-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>账号设置</h2>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" class="settings-tabs">
        <!-- Profile Settings Tab -->
        <el-tab-pane label="个人资料" name="profile">
          <el-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            label-width="100px"
            class="settings-form"
            v-loading="profileLoading"
          >
            <el-form-item label="头像">
              <div class="avatar-upload">
                <el-avatar :size="80" :src="profileForm.avatar">
                  {{ profileForm.nickname?.[0] }}
                </el-avatar>
                <el-button size="small" style="margin-left: 16px">
                  上传头像
                </el-button>
                <div class="avatar-tip">建议尺寸: 200x200 像素</div>
              </div>
            </el-form-item>
            
            <el-form-item label="昵称" prop="nickname">
              <el-input
                v-model="profileForm.nickname"
                placeholder="请输入昵称"
                maxlength="20"
                show-word-limit
              />
            </el-form-item>
            
            <el-form-item label="用户名">
              <el-input
                v-model="profileForm.username"
                disabled
                placeholder="用户名不可修改"
              />
            </el-form-item>
            
            <el-form-item label="个人简介" prop="bio">
              <el-input
                v-model="profileForm.bio"
                type="textarea"
                :rows="4"
                placeholder="介绍一下自己吧"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
            
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="profileForm.gender">
                <el-radio :label="0">保密</el-radio>
                <el-radio :label="1">男</el-radio>
                <el-radio :label="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item label="生日" prop="birthday">
              <el-date-picker
                v-model="profileForm.birthday"
                type="date"
                placeholder="选择生日"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledDate"
              />
            </el-form-item>
            
            <el-form-item label="邮箱">
              <el-input
                v-model="profileForm.email"
                disabled
                placeholder="邮箱不可修改"
              >
                <template #append>
                  <el-button>修改邮箱</el-button>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item label="手机号">
              <el-input
                v-model="profileForm.phone"
                disabled
                placeholder="未绑定手机号"
              >
                <template #append>
                  <el-button>绑定手机</el-button>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item>
              <el-button
                type="primary"
                @click="handleUpdateProfile"
                :loading="profileLoading"
              >
                保存修改
              </el-button>
              <el-button @click="resetProfileForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- Change Password Tab -->
        <el-tab-pane label="修改密码" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="120px"
            class="settings-form"
          >
            <el-alert
              title="密码要求"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom: 20px"
            >
              <template #default>
                <ul style="margin: 0; padding-left: 20px">
                  <li>长度为 6-20 个字符</li>
                  <li>必须包含字母和数字</li>
                  <li>新密码不能与旧密码相同</li>
                </ul>
              </template>
            </el-alert>
            
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入当前密码"
                show-password
              />
            </el-form-item>
            
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码"
                show-password
              />
            </el-form-item>
            
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            
            <el-form-item>
              <el-button
                type="primary"
                @click="handleChangePassword"
                :loading="passwordLoading"
              >
                修改密码
              </el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- Account Settings Tab -->
        <el-tab-pane label="账号安全" name="account">
          <div class="account-section">
            <el-alert
              title="账号安全"
              type="warning"
              :closable="false"
              show-icon
            >
              <template #default>
                <p>请妥善保管您的账号信息，不要向他人透露密码</p>
              </template>
            </el-alert>
            
            <div class="account-item">
              <div class="item-info">
                <div class="item-title">账号状态</div>
                <div class="item-desc">正常</div>
              </div>
              <el-tag type="success">已激活</el-tag>
            </div>
            
            <div class="account-item">
              <div class="item-info">
                <div class="item-title">注册时间</div>
                <div class="item-desc">{{ formatDate(profileForm.createdAt) }}</div>
              </div>
            </div>
            
            <div class="account-item">
              <div class="item-info">
                <div class="item-title">最后更新</div>
                <div class="item-desc">{{ formatDate(profileForm.updatedAt) }}</div>
              </div>
            </div>
            
            <el-divider />
            
            <div class="danger-zone">
              <h3>危险操作</h3>
              <el-button type="danger" plain disabled>
                注销账号
              </el-button>
              <p class="danger-tip">注销后账号将无法恢复，该功能暂未开放</p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getCurrentUserInfo, updateUserInfo, changePassword } from '@/api/user'
import type { UserInfoVO, UpdateUserRequest, ChangePasswordRequest } from '@/types/user'

const activeTab = ref('profile')
const profileLoading = ref(false)
const passwordLoading = ref(false)

// Profile form
const profileFormRef = ref<FormInstance>()
const profileForm = ref<Partial<UserInfoVO>>({
  username: '',
  nickname: '',
  avatar: '',
  bio: '',
  gender: 0,
  birthday: '',
  email: '',
  phone: '',
  createdAt: '',
  updatedAt: ''
})

const profileRules: FormRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为 2-20 个字符', trigger: 'blur' }
  ],
  bio: [
    { max: 200, message: '个人简介不能超过 200 个字符', trigger: 'blur' }
  ]
}

// Password form
const passwordFormRef = ref<FormInstance>()
const passwordForm = ref<ChangePasswordRequest>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePassword = (_rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请输入新密码'))
  } else if (value.length < 6 || value.length > 20) {
    callback(new Error('密码长度为 6-20 个字符'))
  }/* else if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]+$/.test(value)) {
    callback(new Error('密码必须包含字母和数字'))
  }*/ else {
    callback()
  }
}

const validateConfirmPassword = (_rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.value.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// Fetch user info on mount
onMounted(() => {
  fetchUserInfo()
})

const fetchUserInfo = async () => {
  profileLoading.value = true
  try {
    const res = await getCurrentUserInfo()
    profileForm.value = { ...res.data }
  } catch (error) {
    ElMessage.error('获取用户信息失败')
    console.error('Failed to fetch user info:', error)
  } finally {
    profileLoading.value = false
  }
}

const handleUpdateProfile = async () => {
  if (!profileFormRef.value) return
  
  await profileFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    profileLoading.value = true
    try {
      const updateData: UpdateUserRequest = {
        nickname: profileForm.value.nickname!,
        avatar: profileForm.value.avatar,
        bio: profileForm.value.bio,
        gender: profileForm.value.gender,
        birthday: profileForm.value.birthday
      }
      
      await updateUserInfo(updateData)
      ElMessage.success('保存成功')
      
      // Refresh user info
      await fetchUserInfo()
    } catch (error) {
      ElMessage.error('保存失败')
      console.error('Failed to update user info:', error)
    } finally {
      profileLoading.value = false
    }
  })
}

const resetProfileForm = () => {
  fetchUserInfo()
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    passwordLoading.value = true
    try {
      await changePassword(passwordForm.value)
      ElMessage.success('密码修改成功，请重新登录')
      
      // Reset form
      resetPasswordForm()
      
      // Optional: logout and redirect to login page
      // You can uncomment these lines if you want to force re-login
      // import { useUserStore } from '@/store/user'
      // const userStore = useUserStore()
      // userStore.logout()
      // router.push('/auth/login')
    } catch (error) {
      ElMessage.error('密码修改失败')
      console.error('Failed to change password:', error)
    } finally {
      passwordLoading.value = false
    }
  })
}

const resetPasswordForm = () => {
  passwordFormRef.value?.resetFields()
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
}

const disabledDate = (date: Date) => {
  // Disable future dates
  return date > new Date()
}

const formatDate = (dateStr: string | undefined) => {
  if (!dateStr) return '未知'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped lang="scss">
.settings-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
  
  .card-header {
    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: #303133;
    }
  }
  
  .settings-tabs {
    :deep(.el-tabs__content) {
      padding: 24px 0;
    }
  }
  
  .settings-form {
    max-width: 600px;
    
    .avatar-upload {
      display: flex;
      align-items: center;
      
      .avatar-tip {
        font-size: 12px;
        color: #909399;
        margin-left: 12px;
      }
    }
  }
  
  .account-section {
    max-width: 600px;
    
    .account-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 0;
      border-bottom: 1px solid #ebeef5;
      
      &:last-child {
        border-bottom: none;
      }
      
      .item-info {
        .item-title {
          font-size: 15px;
          font-weight: 500;
          color: #303133;
          margin-bottom: 8px;
        }
        
        .item-desc {
          font-size: 14px;
          color: #909399;
        }
      }
    }
    
    .danger-zone {
      margin-top: 32px;
      
      h3 {
        font-size: 16px;
        font-weight: 600;
        color: #f56c6c;
        margin-bottom: 16px;
      }
      
      .danger-tip {
        font-size: 13px;
        color: #909399;
        margin-top: 8px;
      }
    }
  }
}

@media (max-width: 768px) {
  .settings-page {
    padding: 16px;
    
    .settings-form,
    .account-section {
      max-width: 100%;
    }
  }
}
</style>
