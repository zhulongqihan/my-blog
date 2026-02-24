<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card-body">
            <div class="stat-info">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.title }}</div>
            </div>
            <el-icon :size="48" :color="card.color" class="stat-icon">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <div class="stat-footer">
            今日: <span :style="{ color: card.color }">+{{ card.today }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover">
          <template #header>
            <span>近7天访问趋势</span>
          </template>
          <v-chart :option="trendChartOption" style="height: 350px" autoresize />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover">
          <template #header>
            <span>分类文章统计</span>
          </template>
          <v-chart :option="categoryChartOption" style="height: 350px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- 列表区域 -->
    <el-row :gutter="20" class="list-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <span>热门文章</span>
          </template>
          <el-table :data="stats?.popularArticles || []" stripe size="small">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column prop="viewCount" label="浏览量" width="80" align="center" />
            <el-table-column prop="commentCount" label="评论数" width="80" align="center" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <span>最新评论</span>
          </template>
          <div v-if="stats?.recentComments?.length" class="comment-list">
            <div v-for="comment in stats.recentComments" :key="comment.id" class="comment-item">
              <div class="comment-header">
                <el-tag size="small" type="primary">{{ comment.authorName }}</el-tag>
                <span class="comment-time">{{ comment.createdAt }}</span>
              </div>
              <div class="comment-content">{{ comment.content }}</div>
              <div class="comment-article">
                评论于：<em>{{ comment.articleTitle }}</em>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无评论" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'
import { getStats } from '@/api/dashboard'
import type { DashboardStats } from '@/types'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const stats = ref<DashboardStats | null>(null)
const loading = ref(true)

const statCards = computed(() => [
  { title: '文章总数', value: stats.value?.totalArticles ?? 0, today: stats.value?.todayArticles ?? 0, icon: 'Document', color: '#409eff' },
  { title: '评论总数', value: stats.value?.totalComments ?? 0, today: stats.value?.todayComments ?? 0, icon: 'ChatDotRound', color: '#67c23a' },
  { title: '访问总量', value: stats.value?.totalViews ?? 0, today: stats.value?.todayViews ?? 0, icon: 'View', color: '#e6a23c' },
  { title: '用户总数', value: stats.value?.totalUsers ?? 0, today: stats.value?.todayUsers ?? 0, icon: 'User', color: '#f56c6c' },
])

const trendChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['访问量', '文章数'] },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: (stats.value?.viewTrend || []).map(item => item.date),
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: '访问量',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      itemStyle: { color: '#409eff' },
      data: (stats.value?.viewTrend || []).map(item => item.count),
    },
    {
      name: '文章数',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      itemStyle: { color: '#67c23a' },
      data: (stats.value?.articleTrend || []).map(item => item.count),
    },
  ],
}))

const categoryChartOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 篇 ({d}%)' },
  legend: { orient: 'vertical', left: 'left', top: 'center' },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: (stats.value?.categoryStats || []).map(item => ({
        name: item.name,
        value: item.articleCount,
      })),
    },
  ],
}))

onMounted(async () => {
  try {
    const res = await getStats()
    stats.value = res.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stat-card {
  height: 130px;
}

.stat-card-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-icon {
  opacity: 0.6;
}

.stat-footer {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
  font-size: 13px;
  color: #909399;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 310px;
  overflow-y: auto;
}

.comment-item {
  padding: 10px;
  background: #f9f9f9;
  border-radius: 6px;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.comment-time {
  font-size: 12px;
  color: #c0c4cc;
}

.comment-content {
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}

.comment-article {
  font-size: 12px;
  color: #909399;
}

.comment-article em {
  color: #409eff;
  font-style: normal;
}
</style>
