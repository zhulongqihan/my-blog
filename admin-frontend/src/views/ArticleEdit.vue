<template>
  <div class="article-edit">
    <el-card shadow="never">
      <!-- 顶部操作栏 -->
      <div class="header">
        <h3>{{ isEdit ? '编辑文章' : '新建文章' }}</h3>
        <div class="actions">
          <el-button @click="handleBack">返回</el-button>
          <el-button type="info" @click="handleSaveDraft" :loading="saving" :disabled="!canSave">存为草稿</el-button>
          <el-button type="primary" @click="handlePublish" :loading="saving" :disabled="!canSave">发布文章</el-button>
        </div>
      </div>

      <div v-if="articleError" class="load-error" role="alert">
        <span>文章加载失败，请重试后再编辑。</span>
        <el-button @click="loadArticle()" :loading="pageLoading">重新加载文章</el-button>
      </div>
      <div v-if="optionsError" class="load-error" role="alert">
        <span>分类或标签加载失败，已保留正文；重试成功后可以保存。</span>
        <el-button @click="loadOptions()" :loading="optionsLoading">重新加载分类和标签</el-button>
      </div>
      <p v-if="optionsLoading && articleLoaded" role="status">正在加载分类和标签，完成后即可保存。</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        v-loading="pageLoading"
        element-loading-text="正在加载文章…"
        :aria-busy="pageLoading"
        :disabled="pageLoading || !articleLoaded || saving"
      >
        <!-- 标题 -->
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入文章标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <!-- 分类 + 标签 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width: 100%" :loading="optionsLoading" :disabled="!optionsLoaded">
                <el-option
                  v-for="cat in categories"
                  :key="cat.id"
                  :label="cat.name"
                  :value="cat.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标签" prop="tagIds">
              <el-select
                v-model="form.tagIds"
                multiple
                placeholder="选择标签（可多选）"
                style="width: 100%"
                :loading="optionsLoading"
                :disabled="!optionsLoaded"
              >
                <el-option
                  v-for="tag in tags"
                  :key="tag.id"
                  :label="tag.name"
                  :value="tag.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 摘要 -->
        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            placeholder="文章摘要，留空将自动截取正文前 200 字"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 封面图 -->
        <el-form-item label="封面图">
          <div class="cover-upload">
            <el-input
              v-model="form.coverImage"
              placeholder="输入图片 URL 或上传图片"
              style="flex: 1"
            />
            <el-upload
              :show-file-list="false"
              :before-upload="handleCoverUpload"
              accept="image/*"
              style="margin-left: 8px"
            >
              <el-button type="primary" icon="Upload">上传</el-button>
            </el-upload>
          </div>
          <div v-if="form.coverImage" class="cover-preview">
            <el-image
              :src="resolveImageUrl(form.coverImage)"
              fit="cover"
              style="width: 200px; height: 120px; border-radius: 4px; margin-top: 8px"
            />
          </div>
        </el-form-item>

        <!-- 置顶 -->
        <el-form-item label="置顶">
          <el-switch v-model="form.featured" />
        </el-form-item>

        <!-- Markdown 编辑器 -->
        <el-form-item label="正文" prop="content" class="editor-item">
          <MdEditor
            v-if="articleLoaded"
            v-model="form.content"
            :theme="editorTheme"
            language="zh-CN"
            :toolbars="toolbars"
            :preview="true"
            show-code-row-number
            @onUploadImg="handleEditorUpload"
            style="height: 600px"
          />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import type { FormInstance, FormRules, UploadRawFile } from 'element-plus'
import { MdEditor, type ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { getArticleDetail, createArticle, updateArticle, uploadImage } from '@/api/article'
import { getCategories } from '@/api/category'
import { getTags } from '@/api/tag'
import type { Category, Tag, ArticleForm } from '@/types'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)
const pageLoading = ref(true)
const optionsLoading = ref(true)
const articleLoaded = ref(false)
const optionsLoaded = ref(false)
const articleError = ref(false)
const optionsError = ref(false)
let loadGeneration = 0
const canSave = computed(() => articleLoaded.value && optionsLoaded.value && !pageLoading.value && !optionsLoading.value && !saving.value)

const isEdit = computed(() => !!route.params.id)
const articleId = computed(() => Number(route.params.id))

// 编辑器主题
const editorTheme = ref<'light' | 'dark'>('light')

// 编辑器工具栏配置
const toolbars: ToolbarNames[] = [
  'bold', 'underline', 'italic', 'strikeThrough',
  '-',
  'title', 'sub', 'sup', 'quote', 'unorderedList', 'orderedList', 'task',
  '-',
  'codeRow', 'code', 'link', 'image', 'table',
  '-',
  'revoke', 'next',
  '=',
  'pageFullscreen', 'fullscreen', 'preview', 'catalog',
]

// 分类和标签数据
const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])

// 表单数据
const form = reactive<ArticleForm>({
  title: '',
  summary: '',
  content: '',
  coverImage: '',
  categoryId: null,
  tagIds: [],
  published: false,
  featured: false,
})

