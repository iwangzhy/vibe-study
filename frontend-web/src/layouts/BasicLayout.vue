<template>
  <div class="basic-layout">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="appStore.sidebarOpened ? '250px' : '64px'" class="layout-aside">
        <div class="sidebar-header">
          <h1 v-if="appStore.sidebarOpened" class="logo">Vibe Social</h1>
          <h1 v-else class="logo-mini">V</h1>
        </div>

        <el-menu
          :default-active="activeMenu"
          :collapse="!appStore.sidebarOpened"
          :unique-opened="true"
          router
          class="sidebar-menu"
        >
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <template #title>首页</template>
          </el-menu-item>

          <el-menu-item index="/explore">
            <el-icon><Compass /></el-icon>
            <template #title>探索</template>
          </el-menu-item>

          <el-menu-item index="/notifications">
            <el-icon><Bell /></el-icon>
            <template #title>
              通知
              <el-badge v-if="notificationCount > 0" :value="notificationCount" class="badge-item" />
            </template>
          </el-menu-item>

          <el-menu-item index="/messages">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>消息</template>
          </el-menu-item>

          <el-menu-item :index="`/profile/${userStore.userId}`">
            <el-icon><User /></el-icon>
            <template #title>个人主页</template>
          </el-menu-item>

          <el-menu-item index="/settings">
            <el-icon><Setting /></el-icon>
            <template #title>设置</template>
          </el-menu-item>
        </el-menu>

        <div class="sidebar-footer">
          <el-button
            v-if="appStore.sidebarOpened"
            type="primary"
            size="large"
            class="post-button"
            @click="showPostDialog = true"
          >
            <el-icon><EditPen /></el-icon>
            <span>发布动态</span>
          </el-button>
          <el-button
            v-else
            type="primary"
            circle
            class="post-button-mini"
            @click="showPostDialog = true"
          >
            <el-icon><EditPen /></el-icon>
          </el-button>
        </div>
      </el-aside>

      <!-- 主内容区 -->
      <el-container>
        <!-- 顶部导航栏 -->
        <el-header class="layout-header" height="60px">
          <div class="header-left">
            <el-button
              text
              circle
              @click="appStore.toggleSidebar"
            >
              <el-icon><Fold v-if="appStore.sidebarOpened" /><Expand v-else /></el-icon>
            </el-button>

            <el-input
              v-model="searchText"
              placeholder="搜索用户、话题或内容"
              prefix-icon="Search"
              class="search-input"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>
          </div>

          <div class="header-right">
            <!-- 通知图标 -->
            <el-badge :value="notificationCount" :hidden="notificationCount === 0" class="notification-badge">
              <el-button text circle>
                <el-icon :size="20"><Bell /></el-icon>
              </el-button>
            </el-badge>

            <!-- 消息图标 -->
            <el-button text circle>
              <el-icon :size="20"><ChatDotRound /></el-icon>
            </el-button>

            <!-- 用户菜单 -->
            <el-dropdown @command="handleCommand">
              <div class="user-info">
                <el-avatar :src="userStore.avatar" :size="36">
                  {{ userStore.nickname?.charAt(0) }}
                </el-avatar>
                <span class="username">{{ userStore.nickname }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    <span>个人主页</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="settings">
                    <el-icon><Setting /></el-icon>
                    <span>设置</span>
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    <span>退出登录</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 主内容 -->
        <el-main class="layout-main">
          <div class="main-content">
            <router-view />
          </div>
        </el-main>
      </el-container>
    </el-container>

    <!-- 发布动态对话框 -->
    <el-dialog
      v-model="showPostDialog"
      title="发布动态"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-input
        v-model="postContent"
        type="textarea"
        :rows="6"
        placeholder="分享你的想法..."
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="showPostDialog = false">取消</el-button>
        <el-button type="primary" @click="handlePost">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  HomeFilled,
  Compass,
  Bell,
  ChatDotRound,
  User,
  Setting,
  EditPen,
  Search,
  Fold,
  Expand,
  ArrowDown,
  SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const searchText = ref('')
const notificationCount = ref(5) // 示例数据
const showPostDialog = ref(false)
const postContent = ref('')

// 当前激活的菜单
const activeMenu = computed(() => {
  return route.path
})

// 搜索
const handleSearch = () => {
  if (searchText.value.trim()) {
    router.push(`/search?q=${encodeURIComponent(searchText.value)}`)
  }
}

// 发布动态
const handlePost = () => {
  if (!postContent.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  // TODO: 调用发布接口
  ElMessage.success('发布成功')
  showPostDialog.value = false
  postContent.value = ''
}

// 处理下拉菜单命令
const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push(`/profile/${userStore.userId}`)
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      handleLogout()
      break
  }
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
    ElMessage.success('退出成功')
  }).catch(() => {})
}
</script>

<style scoped lang="scss">
.basic-layout {
  height: 100vh;
  overflow: hidden;

  .el-container {
    height: 100%;
  }

  // 侧边栏
  .layout-aside {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background-color: #fff;
    border-right: 1px solid #e8e8e8;
    transition: width 0.3s;

    .sidebar-header {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 60px;
      border-bottom: 1px solid #e8e8e8;

      .logo {
        margin: 0;
        font-size: 24px;
        font-weight: bold;
        color: #1890ff;
        cursor: pointer;
      }

      .logo-mini {
        margin: 0;
        font-size: 28px;
        font-weight: bold;
        color: #1890ff;
        cursor: pointer;
      }
    }

    .sidebar-menu {
      flex: 1;
      border-right: none;
      overflow-y: auto;

      .el-menu-item {
        height: 56px;
        line-height: 56px;
        font-size: 16px;

        .el-icon {
          font-size: 20px;
        }
      }

      .badge-item {
        margin-left: 8px;
      }
    }

    .sidebar-footer {
      padding: 16px;
      border-top: 1px solid #e8e8e8;

      .post-button {
        width: 100%;
        font-size: 16px;
        font-weight: bold;

        .el-icon {
          margin-right: 8px;
        }
      }

      .post-button-mini {
        width: 100%;
      }
    }
  }

  // 顶部栏
  .layout-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    background-color: #fff;
    border-bottom: 1px solid #e8e8e8;

    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;

      .search-input {
        width: 400px;
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 16px;

      .notification-badge {
        :deep(.el-badge__content) {
          top: 8px;
          right: 12px;
        }
      }

      .user-info {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 4px 12px;
        border-radius: 20px;
        cursor: pointer;
        transition: background-color 0.3s;

        &:hover {
          background-color: #f5f5f5;
        }

        .username {
          font-size: 14px;
          font-weight: 500;
        }
      }
    }
  }

  // 主内容区
  .layout-main {
    background-color: #f0f2f5;
    overflow-y: auto;
    padding: 0;

    .main-content {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px;
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .layout-aside {
    width: 64px !important;
  }

  .header-left .search-input {
    width: 200px;
  }
}
</style>
