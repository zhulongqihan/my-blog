<template>
  <div class="rate-limit-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card blocked-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">今日拦截</div>
              <div class="stat-value">{{ stats.todayBlocked }}</div>
            </div>
            <el-icon :size="40" class="stat-icon"><Warning /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card blacklist-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">黑名单IP</div>
              <div class="stat-value">{{ stats.blacklistCount }}</div>
            </div>
            <el-icon :size="40" class="stat-icon"><Lock /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card whitelist-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">白名单IP</div>
              <div class="stat-value">{{ stats.whitelistCount }}</div>
            </div>
            <el-icon :size="40" class="stat-icon"><Unlock /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card api-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">受保护接口</div>
              <div class="stat-value">{{ stats.apiStats.length }}</div>
            </div>
            <el-icon :size="40" class="stat-icon"><Shield /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表 + 接口限流排名 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>近7日限流趋势</span>
              <el-button type="primary" link @click="refreshStats">
                <el-icon><Refresh /></el-icon> 刷新
              </el-button>
            </div>
          </template>
          <div ref="chartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>
            <span>今日接口限流排名</span>
          </template>
          <div v-if="stats.apiStats.length === 0" class="empty-tip">
            暂无限流记录
          </div>
          <div v-else class="api-rank-list">
            <div v-for="(item, index) in stats.apiStats" :key="index" class="api-rank-item">
              <div class="rank-left">
                <el-tag :type="index < 3 ? 'danger' : 'info'" size="small" class="rank-badge">
                  {{ index + 1 }}
                </el-tag>
                <span class="api-name" :title="item.api">{{ formatApiName(item.api) }}</span>
              </div>
              <div class="rank-right">
                <span class="rank-count">{{ item.count }}</span>
                <span class="rank-unit">次</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- IP黑白名单管理 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span><el-icon><Lock /></el-icon> IP黑名单</span>
              <el-button type="danger" size="small" @click="showAddBlacklistDialog">
                <el-icon><Plus /></el-icon> 添加
              </el-button>
            </div>
          </template>
          <el-table :data="blacklist" max-height="300" size="small">
            <el-table-column prop="ip" label="IP地址" min-width="140" />
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row }">
                <el-popconfirm
                  title="确定移除该IP？"
                  @confirm="handleRemoveBlacklist(row.ip)"
                >
                  <template #reference>
                    <el-button type="danger" link size="small">移除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="blacklist.length === 0" class="empty-tip">暂无黑名单IP</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span><el-icon><Unlock /></el-icon> IP白名单</span>
              <el-button type="success" size="small" @click="showAddWhitelistDialog">
                <el-icon><Plus /></el-icon> 添加
              </el-button>
            </div>
          </template>
          <el-table :data="whitelist" max-height="300" size="small">
            <el-table-column prop="ip" label="IP地址" min-width="140" />
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row }">
                <el-popconfirm
                  title="确定移除该IP？"
                  @confirm="handleRemoveWhitelist(row.ip)"
                >
                  <template #reference>
                    <el-button type="warning" link size="small">移除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="whitelist.length === 0" class="empty-tip">暂无白名单IP</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近限流事件 -->
    <el-card shadow="hover" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>最近限流事件</span>
          <el-button type="primary" link @click="refreshEvents">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>
      <el-table :data="events" max-height="400" size="small" stripe>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.timestamp) }}
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" width="150" />
        <el-table-column prop="key" label="限流Key" min-width="300" show-overflow-tooltip />
      </el-table>
      <div v-if="events.length === 0" class="empty-tip">暂无限流事件</div>
    </el-card>

    <!-- 添加黑名单对话框 -->
    <el-dialog v-model="blacklistDialogVisible" title="添加IP到黑名单" width="480px">
      <el-form :model="blacklistForm" label-width="100px">
        <el-form-item label="IP地址" required>
          <el-input v-model="blacklistForm.ip" placeholder="例如：192.168.1.100" />
        </el-form-item>
        <el-form-item label="封禁原因">
          <el-input v-model="blacklistForm.reason" placeholder="可选，填写封禁原因" />
        </el-form-item>
        <el-form-item label="封禁时长">
          <el-select v-model="blacklistForm.duration" placeholder="永久封禁" clearable style="width: 100%">
            <el-option label="10分钟" value="10" />
            <el-option label="30分钟" value="30" />
            <el-option label="1小时" value="60" />
            <el-option label="6小时" value="360" />
            <el-option label="24小时" value="1440" />
            <el-option label="7天" value="10080" />
            <el-option label="永久" value="" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="blacklistDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleAddBlacklist" :loading="submitting">确定封禁</el-button>
      </template>
    </el-dialog>

    <!-- 添加白名单对话框 -->
    <el-dialog v-model="whitelistDialogVisible" title="添加IP到白名单" width="400px">
      <el-form :model="whitelistForm" label-width="80px">
        <el-form-item label="IP地址" required>
          <el-input v-model="whitelistForm.ip" placeholder="例如：127.0.0.1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="whitelistDialogVisible = false">取消</el-button>
        <el-button type="success" @click="handleAddWhitelist" :loading="submitting">确定添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  getRateLimitStats,
  getRecentEvents,
  getBlacklist,
  addToBlacklist,
  removeFromBlacklist,
  getWhitelist,
  addToWhitelist,
  removeFromWhitelist,
} from '@/api/rateLimit'
import type { RateLimitStats, RateLimitEvent } from '@/api/rateLimit'

// ==================== 统计数据 ====================
const stats = reactive<RateLimitStats>({
  todayBlocked: 0,
  dailyStats: [],
  blacklistCount: 0,
  whitelistCount: 0,
  apiStats: [],
})

// ==================== 图表 ====================
const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

