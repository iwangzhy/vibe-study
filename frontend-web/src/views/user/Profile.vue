<template>
  <div class="profile-page">
    <el-card v-loading="loading" class="profile-card">
      <!-- User Header Section -->
      <div class="profile-header">
        <el-avatar :size="120" :src="userInfo?.avatar" class="avatar">
          {{ userInfo?.nickname?.[0] }}
        </el-avatar>
        
        <div class="user-info">
          <div class="name-section">
            <h2 class="nickname">{{ userInfo?.nickname }}</h2>
            <span class="username">@{{ userInfo?.username }}</span>
          </div>
          
          <div class="stats-section">
            <div class="stat-item">
              <span class="stat-value">{{ userInfo?.followingCount || 0 }}</span>
              <span class="stat-label">关注</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ userInfo?.followerCount || 0 }}</span>
              <span class="stat-label">粉丝</span>
            </div>
          </div>
          
          <div class="bio-section" v-if="userInfo?.bio">
            <p class="bio">{{ userInfo.bio }}</p>
          </div>
          
          <div class="details-section">
            <div class="detail-item" v-if="userInfo?.gender">
              <el-icon><User /></el-icon>
              <span>{{ getGenderText(userInfo.gender) }}</span>
            </div>
            <div class="detail-item" v-if="userInfo?.birthday">
              <el-icon><Calendar /></el-icon>
              <span>{{ formatBirthday(userInfo.birthday) }}</span>
            </div>
            <div class="detail-item" v-if="userInfo?.email">
              <el-icon><Message /></el-icon>
              <span>{{ userInfo.email }}</span>
            </div>
            <div class="detail-item" v-if="userInfo?.phone">
              <el-icon><Phone /></el-icon>
              <span>{{ userInfo.phone }}</span>
            </div>
          </div>
        </div>
        
        <!-- Action Buttons -->
        <div class="action-buttons">
          <!-- If viewing own profile -->
          <el-button v-if="isOwnProfile" type="primary" @click="goToSettings">
            编辑资料
          </el-button>
          
          <!-- If viewing other user's profile -->
          <template v-else>
            <el-button
              v-if="userInfo?.isFollowing"
              type="info"
              @click="handleUnfollow"
              :loading="followLoading"
            >
              已关注
            </el-button>
            <el-button
              v-else
              type="primary"
              @click="handleFollow"
              :loading="followLoading"
            >
              关注
            </el-button>
          </template>
        </div>
      </div>
      
      <!-- Content Tabs (placeholder for future features) -->
      <el-divider />
      
      <el-tabs v-model="activeTab" class="content-tabs">
        <el-tab-pane label="动态" name="posts">
          <el-empty description="暂无动态" />
        </el-tab-pane>
        <el-tab-pane label="关注" name="following">
          <el-empty description="暂无关注" />
        </el-tab-pane>
        <el-tab-pane label="粉丝" name="followers">
          <el-empty description="暂无粉丝" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Calendar, Message, Phone } from '@element-plus/icons-vue'
import { getUserInfo, getCurrentUserInfo, followUser, unfollowUser } from '@/api/user'
import { useUserStore } from '@/store/user'
import type { UserInfoVO } from '@/types/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const followLoading = ref(false)
const userInfo = ref<UserInfoVO | null>(null)
const activeTab = ref('posts')

// Check if viewing own profile
const isOwnProfile = computed(() => {
  const routeId = route.params.id
  // If no ID in route, it's current user's profile
  if (!routeId) return true
  // Compare with stored user ID
  return routeId === String(userStore.userId)
})

// Fetch user info on mount
onMounted(() => {
  fetchUserInfo()
})

// Watch route changes (when navigating between different profiles)
watch(() => route.params.id, () => {
  fetchUserInfo()
})

const fetchUserInfo = async () => {
  loading.value = true
  try {
    let res
    if (isOwnProfile.value) {
      // Fetch current user's info
      res = await getCurrentUserInfo()
    } else {
      // Fetch other user's info
      const userId = Number(route.params.id)
      res = await getUserInfo(userId)
    }
    userInfo.value = res.data
  } catch (error) {
    ElMessage.error('获取用户信息失败')
    console.error('Failed to fetch user info:', error)
  } finally {
    loading.value = false
  }
}

const handleFollow = async () => {
  if (!userInfo.value) return
  
  followLoading.value = true
  try {
    await followUser(userInfo.value.id)
    ElMessage.success('关注成功')
    // Update local state
    userInfo.value.isFollowing = true
    userInfo.value.followerCount++
  } catch (error) {
    ElMessage.error('关注失败')
    console.error('Failed to follow user:', error)
  } finally {
    followLoading.value = false
  }
}

const handleUnfollow = async () => {
  if (!userInfo.value) return
  
  followLoading.value = true
  try {
    await unfollowUser(userInfo.value.id)
    ElMessage.success('取消关注成功')
    // Update local state
    userInfo.value.isFollowing = false
    userInfo.value.followerCount--
  } catch (error) {
    ElMessage.error('取消关注失败')
    console.error('Failed to unfollow user:', error)
  } finally {
    followLoading.value = false
  }
}

const goToSettings = () => {
  router.push('/user/settings')
}

const getGenderText = (gender: number): string => {
  switch (gender) {
    case 1:
      return '男'
    case 2:
      return '女'
    default:
      return '保密'
  }
}

const formatBirthday = (birthday: string): string => {
  return new Date(birthday).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}
</script>

<style scoped lang="scss">
.profile-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.profile-card {
  .profile-header {
    display: flex;
    gap: 32px;
    position: relative;
    
    .avatar {
      flex-shrink: 0;
    }
    
    .user-info {
      flex: 1;
      
      .name-section {
        display: flex;
        align-items: baseline;
        gap: 12px;
        margin-bottom: 16px;
        
        .nickname {
          margin: 0;
          font-size: 28px;
          font-weight: 600;
          color: #303133;
        }
        
        .username {
          font-size: 16px;
          color: #909399;
        }
      }
      
      .stats-section {
        display: flex;
        gap: 32px;
        margin-bottom: 16px;
        
        .stat-item {
          display: flex;
          flex-direction: column;
          align-items: center;
          
          .stat-value {
            font-size: 24px;
            font-weight: 600;
            color: #303133;
          }
          
          .stat-label {
            font-size: 14px;
            color: #909399;
            margin-top: 4px;
          }
        }
      }
      
      .bio-section {
        margin-bottom: 16px;
        
        .bio {
          font-size: 15px;
          color: #606266;
          line-height: 1.6;
          margin: 0;
        }
      }
      
      .details-section {
        display: flex;
        flex-wrap: wrap;
        gap: 16px;
        
        .detail-item {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 14px;
          color: #606266;
          
          .el-icon {
            color: #909399;
          }
        }
      }
    }
    
    .action-buttons {
      position: absolute;
      top: 0;
      right: 0;
      
      .el-button {
        min-width: 100px;
      }
    }
  }
  
  .content-tabs {
    margin-top: 24px;
    
    :deep(.el-tabs__content) {
      padding: 24px 0;
    }
  }
}

@media (max-width: 768px) {
  .profile-page {
    padding: 16px;
  }
  
  .profile-card .profile-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
    
    .action-buttons {
      position: static;
      margin-top: 16px;
    }
    
    .user-info {
      .name-section {
        flex-direction: column;
        gap: 4px;
      }
      
      .stats-section {
        justify-content: center;
      }
      
      .details-section {
        justify-content: center;
      }
    }
  }
}
</style>
