<template>
  <div class="comment-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>评论管理</span>
          <el-button type="danger" icon="Delete" :disabled="!selectedIds.length" @click="handleBatchDelete">
            批量删除 ({{ selectedIds.length }})
          </el-button>
        </div>
      </template>

      <el-table
        :data="comments"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        stripe
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="评论内容" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.content }}
          </template>
        </el-table-column>
        <el-table-column label="文章" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.article?.title || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="评论者" width="120">
          <template #default="{ row }">
            {{ row.user?.nickname || row.guestName || '匿名' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.approved ? 'success' : 'warning'" size="small">
              {{ row.approved ? '已通过' : '待审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.approved"
              type="success"
              size="small"
              text
              @click="handleApprove(row.id)"
            >通过</el-button>
            <el-button
              type="danger"
              size="small"
              text
              @click="handleDelete(row.id)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchComments"
          @current-change="fetchComments"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getComments, approveComment, deleteComment, batchDeleteComments } from '@/api/comment'
import type { Comment } from '@/types'

const loading = ref(false)
const comments = ref<Comment[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedIds = ref<number[]>([])

async function fetchComments() {
  loading.value = true
  try {
    const res = await getComments(currentPage.value, pageSize.value)
    comments.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(rows: Comment[]) {
  selectedIds.value = rows.map(r => r.id)
}

async function handleApprove(id: number) {
  await approveComment(id)
  ElMessage.success('审核通过')
  fetchComments()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该评论吗？', '确认删除', { type: 'warning' })
  await deleteComment(id)
  ElMessage.success('删除成功')
  fetchComments()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条评论吗？`, '批量删除', {
    type: 'warning',
  })
  await batchDeleteComments(selectedIds.value)
  ElMessage.success('删除成功')
  selectedIds.value = []
  fetchComments()
}

onMounted(fetchComments)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
