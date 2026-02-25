<template>
  <div class="cache-monitor">
    <!-- Redis 服务器信息 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
              <el-icon :size="28"><Coin /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ redisInfo.usedMemory || '0B' }}</div>
              <div class="stat-label">内存使用</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
              <el-icon :size="28"><Key /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ redisInfo.totalKeys || 0 }}</div>
              <div class="stat-label">总 Key 数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
              <el-icon :size="28"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ redisInfo.hitRate || '0' }}%</div>
              <div class="stat-label">缓存命中率</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b, #38f9d7)">
              <el-icon :size="28"><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ redisInfo.connectedClients || 0 }}</div>
              <div class="stat-label">连接客户端</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <!-- 缓存命中率图表 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>缓存命中率分析</span>
            </div>
          </template>
          <div ref="hitRateChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>

      <!-- 缓存空间分布图 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>缓存空间分布</span>
            </div>
          </template>
          <div ref="spaceChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 缓存空间详情 -->
    <el-card shadow="hover" style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>缓存空间管理</span>
          <el-button type="danger" size="small" @click="handleClearAll">
            <el-icon><Delete /></el-icon>清除所有缓存
          </el-button>
        </div>
      </template>
      <el-table :data="cacheSpaces" stripe style="width: 100%">
        <el-table-column prop="name" label="缓存空间" width="200">
          <template #default="{ row }">
            <el-tag :type="getTagType(row.name)" effect="plain">{{ row.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" width="200" />
        <el-table-column prop="keyCount" label="Key 数量" width="120">
          <template #default="{ row }">
            <el-badge :value="row.keyCount" :type="row.keyCount > 0 ? 'primary' : 'info'" />
          </template>
        </el-table-column>
        <el-table-column label="缓存状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.keyCount > 0" type="success" size="small">活跃</el-tag>
            <el-tag v-else type="info" size="small">空闲</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="isSpringCache(row.name)"
              type="danger"
              size="small"
              text
              @click="handleClearCache(row.name)"
            >
              <el-icon><Delete /></el-icon>清除
            </el-button>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Redis 服务器详情 -->
    <el-card shadow="hover" style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>Redis 服务器信息</span>
          <el-button size="small" @click="loadStats">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="Redis 版本">{{ redisInfo.redisVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内存使用">{{ redisInfo.usedMemory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内存峰值">{{ redisInfo.usedMemoryPeak || '-' }}</el-descriptions-item>
        <el-descriptions-item label="连接客户端">{{ redisInfo.connectedClients || '-' }}</el-descriptions-item>
        <el-descriptions-item label="运行天数">{{ redisInfo.uptimeInDays || '-' }} 天</el-descriptions-item>
        <el-descriptions-item label="总 Key 数">{{ redisInfo.totalKeys || 0 }}</el-descriptions-item>
        <el-descriptions-item label="命中次数">
          <span style="color: #67c23a;">{{ formatNumber(redisInfo.keyspaceHits) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="未命中次数">
          <span style="color: #f56c6c;">{{ formatNumber(redisInfo.keyspaceMisses) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="命中率">
          <el-progress 
            :percentage="Number(redisInfo.hitRate) || 0" 
            :color="getHitRateColor(Number(redisInfo.hitRate) || 0)"
            :stroke-width="16"
          />
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { getCacheStats, evictCache, evictAllCaches } from '@/api/cache'
import type { RedisInfo, CacheSpaceStat } from '@/api/cache'

const redisInfo = ref<Partial<RedisInfo>>({})
const cacheSpaces = ref<CacheSpaceStat[]>([])

const hitRateChartRef = ref<HTMLDivElement>()
const spaceChartRef = ref<HTMLDivElement>()
let hitRateChart: echarts.ECharts | null = null
let spaceChart: echarts.ECharts | null = null

// Spring Cache 缓存空间（可清除的）
const springCacheNames = ['articleDetail', 'featuredArticles', 'popularArticles', 'categories', 'tags', 'dashboardStats']

function isSpringCache(name: string) {
  return springCacheNames.includes(name)
}

function getTagType(name: string): string {
  const typeMap: Record<string, string> = {
    articleDetail: '',
    featuredArticles: 'success',
    popularArticles: 'warning',
    categories: 'info',
    tags: 'info',
    dashboardStats: 'danger',
    viewCountBuffer: 'warning',
    dailyViews: 'success',
    jwtBlacklist: 'danger',
    rateLimit: 'danger',
  }
  return typeMap[name] || ''
}

function getHitRateColor(rate: number): string {
  if (rate >= 80) return '#67c23a'
  if (rate >= 50) return '#e6a23c'
  return '#f56c6c'
}

function formatNumber(num: number | undefined): string {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}

async function loadStats() {
  try {
    const res = await getCacheStats()
    if (res.data) {
      redisInfo.value = res.data.redisInfo || {}
      cacheSpaces.value = res.data.cacheSpaces || []
      await nextTick()
      renderCharts()
    }
  } catch (e) {
    console.error('加载缓存统计失败', e)
  }
}

function renderCharts() {
  renderHitRateChart()
  renderSpaceChart()
}

function renderHitRateChart() {
  if (!hitRateChartRef.value) return
  if (!hitRateChart) {
    hitRateChart = echarts.init(hitRateChartRef.value)
  }

  const hits = redisInfo.value.keyspaceHits || 0
  const misses = redisInfo.value.keyspaceMisses || 0

  hitRateChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data: [
        { value: hits, name: '命中', itemStyle: { color: '#67c23a' } },
        { value: misses, name: '未命中', itemStyle: { color: '#f56c6c' } },
      ]
    }]
  })
}

function renderSpaceChart() {
  if (!spaceChartRef.value) return
  if (!spaceChart) {
    spaceChart = echarts.init(spaceChartRef.value)
  }

  const data = cacheSpaces.value
    .filter(s => s.keyCount > 0)
    .map(s => ({ name: s.description || s.name, value: s.keyCount }))

  if (data.length === 0) {
    data.push({ name: '暂无缓存数据', value: 0 })
  }

  spaceChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 个Key ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: '65%',
      data,
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' }
      }
    }]
  })
}

async function handleClearCache(cacheName: string) {
  try {
    await ElMessageBox.confirm(`确定清除缓存空间 [${cacheName}] 吗？`, '确认', {
      confirmButtonText: '清除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await evictCache(cacheName)
    ElMessage.success(`缓存空间 [${cacheName}] 已清除`)
    loadStats()
  } catch {
    // 用户取消
  }
}

async function handleClearAll() {
  try {
    await ElMessageBox.confirm('确定清除所有 Spring Cache 缓存吗？不影响 JWT 黑名单和限流计数器。', '确认', {
      confirmButtonText: '全部清除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await evictAllCaches()
    ElMessage.success('所有缓存已清除')
    loadStats()
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.cache-monitor {
  padding: 0;
}

.stat-cards .el-col {
  margin-bottom: 0;
}

.stat-card {
  border-radius: 12px;
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
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

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.text-muted {
  color: #c0c4cc;
}
</style>
