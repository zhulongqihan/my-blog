<template>
  <div class="notification-center">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <el-icon :size="32" color="#409eff"><Bell /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ totalNotifications }}</div>
              <div class="stat-label">总通知数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <el-icon :size="32" color="#e6a23c"><ChatDotRound /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ unreadCount }}</div>
              <div class="stat-label">未读通知</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <el-icon :size="32" :color="wsConnected ? '#67c23a' : '#f56c6c'">
              <Connection />
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ wsConnected ? '已连接' : '未连接' }}</div>
              <div class="stat-label">WebSocket</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <el-icon :size="32" color="#909399"><User /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ onlineCount }}</div>
              <div class="stat-label">在线人数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作区 -->
    <el-card class="action-card">
      <div class="action-bar">
        <div class="action-left">
          <el-button type="primary" @click="showBroadcastDialog = true">
            <el-icon><Promotion /></el-icon> 发送公告
          </el-button>
          <el-button @click="handleMarkAllRead" :disabled="unreadCount === 0">
            <el-icon><Check /></el-icon> 全部已读
          </el-button>
          <el-button @click="fetchNotifications">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
        <div class="action-right">
          <el-tag :type="wsConnected ? 'success' : 'danger'" effect="dark" size="small">
            <span class="ws-dot" :class="{ active: wsConnected }"></span>
            WebSocket {{ wsConnected ? '已连接' : '未连接' }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- 通知列表 -->
    <el-card class="list-card">
      <el-table :data="notifications" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.type] || 'info'" size="small">
              {{ typeTextMap[row.type] || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column prop="senderName" label="发送者" width="120" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isRead ? 'info' : 'warning'" size="small">
              {{ row.isRead ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="!row.isRead" 
              type="primary" 
              link 
              size="small"
              @click="handleMarkRead(row)"
            >
              标记已读
            </el-button>
            <el-button 
              v-if="row.relatedId && row.relatedType === 'article'" 
              type="success" 
              link 
              size="small"
              @click="goToArticle(row.relatedId)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 发送公告对话框 -->
    <el-dialog v-model="showBroadcastDialog" title="发送系统公告" width="500">
      <el-form :model="broadcastForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="broadcastForm.title" placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input 
            v-model="broadcastForm.content" 
            type="textarea" 
            :rows="4" 
            placeholder="公告内容（将实时推送给所有在线用户）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBroadcastDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSendBroadcast" :loading="sending">
          发送
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useNotificationStore } from '@/stores/notification'
import { 
  getNotifications, markAsRead, markAllAsRead, sendBroadcast,
  type NotificationItem 
} from '@/api/notification'

const router = useRouter()
const notificationStore = useNotificationStore()

const loading = ref(false)
const sending = ref(false)
const notifications = ref<NotificationItem[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalNotifications = ref(0)
const showBroadcastDialog = ref(false)
const broadcastForm = ref({ title: '系统公告', content: '' })

const wsConnected = computed(() => notificationStore.connected)
const onlineCount = computed(() => notificationStore.onlineCount)
const unreadCount = computed(() => notificationStore.unreadCount)

const typeTagMap: Record<string, string> = {
  COMMENT: 'warning',
  SYSTEM: 'primary',
  LIKE: 'success',
}

const typeTextMap: Record<string, string> = {
  COMMENT: '评论',
  SYSTEM: '公告',
  LIKE: '点赞',
}

function formatTime(time: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

async function fetchNotifications() {
  loading.value = true
  try {
    const res = await getNotifications(currentPage.value - 1, pageSize.value)
    const data = res.data as any
    notifications.value = data.content
    total.value = data.totalElements
    totalNotifications.value = data.totalElements
  } catch (e) {
    ElMessage.error('获取通知列表失败')
  } finally {
    loading.value = false
  }
}

async function handleMarkRead(row: NotificationItem) {
  try {
    await markAsRead(row.id)
    row.isRead = true
    notificationStore.fetchUnreadCount()
    ElMessage.success('已标记为已读')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function handleMarkAllRead() {
  try {
    await markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
    notificationStore.resetUnread()
    ElMessage.success('已全部标记为已读')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function handleSendBroadcast() {
  if (!broadcastForm.value.content.trim()) {
    ElMessage.warning('内容不能为空')
    return
  }
  sending.value = true
  try {
    await sendBroadcast(broadcastForm.value.title, broadcastForm.value.content)
    ElMessage.success('公告发送成功，已推送给所有在线用户')
    showBroadcastDialog.value = false
    broadcastForm.value = { title: '系统公告', content: '' }
    fetchNotifications()
  } catch (e) {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

function goToArticle(id: number) {
  window.open(`/article/${id}`, '_blank')
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchNotifications()
}

onMounted(() => {
  fetchNotifications()
  notificationStore.fetchUnreadCount()
})
</script>

<style scoped>
.notification-center { display: flex; flex-direction: column; gap: 20px; }

.stats-row { margin-bottom: 0; }
.stat-card { cursor: default; }
.stat-item { display: flex; align-items: center; gap: 16px; }
.stat-info { flex: 1; }
.stat-value { font-size: 24px; font-weight: 700; color: #303133; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }

.action-bar { display: flex; justify-content: space-between; align-items: center; }
.action-left { display: flex; gap: 10px; }

.ws-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f56c6c;
  margin-right: 4px;
  vertical-align: middle;
}
.ws-dot.active {
  background: #67c23a;
  animation: ws-pulse 2s ease-in-out infinite;
}
@keyframes ws-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.pagination-container { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
