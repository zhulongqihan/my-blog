<template>
  <div class="tag-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>标签管理</span>
          <el-button type="primary" icon="Plus" @click="openDialog()">新增标签</el-button>
        </div>
      </template>

      <el-table :data="tags" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="标签名称" width="200">
          <template #default="{ row }">
            <el-tag :color="row.color" effect="dark" size="small" style="border: none">
              {{ row.name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="color" label="颜色" width="120" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="primary" size="small" text icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" text icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑标签' : '新增标签'"
      width="420px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getTags, createTag, updateTag, deleteTag } from '@/api/tag'
import type { Tag, TagForm } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const tags = ref<Tag[]>([])
const formRef = ref<FormInstance>()

const form = reactive<TagForm>({
  name: '',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
}

async function fetchTags() {
  loading.value = true
  try {
    const res = await getTags()
    tags.value = res.data
  } finally {
    loading.value = false
  }
}

function openDialog(row?: Tag) {
  dialogVisible.value = true
  if (row) {
    isEdit.value = true
    editId.value = row.id
    form.name = row.name
  } else {
    isEdit.value = false
    editId.value = null
    form.name = ''
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()

  submitting.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateTag(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createTag(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchTags()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: Tag) {
  await ElMessageBox.confirm(`确定要删除标签「${row.name}」吗？`, '确认删除', { type: 'warning' })
  await deleteTag(row.id)
  ElMessage.success('删除成功')
  fetchTags()
}

onMounted(fetchTags)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
