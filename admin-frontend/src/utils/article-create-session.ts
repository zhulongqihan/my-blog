import type { ArticleDetail, ArticleForm, Result } from '@/types'

interface RecoveryRecord {
  version: 1
  key: string
  payload: ArticleForm
  draft: ArticleForm
  articleId?: number
}

type Create = (payload: ArticleForm, key: string) => Promise<Result<ArticleDetail>>
type Update = (id: number, payload: ArticleForm) => Promise<Result<ArticleDetail>>

const copy = (form: ArticleForm): ArticleForm => ({ ...JSON.parse(JSON.stringify(form)), categoryId: form.categoryId ?? null })
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function newRequestKey(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID()
  // getRandomValues is also available when the existing admin is opened over HTTP.
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

export class DraftRecoveryError extends Error {}

export function articleSaveOwner(): string {
  try {
    const user = JSON.parse(localStorage.getItem('admin_user') || 'null')
    if (Number.isSafeInteger(user?.id) && user.id > 0) return `id:${user.id}`
    if (typeof user?.username === 'string' && user.username) return `username:${user.username}`
  } catch { /* An unavailable identity must not mix recovery records between accounts. */ }
  throw new DraftRecoveryError('无法确定当前账号，请刷新页面后再保存；当前输入已保留。')
}

function validForm(value: any): value is ArticleForm {
  return value && ['title', 'summary', 'content', 'coverImage'].every(key => typeof value[key] === 'string')
    && (value.categoryId == null || Number.isSafeInteger(value.categoryId))
    && Array.isArray(value.tagIds) && value.tagIds.every((id: unknown) => Number.isSafeInteger(id))
    && typeof value.published === 'boolean' && typeof value.featured === 'boolean'
}

function parseRecord(raw: string): RecoveryRecord {
  const record = JSON.parse(raw)
  if (record.version !== 1 || !uuid.test(record.key) || !validForm(record.payload) || !validForm(record.draft)
    || (record.articleId !== undefined && (!Number.isSafeInteger(record.articleId) || record.articleId <= 0))) {
    throw new Error('Invalid recovery record')
  }
  // Older clearable selects omitted undefined categoryId when serializing recovery records.
  return { ...record, payload: copy(record.payload), draft: copy(record.draft) }
}

/** One create operation per account and browser tab; retries never change its request payload. */
export class ArticleCreateSession {
  private record?: RecoveryRecord
  private readonly storageKey: string
  private active = true
  articleId?: number

  constructor(readonly owner: string, private readonly storage: Storage) {
    this.storageKey = `myblog:article-create:v1:${encodeURIComponent(owner)}`
    try {
      const raw = storage.getItem(this.storageKey)
      if (!raw) return
      const record = parseRecord(raw)
      this.record = record
      this.articleId = record.articleId
    } catch {
      throw new DraftRecoveryError('恢复记录无法读取，已暂停新建以避免重复；请保留输入并联系维护者。')
    }
  }

  get draft(): ArticleForm | undefined { return this.record && copy(this.record.draft) }
  get pending(): boolean { return !!this.record && !this.articleId }

  private currentRecord(): RecoveryRecord | undefined {
    try {
      const raw = this.storage.getItem(this.storageKey)
      return raw ? parseRecord(raw) : undefined
    } catch {
      throw new DraftRecoveryError('恢复记录无法读取，请保留当前输入并刷新后重试。')
    }
  }

  private persist() {
    try {
      this.storage.setItem(this.storageKey, JSON.stringify(this.record))
    } catch {
      throw new DraftRecoveryError('无法保存恢复记录，已暂停提交；请保留输入并检查浏览器存储后重试。')
    }
  }

  rememberDraft(draft: ArticleForm) {
    if (!this.record || !this.active) return
    this.checkOwner()
    const latest = this.currentRecord()
    if (latest && latest.key !== this.record.key) {
      throw new DraftRecoveryError('保存会话已变化，请保留当前输入并刷新后重试。')
    }
    // Another instance may have received the ID while this editor was still open.
    this.record = { ...(latest || this.record), draft: copy(draft) }
    this.articleId = this.record.articleId ?? this.articleId
    this.persist()
  }

  async save(payload: ArticleForm, draft: ArticleForm, create: Create, update: Update): Promise<void> {
    this.checkOwner()
    const firstAttempt = !this.record && !this.articleId
    if (!this.record && !this.articleId) {
      this.record = { version: 1, key: newRequestKey(), payload: copy(payload), draft: copy(draft) }
    }
    this.rememberDraft(draft) // Persist the operation before any network write.
    if (!this.articleId) {
      const record = this.record!
      let response: Result<ArticleDetail>
      try {
        response = await create(copy(record.payload), record.key)
      } catch (error: any) {
        // Only a fresh operation's explicit validation rejection proves that nothing was created.
        // A replay may refer to a previously committed operation, so its key must never be rotated.
        if (this.active && firstAttempt && [400, 422].includes(error?.response?.status)) this.clear()
        throw error
      }
      if (!Number.isSafeInteger(response.data?.id) || response.data.id <= 0) {
        throw new Error('Create response did not contain an article ID')
      }
      this.articleId = response.data.id // Bind immediately, including when persistence or navigation fails.
      const latest = this.currentRecord()
      if (!latest || latest.key !== record.key || (latest.articleId && latest.articleId !== this.articleId)) {
        throw new DraftRecoveryError('保存会话已变化，原文章已保存；请保留当前输入并刷新后重试。')
      }
      this.checkOwner()
      // Patch only the ID into the latest stored draft; a reopened editor may have newer input.
      this.record = { ...latest, articleId: this.articleId }
      this.persist()
      if (!this.active || JSON.stringify(payload) === JSON.stringify(record.payload)) return
    }
    if (!this.active) return
    this.checkOwner()
    await update(this.articleId, copy(payload))
  }

  checkOwner() {
    if (this.owner !== articleSaveOwner()) {
      throw new DraftRecoveryError('登录账号已变化，请刷新后再保存；当前输入已保留。')
    }
  }

  deactivate() { this.active = false }

  clear() {
    const latest = this.currentRecord()
    if (latest?.key === this.record?.key) {
      this.checkOwner()
      this.storage.removeItem(this.storageKey)
    }
    this.record = undefined
    // Keep the ID in memory if the editor remains mounted after navigation.
  }
}