// 表单校验规则
const rules: FormRules = {
  title: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过 200 字', trigger: 'blur' },
  ],
  content: [
    { required: true, message: '请输入文章正文', trigger: 'blur' },
  ],
}

/**
 * 将可能的相对路径转为可访问的完整 URL
 */
function resolveImageUrl(url: string): string {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  // 开发环境拼接 API base URL
  const base = import.meta.env.VITE_API_BASE_URL || ''
  if (url.startsWith('/uploads')) {
    return base ? base.replace('/api', '') + url : url
  }
  return url
}

/**
 * 封面图上传
 */
async function handleCoverUpload(file: UploadRawFile) {
  try {
    const res = await uploadImage(file)
    form.coverImage = res.data.url
    ElMessage.success('封面上传成功')
  } catch {
    ElMessage.error('封面上传失败')
  }
  return false // 阻止 el-upload 默认行为
}

/**
 * 编辑器内图片上传回调
 * md-editor-v3 的 onUploadImg 回调：files 是文件数组，callback 接收 URL 数组
 */
async function handleEditorUpload(files: File[], callback: (urls: string[]) => void) {
  const urls: string[] = []
  for (const file of files) {
    try {
      const res = await uploadImage(file)
      urls.push(resolveImageUrl(res.data.url))
    } catch {
      ElMessage.error(`上传 ${file.name} 失败`)
    }
  }
  callback(urls)
}

/**
 * 保存为草稿
 */
async function handleSaveDraft() {
  if (!canSave.value) return
  const generation = loadGeneration
  await formRef.value?.validate()
  if (generation !== loadGeneration || !canSave.value) return
  form.published = false
  await saveArticle()
}

/**
 * 发布文章
 */
async function handlePublish() {
  if (!canSave.value) return
  const generation = loadGeneration
  await formRef.value?.validate()
  if (generation !== loadGeneration || !canSave.value) return
  form.published = true
  await saveArticle()
}

/**
 * 保存文章（创建或更新）
 */
async function saveArticle() {
  saving.value = true
  try {
    // 自动生成摘要
    const submitData: ArticleForm = { ...form }
    if (!submitData.summary && submitData.content) {
      // 去除 Markdown 标记，截取前 200 字
      const plainText = submitData.content
        .replace(/[#*`>\[\]()!~_\-|]/g, '')
        .replace(/\n+/g, ' ')
        .trim()
      submitData.summary = plainText.substring(0, 200)
    }

    if (isEdit.value) {
      await updateArticle(articleId.value, submitData)
      ElMessage.success('文章更新成功')
    } else {
      await createArticle(submitData)
      ElMessage.success('文章创建成功')
    }
    router.push('/articles')
  } catch (e: any) {
    // 错误已由 request 拦截器处理
  } finally {
    saving.value = false
  }
}

/**
 * 返回文章列表
 */
function handleBack() {
  router.push('/articles')
}

/**
 * 加载分类和标签
 */
async function loadOptions(generation = loadGeneration) {
  optionsLoading.value = true
  optionsLoaded.value = false
  optionsError.value = false
  try {
    const [catRes, tagRes] = await Promise.all([getCategories(), getTags()])
    if (generation !== loadGeneration) return
    categories.value = catRes.data
    tags.value = tagRes.data
    optionsLoaded.value = true
  } catch {
    if (generation === loadGeneration) optionsError.value = true
  } finally {
    if (generation === loadGeneration) optionsLoading.value = false
  }
}

/**
 * 编辑模式：加载文章数据回显
 */
async function loadArticle(generation = loadGeneration) {
  articleError.value = false
  articleLoaded.value = !isEdit.value
  if (!isEdit.value) {
    pageLoading.value = false
    return
  }
  pageLoading.value = true
  try {
    const res = await getArticleDetail(articleId.value)
    if (generation !== loadGeneration) return
    const article = res.data
    form.title = article.title
    form.summary = article.summary || ''
    form.content = article.content
    form.coverImage = article.coverImage || ''
    form.categoryId = article.category?.id ?? null
    form.tagIds = article.tags?.map(t => t.id) ?? []
    form.published = article.published
    form.featured = article.featured
    articleLoaded.value = true
  } catch {
    if (generation === loadGeneration) articleError.value = true
  } finally {
    if (generation === loadGeneration) pageLoading.value = false
  }
}

watch(() => route.params.id, () => {
  const generation = ++loadGeneration
  Object.assign(form, { title: '', summary: '', content: '', coverImage: '', categoryId: null, tagIds: [], published: false, featured: false })
  // Independent reads start together. Metadata must not delay the article body.
  void loadOptions(generation)
  void loadArticle(generation)
}, { immediate: true })

onBeforeUnmount(() => { loadGeneration++ })
</script>

<style scoped>
.article-edit {
  padding: 0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h3 {
  margin: 0;
  font-size: 18px;
}

.actions {
  display: flex;
  gap: 8px;
}

.load-error {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px;
  margin-bottom: 16px;
  background: #fdf6ec;
  color: #8a5b1d;
  border-radius: 4px;
}

.cover-upload {
  display: flex;
  align-items: center;
  width: 100%;
}

.editor-item :deep(.el-form-item__content) {
  display: block;
}
</style>
