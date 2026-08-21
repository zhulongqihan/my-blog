<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo-container">
        <el-icon :size="28" color="#409eff"><Monitor /></el-icon>
        <span v-show="!isCollapse" class="logo-text">博客后台</span>
      </div>
      <el-menu
        aria-label="后台主导航"
        :default-active="activeMenu"
        router
        :collapse="isCollapse"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        class="aside-menu"
      >
        <el-menu-item-group v-for="group in menuGroups" :key="group.key">
          <template #title>{{ group.title }}</template>
          <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </el-aside>

    <!-- 右侧区域 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="layout-header">
        <a class="skip-link" href="#admin-main">跳到主要内容</a>
        <div class="header-left">
          <el-button
            class="collapse-btn"
            text
            :aria-label="isCollapse ? '展开侧边栏' : '收起侧边栏'"
            :aria-expanded="!isCollapse"
            @click="isCollapse = !isCollapse"
          >
            <el-icon :size="20"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute.meta.title">
              {{ currentRoute.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-badge :value="notificationStore.unreadCount" :hidden="!notificationStore.hasUnread" :max="99" class="notification-badge">
            <el-button :icon="Bell" circle size="small" aria-label="打开通知中心" @click="router.push('/notifications')" />
          </el-badge>
          <el-tag v-if="notificationStore.connected" type="success" effect="plain" size="small" class="online-tag">
            {{ notificationStore.onlineCount }} 在线
          </el-tag>
          <el-dropdown @command="handleCommand">
            <button class="user-info" type="button" aria-label="打开用户菜单">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ userStore.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home">
                  <el-icon><HomeFilled /></el-icon>返回博客
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main id="admin-main" class="layout-main" tabindex="-1">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const notificationStore = useNotificationStore()

const isCollapse = ref(window.innerWidth < 900)

const currentRoute = computed(() => route)
const activeMenu = computed(() => (route.meta.activeMenu as string) || route.path)

const menuGroups = [
  {
    key: 'content',
    title: '内容管理',
    items: [
      { path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
      { path: '/articles', title: '文章管理', icon: 'Document' },
      { path: '/categories', title: '分类管理', icon: 'Folder' },
      { path: '/tags', title: '标签管理', icon: 'PriceTag' },
    ],
  },
  {
    key: 'interaction',
    title: '互动管理',
    items: [
      { path: '/comments', title: '评论管理', icon: 'ChatDotRound' },
      { path: '/notifications', title: '通知中心', icon: 'Bell' },
    ],
  },
  {
    key: 'operations',
    title: '系统运维',
    items: [
      { path: '/logs', title: '操作日志', icon: 'Tickets' },
      { path: '/rate-limit', title: '限流监控', icon: 'Odometer' },
      { path: '/cache', title: '缓存监控', icon: 'Coin' },
      { path: '/mq', title: 'MQ 监控', icon: 'Connection' },
    ],
  },
  {
    key: 'ai',
    title: 'AI 能力',
    items: [{ path: '/ai-observability', title: 'AI 可观测性', icon: 'DataAnalysis' }],
  },
]

const resizeMenu = () => {
  if (window.innerWidth < 900) isCollapse.value = true
}

function handleCommand(command: string) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      notificationStore.disconnect()
      userStore.logout()
      router.push('/login')
    })
  } else if (command === 'home') {
    window.open('/', '_blank')
  }
}

// WebSocket 连接生命周期
onMounted(() => {
  notificationStore.connect()
  notificationStore.fetchUnreadCount()
  window.addEventListener('resize', resizeMenu)
})

onUnmounted(() => {
  notificationStore.disconnect()
  window.removeEventListener('resize', resizeMenu)
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
}

.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}

.aside-menu {
  border-right: none;
}

.aside-menu:not(.el-menu--collapse) {
  width: 220px;
}

.layout-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e6e6;
  background-color: #fff;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  color: #333;
  min-width: 40px;
  min-height: 40px;
  cursor: pointer;
}

.collapse-btn:hover {
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notification-badge { margin-right: 0; }
.online-tag { font-size: 12px; }

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #333;
  border: 0;
  background: transparent;
  padding: 4px;
  border-radius: 6px;
}

.user-info:focus-visible,
.collapse-btn:focus-visible,
.skip-link:focus-visible {
  outline: 2px solid #409eff;
  outline-offset: 3px;
}

.skip-link {
  position: absolute;
  left: 12px;
  top: -48px;
  z-index: 10;
  padding: 8px 12px;
  border-radius: 6px;
  background: #304156;
  color: #fff;
  text-decoration: none;
  transition: top 0.2s ease;
}

.skip-link:focus {
  top: 8px;
}

.username {
  font-size: 14px;
}

.layout-main {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}

@media (max-width: 900px) {
  .layout-header {
    padding: 0 12px;
  }

  .layout-main {
    padding: 12px;
  }

  .username,
  .online-tag {
    display: none;
  }
}
</style>
