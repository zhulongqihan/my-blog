<template>
  <div class="article-list">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索标题"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryParams.categoryId" placeholder="全部分类" clearable style="width: 150px">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.published" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="已发布" :value="true" />
            <el-option label="草稿" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <el-card shadow="never" style="margin-top: 16px">
      <div class="toolbar">
        <el-button type="danger" icon="Delete" :disabled="!selectedIds.length" @click="handleBatchDelete">
          批量删除 ({{ selectedIds.length }})
        </el-button>
      </div>

      <!-- 文章表格 -->
      <el-table
        :data="articles"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="title-cell">
              <el-tag v-if="row.featured" type="warning" size="small" effect="dark" style="margin-right: 6px">置顶</el-tag>
              {{ row.title }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.categoryName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tagNames" label="标签" width="160">
          <template #default="{ row }">
            <el-tag
              v-for="tag in row.tagNames"
              :key="tag"
              size="small"
              type="info"
              style="margin: 2px"
            >{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="70" align="center" />
        <el-table-column prop="commentCount" label="评论" width="70" align="center" />
        <el-table-column prop="published" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.published ? 'success' : 'info'" size="small">
              {{ row.published ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.published"
              type="primary"
              size="small"
              text
              @click="handlePublish(row.id)"
            >发布</el-button>
            <el-button
              v-else
              type="warning"
              size="small"
              text
              @click="handleUnpublish(row.id)"
            >撤回</el-button>
            <el-button
              v-if="!row.featured"
              type="success"
              size="small"
              text
              @click="handleTop(row.id)"
            >置顶</el-button>
            <el-button
              v-else
              type="info"
              size="small"
              text
              @click="handleUntop(row.id)"
            >取消置顶</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchArticles"
          @current-change="fetchArticles"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getArticles,
  publishArticle,
  unpublishArticle,
  topArticle,
  untopArticle,
  batchDeleteArticles,
} from '@/api/article'
import { getCategories } from '@/api/category'
import type { ArticleAdminResponse, ArticleQueryParams, Category } from '@/types'

const loading = ref(false)
const articles = ref<ArticleAdminResponse[]>([])
const categories = ref<Category[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])

const queryParams = reactive<ArticleQueryParams>({
  page: 1,
  size: 10,
  keyword: '',
  categoryId: undefined,
  published: undefined,
  sortField: 'createdAt',
  sortOrder: 'desc',
})

async function fetchArticles() {
  loading.value = true
  try {
    const res = await getArticles(queryParams)
    articles.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data
  } catch { /* ignore */ }
}

function handleSearch() {
  queryParams.page = 1
  fetchArticles()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.categoryId = undefined
  queryParams.published = undefined
  queryParams.page = 1
  fetchArticles()
}

function handleSelectionChange(rows: ArticleAdminResponse[]) {
  selectedIds.value = rows.map(r => r.id)
}

async function handlePublish(id: number) {
  await publishArticle(id)
  ElMessage.success('发布成功')
  fetchArticles()
}

async function handleUnpublish(id: number) {
  await ElMessageBox.confirm('确定要撤回该文章为草稿吗？', '确认')
  await unpublishArticle(id)
  ElMessage.success('已撤回为草稿')
  fetchArticles()
}

async function handleTop(id: number) {
  await topArticle(id)
  ElMessage.success('置顶成功')
  fetchArticles()
}

async function handleUntop(id: number) {
  await untopArticle(id)
  ElMessage.success('已取消置顶')
  fetchArticles()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 篇文章吗？此操作不可恢复！`, '批量删除', {
    type: 'warning',
    confirmButtonText: '确认删除',
    confirmButtonClass: 'el-button--danger',
  })
  await batchDeleteArticles(selectedIds.value)
  ElMessage.success('删除成功')
  selectedIds.value = []
  fetchArticles()
}

onMounted(() => {
  fetchArticles()
  fetchCategories()
})
</script>

<style scoped>
.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.toolbar {
  margin-bottom: 16px;
}

.title-cell {
  display: flex;
  align-items: center;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