function initChart() {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

function updateChart() {
  if (!chartInstance) return
  const dates = stats.dailyStats.map(s => s.date.substring(5)) // MM-DD
  const counts = stats.dailyStats.map(s => s.count)

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>拦截次数: <b>{c}</b>',
    },
    grid: { top: 30, right: 20, bottom: 30, left: 50 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { color: '#666' },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#666' },
    },
    series: [
      {
        name: '拦截次数',
        type: 'bar',
        data: counts,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#ff6b6b' },
            { offset: 1, color: '#ee5a24' },
          ]),
          borderRadius: [4, 4, 0, 0],
        },
        emphasis: {
          itemStyle: { color: '#e74c3c' },
        },
        barWidth: '40%',
      },
    ],
  })
}

// ==================== 黑白名单 ====================
const blacklist = ref<{ ip: string }[]>([])
const whitelist = ref<{ ip: string }[]>([])
const events = ref<RateLimitEvent[]>([])

const blacklistDialogVisible = ref(false)
const whitelistDialogVisible = ref(false)
const submitting = ref(false)

const blacklistForm = reactive({ ip: '', reason: '', duration: '' })
const whitelistForm = reactive({ ip: '' })

// ==================== 数据加载 ====================
async function refreshStats() {
  try {
    const res = await getRateLimitStats()
    Object.assign(stats, res.data)
    await nextTick()
    updateChart()
  } catch (e) {
    console.error('获取限流统计失败', e)
  }
}

async function refreshEvents() {
  try {
    const res = await getRecentEvents(50)
    events.value = res.data || []
  } catch (e) {
    console.error('获取限流事件失败', e)
  }
}

async function refreshBlacklist() {
  try {
    const res = await getBlacklist()
    blacklist.value = (res.data || []).map((ip: string) => ({ ip }))
  } catch (e) {
    console.error('获取黑名单失败', e)
  }
}

async function refreshWhitelist() {
  try {
    const res = await getWhitelist()
    whitelist.value = (res.data || []).map((ip: string) => ({ ip }))
  } catch (e) {
    console.error('获取白名单失败', e)
  }
}

// ==================== 黑名单操作 ====================
function showAddBlacklistDialog() {
  blacklistForm.ip = ''
  blacklistForm.reason = ''
  blacklistForm.duration = ''
  blacklistDialogVisible.value = true
}

async function handleAddBlacklist() {
  if (!blacklistForm.ip.trim()) {
    ElMessage.warning('请输入IP地址')
    return
  }
  submitting.value = true
  try {
    await addToBlacklist({
      ip: blacklistForm.ip.trim(),
      reason: blacklistForm.reason || undefined,
      duration: blacklistForm.duration || undefined,
    })
    ElMessage.success('已添加到黑名单')
    blacklistDialogVisible.value = false
    refreshBlacklist()
    refreshStats()
  } catch (e) {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleRemoveBlacklist(ip: string) {
  try {
    await removeFromBlacklist(ip)
    ElMessage.success('已从黑名单移除')
    refreshBlacklist()
    refreshStats()
  } catch (e) {
    // error handled by interceptor
  }
}

// ==================== 白名单操作 ====================
function showAddWhitelistDialog() {
  whitelistForm.ip = ''
  whitelistDialogVisible.value = true
}

async function handleAddWhitelist() {
  if (!whitelistForm.ip.trim()) {
    ElMessage.warning('请输入IP地址')
    return
  }
  submitting.value = true
  try {
    await addToWhitelist({ ip: whitelistForm.ip.trim() })
    ElMessage.success('已添加到白名单')
    whitelistDialogVisible.value = false
    refreshWhitelist()
    refreshStats()
  } catch (e) {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleRemoveWhitelist(ip: string) {
  try {
    await removeFromWhitelist(ip)
    ElMessage.success('已从白名单移除')
    refreshWhitelist()
    refreshStats()
  } catch (e) {
    // error handled by interceptor
  }
}

// ==================== 工具函数 ====================
function formatTime(timestamp: string) {
  const date = new Date(Number(timestamp))
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

function formatApiName(api: string) {
  // 从完整的 rate:limit:ip:xxx:Controller.method 格式中提取最后的方法名
  const parts = api.split(':')
  return parts[parts.length - 1] || api
}

// ==================== 生命周期 ====================
onMounted(async () => {
  await refreshStats()
  await nextTick()
  initChart()
  refreshEvents()
  refreshBlacklist()
  refreshWhitelist()
})
</script>

<style scoped>
.rate-limit-page {
  padding: 0;
}

.stat-cards .stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-icon {
  color: #c0c4cc;
  opacity: 0.6;
}

.blocked-card .stat-value { color: #f56c6c; }
.blocked-card .stat-icon { color: #f56c6c; }

.blacklist-card .stat-value { color: #e6a23c; }
.blacklist-card .stat-icon { color: #e6a23c; }

.whitelist-card .stat-value { color: #67c23a; }
.whitelist-card .stat-icon { color: #67c23a; }

.api-card .stat-value { color: #409eff; }
.api-card .stat-icon { color: #409eff; }

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.empty-tip {
  text-align: center;
  color: #909399;
  padding: 20px 0;
  font-size: 14px;
}

.api-rank-list {
  max-height: 260px;
  overflow-y: auto;
}

.api-rank-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.api-rank-item:last-child {
  border-bottom: none;
}

.rank-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.rank-badge {
  min-width: 24px;
  text-align: center;
}

.api-name {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-right {
  display: flex;
  align-items: baseline;
  gap: 2px;
  margin-left: 12px;
}

.rank-count {
  font-size: 16px;
  font-weight: 600;
  color: #f56c6c;
}

.rank-unit {
  font-size: 12px;
  color: #909399;
}
</style>
