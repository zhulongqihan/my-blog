<template>
  <div class="mq-monitor">
    <!-- 连接状态卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" :style="{ background: connected ? 'linear-gradient(135deg, #43e97b, #38f9d7)' : 'linear-gradient(135deg, #f5576c, #ff6b6b)' }">
              <el-icon :size="28"><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ connected ? 'UP' : 'DOWN' }}</div>
              <div class="stat-label">连接状态</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
              <el-icon :size="28"><Message /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ totalMessages }}</div>
              <div class="stat-label">待处理消息</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
              <el-icon :size="28"><Switch /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ exchanges.length }}</div>
              <div class="stat-label">交换机数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
              <el-icon :size="28"><Box /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ queues.length }}</div>
              <div class="stat-label">队列数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 队列详情 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>队列状态</span>
              <el-button type="primary" :icon="Refresh" size="small" @click="fetchStats" :loading="loading">
                刷新
              </el-button>
            </div>
          </template>
          <el-table :data="queues" stripe style="width: 100%">
            <el-table-column prop="description" label="队列" min-width="140" />
            <el-table-column prop="name" label="队列名" min-width="220">
              <template #default="{ row }">
                <el-text type="info" size="small">{{ row.name }}</el-text>
              </template>
            </el-table-column>
            <el-table-column prop="messageCount" label="待处理消息" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="row.messageCount > 0 ? 'warning' : 'success'" size="small">
                  {{ row.messageCount >= 0 ? row.messageCount : 'N/A' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="consumerCount" label="消费者" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.consumerCount > 0 ? 'success' : 'danger'" size="small">
                  {{ row.consumerCount >= 0 ? row.consumerCount : 'N/A' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'running' ? 'success' : 'danger'" size="small">
                  {{ row.status === 'running' ? '运行中' : row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 交换机信息 -->
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>
            <span>交换机信息</span>
          </template>
          <el-table :data="exchanges" stripe style="width: 100%">
            <el-table-column prop="description" label="交换机" min-width="130" />
            <el-table-column prop="type" label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="交换机名" min-width="180">
              <template #default="{ row }">
                <el-text type="info" size="small">{{ row.name }}</el-text>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 消息测试 -->
        <el-card shadow="hover" style="margin-top: 16px;">
          <template #header>
            <span>消息测试</span>
          </template>
          <div class="test-section">
            <p class="test-desc">发送测试消息到指定队列，验证MQ连通性</p>
            <el-space direction="vertical" :size="12" style="width: 100%;">
              <el-button 
                type="primary" 
                plain 
                style="width: 100%;"
                @click="handleTestMessage('blog.comment.notification.queue')"
                :loading="testLoading === 'comment'"
              >
                <el-icon><ChatDotRound /></el-icon>
                测试评论通知队列
              </el-button>
              <el-button 
                type="warning" 
                plain 
                style="width: 100%;"
                @click="handleTestMessage('blog.log.queue')"
                :loading="testLoading === 'log'"
              >
                <el-icon><Tickets /></el-icon>
                测试操作日志队列
              </el-button>
            </el-space>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- MQ 架构说明 -->
    <el-card shadow="hover" style="margin-top: 16px;">
      <template #header>
        <span>MQ 架构说明</span>
      </template>
      <div class="architecture-desc">
        <el-row :gutter="24">
          <el-col :span="8">
            <div class="arch-item">
              <h4>📝 评论通知流程</h4>
              <el-steps direction="vertical" :active="3" :space="36" finish-status="success">
                <el-step title="用户发表评论" description="CommentService 保存评论到DB" />
                <el-step title="发送MQ消息" description="Producer 发送到 comment.exchange" />
                <el-step title="消费者处理" description="Consumer 消费消息，发送邮件通知" />
              </el-steps>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="arch-item">
              <h4>📋 操作日志流程</h4>
              <el-steps direction="vertical" :active="3" :space="36" finish-status="success">
                <el-step title="AOP拦截请求" description="LogAspect 拦截 @Log 注解方法" />
                <el-step title="发送MQ消息" description="Producer 发送到 log.exchange" />
                <el-step title="消费者处理" description="Consumer 异步写入 DB" />
              </el-steps>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="arch-item">
              <h4>💀 死信队列流程</h4>
              <el-steps direction="vertical" :active="3" :space="36" finish-status="success">
                <el-step title="消费失败" description="Consumer NACK 且不重回队列" />
                <el-step title="路由到DLX" description="消息自动路由到死信交换机" />
                <el-step title="死信消费者" description="记录日志，人工排查" />
              </el-steps>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Refresh, Connection, Message, Switch, Box, ChatDotRound, Tickets } from '@element-plus/icons-vue'
import { getMQStats, sendTestMessage } from '@/api/mq'
import type { QueueInfo, ExchangeInfo } from '@/api/mq'

const loading = ref(false)
const testLoading = ref('')
const connected = ref(false)
const queues = ref<QueueInfo[]>([])
const exchanges = ref<ExchangeInfo[]>([])

const totalMessages = computed(() =>
  queues.value.reduce((sum, q) => sum + Math.max(q.messageCount, 0), 0)
)

async function fetchStats() {
  loading.value = true
  try {
    const res = await getMQStats()
    connected.value = res.data.connected
    queues.value = res.data.queues
    exchanges.value = res.data.exchanges
  } catch (e: any) {
    ElMessage.error('获取MQ状态失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

async function handleTestMessage(queueName: string) {
  const key = queueName.includes('comment') ? 'comment' : 'log'
  testLoading.value = key
  try {
    const res = await sendTestMessage(queueName)
    ElMessage.success(res.data || '测试消息已发送')
    // 延迟刷新，等待消息被消费
    setTimeout(() => fetchStats(), 1000)
  } catch (e: any) {
    ElMessage.error('发送失败: ' + (e.message || '未知错误'))
  } finally {
    testLoading.value = ''
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.mq-monitor {
  padding: 0;
}

.stat-cards .stat-card {
  border-radius: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-info .stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-info .stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.test-section {
  text-align: center;
}

.test-desc {
  color: #909399;
  font-size: 13px;
  margin-bottom: 16px;
}

.architecture-desc {
  padding: 8px 0;
}

.arch-item h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 15px;
}
</style>
