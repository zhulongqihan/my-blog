<template>
  <div class="ai-observability">
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6"><el-card><div class="stat-label">近期开启请求</div><div class="stat-value">{{ stats.total }}</div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat-label">成功率</div><div class="stat-value">{{ stats.successRate }}%</div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat-label">平均总耗时</div><div class="stat-value">{{ stats.avgLatencyMs }} ms</div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat-label">最近模型</div><div class="stat-value model">{{ traces[0]?.model || '-' }}</div></el-card></el-col>
    </el-row>

    <el-card class="trace-card">
      <template #header>
        <div class="card-header"><span>AI Gateway 请求轨迹</span><el-button :icon="Refresh" size="small" @click="load">刷新</el-button></div>
      </template>
      <el-table :data="traces" stripe>
        <el-table-column prop="createdAt" label="时间" width="190" />
        <el-table-column prop="requestId" label="请求 ID" min-width="210" show-overflow-tooltip />
        <el-table-column prop="mode" label="场景" width="120" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 'success' ? 'success' : 'danger'">{{ row.status }}</el-tag></template></el-table-column>
        <el-table-column label="首字节" width="110"><template #default="{ row }">{{ row.timeToFirstTokenMs }} ms</template></el-table-column>
        <el-table-column label="总耗时" width="110"><template #default="{ row }">{{ row.totalLatencyMs }} ms</template></el-table-column>
        <el-table-column label="输入/输出" width="120"><template #default="{ row }">{{ row.inputChars }} / {{ row.outputChars }}</template></el-table-column>
        <el-table-column label="检索来源" min-width="220"><template #default="{ row }"><el-tag v-for="source in row.sources" :key="source.articleId" size="small" class="source-tag">{{ source.title }}</el-tag><span v-if="!row.sources?.length">无</span></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getAiTraces } from '@/api/ai'
import type { AiTrace, AiTraceStats } from '@/api/ai'

const traces = ref<AiTrace[]>([])
const stats = ref<AiTraceStats>({ traces: [], total: 0, successRate: 0, avgLatencyMs: 0 })

async function load() {
  try {
    const response = await getAiTraces()
    if (response.data) {
      stats.value = response.data
      traces.value = response.data.traces || []
    }
  } catch (error) {
    console.error('加载 AI 轨迹失败', error)
  }
}

onMounted(load)
</script>

<style scoped>
.stat-cards { margin-bottom: 16px; }
.stat-label { color: #909399; font-size: 13px; }
.stat-value { color: #303133; font-size: 26px; font-weight: 700; margin-top: 8px; }
.stat-value.model { font-size: 18px; }
.trace-card { min-height: 520px; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.source-tag { margin: 2px 4px 2px 0; max-width: 180px; overflow: hidden; text-overflow: ellipsis; vertical-align: middle; }
</style>
