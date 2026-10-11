<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, DocumentCopy, Edit, Picture } from '@element-plus/icons-vue'
import {
  FILE_MAX_SIZE_BYTE,
  FILE_MAX_SIZE_MESSAGE,
  avatarSrc,
  uploadBroadcastFile,
} from '@/api'
import type { BroadcastUserinfo } from '@/api'
import BroadcastPostCard from '@/components/BroadcastPostCard.vue'
import { useBroadcastStore } from '@/stores/broadcast'
import { useUserStore } from '@/stores/user'
import { copyText } from '@/utils/clipboard'
import { shortKey } from '@/utils/display'
import { showError } from '@/utils/feedback'

/**
 * 用户主页：昵称、头像、简介，以及 TA 的帖子；看自己的主页时还能编辑资料。
 *
 * <p>广播没有关注关系，所以主页只做"看"和"改自己"两件事。头像也走广播文件通道：选图时只本地预览，
 * 到提交时才上传、把拿到的文件 ID 存进资料里——中途取消就不会留下没人引用的孤儿文件。</p>
 */

const route = useRoute()
const router = useRouter()
const broadcastStore = useBroadcastStore()
const userStore = useUserStore()

/** 主页所属的用户公钥。 */
const uid = computed(() => String(route.params.uid ?? ''))

/** 是不是自己的主页。 */
const isSelf = computed(() => uid.value.length > 0 && uid.value === userStore.publicKey)

/** 资料。 */
const profile = computed(() => broadcastStore.profileOf(uid.value))

/** 展示名：没有资料就退回公钥末几位。 */
const displayName = computed(() => profile.value?.nickname ?? shortKey(uid.value, 5))

/** 是否正在编辑资料。 */
const editing = ref(false)

/** 是否正在保存。 */
const saving = ref(false)

/** 编辑中的资料。 */
const form = ref({ nickname: '', gender: '', avatarUrl: '', description: '' })

/** 新选的头像文件：提交时才上传。 */
const avatarFile = ref<File | null>(null)

/** 新选头像的本地预览地址。 */
const avatarPreviewUrl = ref('')

/** 头像最终展示地址：本地预览优先，其次已保存的资料。 */
const avatarDisplay = computed(() => avatarPreviewUrl.value || avatarSrc(profile.value?.avatarUrl) || '')

/** 隐藏的头像选择框。 */
const avatarInput = ref<HTMLInputElement>()

/** 是否放大查看头像。 */
const avatarPreviewVisible = ref(false)

/** 主页滚动容器。 */
const listEl = ref<HTMLDivElement>()

/** 内容容器，用来观察内容高度变化。 */
const contentEl = ref<HTMLDivElement>()

/** 待定位的帖子 ID：从主页点开帖子再返回时，把那条帖子滚回可视区中间。 */
const pendingAnchor = ref('')

/** 重新定位的截止时间：附件加载完之前一直跟着挪，之后就交给用户。 */
let anchorDeadline = 0

/** 内容高度观察器。 */
let contentObserver: ResizeObserver | null = null

watch(uid, async (value) => {
  if (value.length === 0) {
    return
  }
  editing.value = false
  // 换个人看时把没提交的头像丢掉，免得误传到别人名下
  clearAvatarFile()
  try {
    await broadcastStore.loadProfile(value, true)
    fillForm()
    await broadcastStore.loadUserPosts(value, true)
    // 把上次从主页点开的那条帖子放回可视区中间（换人看时锚点不套用）
    pendingAnchor.value = broadcastStore.profileAnchorUid === value
      ? broadcastStore.profileAnchorId
      : ''
    anchorDeadline = Date.now() + 3000
    await nextTick()
    applyAnchor()
    observeContent()
  } catch (error) {
    showError(error, '加载主页失败')
  }
}, { immediate: true })

watch(() => userStore.publicKey, () => {
  // 公钥是异步取到的：先打开自己的主页时补一次
  if (isSelf.value && profile.value === null) {
    void broadcastStore.loadProfile(uid.value, true).then(fillForm).catch(() => {})
  }
}, { immediate: true })

onUnmounted(() => {
  clearAvatarFile()
  contentObserver?.disconnect()
  contentObserver = null
})

/**
 * 把锚点帖子滚到可视区中间。
 */
function applyAnchor(): void {
  const id = pendingAnchor.value
  const container = listEl.value
  if (id.length === 0 || !container) {
    return
  }
  const target = container.querySelector(`[data-item-id="${id}"]`)
  target?.scrollIntoView({ block: 'center' })
}

/**
 * 观察内容高度：附件异步加载会让内容变高、锚点位置跟着变，这段时间里持续把它拉回中间。
 */
function observeContent(): void {
  contentObserver?.disconnect()
  contentObserver = null
  const content = contentEl.value
  if (!content || typeof ResizeObserver === 'undefined') {
    return
  }
  contentObserver = new ResizeObserver(() => {
    if (pendingAnchor.value.length === 0 || Date.now() > anchorDeadline) {
      return
    }
    applyAnchor()
  })
  contentObserver.observe(content)
}

/**
 * 用户自己滚动：不要再把视图拽回锚点。
 */
function onUserScroll(): void {
  pendingAnchor.value = ''
}

/**
 * 返回上一页；直接打开（没有历史）时回时间线。
 */
function onBack(): void {
  if (window.history.state?.back) {
    router.back()
    return
  }
  void router.push({ name: 'broadcast-feed' })
}

/**
 * 复制当前主页的公钥，方便确认搜到的确实是这个人。
 */
async function onCopyKey(): Promise<void> {
  const ok = await copyText(uid.value)
  if (ok) {
    ElMessage.success('已复制公钥')
  } else {
    ElMessage.error('复制失败，请手动选中复制')
  }
}

/**
 * 把资料填进编辑表单。
 */
function fillForm(): void {
  const info = profile.value
  form.value = {
    nickname: info?.nickname ?? '',
    gender: info?.gender ?? '',
    avatarUrl: info?.avatarUrl ?? '',
    description: info?.description ?? '',
  }
}

/**
 * 选择头像：只记下文件并本地预览，不上传（提交时才传，避免孤儿文件）。
 *
 * @param event 选择框的 change 事件
 */
function onPickAvatar(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  if (file.size > FILE_MAX_SIZE_BYTE) {
    ElMessage.warning(FILE_MAX_SIZE_MESSAGE)
    return
  }
  clearAvatarFile()
  avatarFile.value = file
  avatarPreviewUrl.value = URL.createObjectURL(file)
}

/**
 * 清掉选中的头像文件与预览地址。
 */
function clearAvatarFile(): void {
  if (avatarPreviewUrl.value.length > 0) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
  }
  avatarPreviewUrl.value = ''
  avatarFile.value = null
}

/**
 * 取消编辑：连同没提交的头像一起丢掉。
 */
function cancelEdit(): void {
  clearAvatarFile()
  editing.value = false
}

/**
 * 移除头像：清掉刚选的文件，并把资料里的文件 ID 也清空（展示退回昵称首字）。
 */
function onRemoveAvatar(): void {
  clearAvatarFile()
  form.value.avatarUrl = ''
}

/**
 * 放大查看头像：没有设置头像（显示的是昵称首字）时不弹。
 */
function openAvatarPreview(): void {
  if (avatarDisplay.value.length > 0) {
    avatarPreviewVisible.value = true
  }
}

/**
 * 开始编辑：先把当前资料填进表单。
 */
function startEdit(): void {
  fillForm()
  clearAvatarFile()
  editing.value = true
}

/**
 * 保存资料。
 */
async function onSave(): Promise<void> {
  const nickname = form.value.nickname.trim()
  if (nickname.length === 0) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    // 头像到提交时才上传：用户取消编辑就不会留下没人引用的文件
    let avatarUrlValue = form.value.avatarUrl.trim()
    if (avatarFile.value) {
      const uploaded = await uploadBroadcastFile(avatarFile.value)
      avatarUrlValue = uploaded.objectKey
    }
    const userinfo: BroadcastUserinfo = {
      nickname,
      gender: form.value.gender.trim(),
      avatarUrl: avatarUrlValue,
      description: form.value.description.trim(),
    }
    await broadcastStore.saveProfile(uid.value, userinfo)
    clearAvatarFile()
    form.value.avatarUrl = avatarUrlValue
    editing.value = false
    ElMessage.success('已保存')
  } catch (error) {
    showError(error, '保存失败')
  } finally {
    saving.value = false
  }
}

/**
 * 加载更多帖子。
 */
async function onLoadMore(): Promise<void> {
  try {
    await broadcastStore.loadUserPosts(uid.value, false)
  } catch (error) {
    showError(error, '加载更多失败')
  }
}

/**
 * 打开帖子详情。
 *
 * @param id 帖子消息 ID
 */
function openPost(id: string): void {
  // 记下点开的是哪条、属于谁的主页，返回时把它放回中间
  broadcastStore.profileAnchorUid = uid.value
  broadcastStore.profileAnchorId = id
  void router.push({ name: 'broadcast-post', params: { id } })
}

/**
 * 打开另一个用户的主页。
 *
 * @param value  用户公钥
 * @param itemId 这条内容的 ID，回来时用它定位
 */
function openUser(value: string, itemId: string): void {
  broadcastStore.profileAnchorUid = uid.value
  broadcastStore.profileAnchorId = itemId
  if (value === uid.value) {
    return
  }
  void router.push({ name: 'broadcast-profile', params: { uid: value } })
}
</script>

<template>
  <section class="profile">
    <header class="profile__header">
      <el-button
        :icon="ArrowLeft"
        circle
        size="small"
        title="返回"
        @click="onBack"
      />
      <span class="profile__title">用户主页</span>
    </header>

    <div
      ref="listEl"
      class="profile__body scroll-y"
      @wheel.passive="onUserScroll"
      @touchstart.passive="onUserScroll"
    >
      <div ref="contentEl">
      <div class="profile__card">
        <el-avatar
          :size="56"
          class="profile__avatar"
          :class="{ 'profile__avatar--zoom': avatarDisplay.length > 0 }"
          :src="avatarDisplay || undefined"
          title="点击放大"
          @click="openAvatarPreview"
        >
          {{ displayName.charAt(0) }}
        </el-avatar>

        <div class="profile__main">
          <div class="profile__name">{{ displayName }}</div>
          <div v-if="profile?.gender" class="profile__meta">{{ profile.gender }}</div>
          <div class="profile__key">
            <span class="profile__key-text" :title="uid">{{ uid }}</span>
            <el-button
              :icon="DocumentCopy"
              text
              size="small"
              title="复制公钥"
              @click="onCopyKey"
            />
          </div>
          <div class="profile__desc">
            {{ profile?.description || '这个人很懒，还没有写简介' }}
          </div>
        </div>

        <el-button v-if="isSelf && !editing" :icon="Edit" size="small" @click="startEdit">
          编辑资料
        </el-button>
      </div>

      <div v-if="editing" class="profile__form">
        <el-form label-width="72px">
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" maxlength="32" placeholder="展示用的昵称" />
          </el-form-item>
          <el-form-item label="性别">
            <el-input v-model="form.gender" maxlength="16" placeholder="如：男 / 女 / 保密" />
          </el-form-item>
          <el-form-item label="头像">
            <div class="profile__avatar-row">
              <el-avatar :size="40" class="profile__avatar" :src="avatarDisplay || undefined">
                {{ displayName.charAt(0) }}
              </el-avatar>
              <el-button :icon="Picture" @click="avatarInput?.click()">选择图片</el-button>
              <el-button v-if="avatarDisplay" @click="onRemoveAvatar">移除</el-button>
            </div>
            <div class="profile__hint">
              头像只走广播文件通道：选本地图片，保存时才会上传，取消编辑不会留下文件；移除后显示昵称首字。
            </div>
          </el-form-item>
          <el-form-item label="简介">
            <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
            <el-button @click="cancelEdit">取消</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="profile__section">{{ isSelf ? '我的帖子' : 'TA 的帖子' }}</div>

      <el-skeleton v-if="broadcastStore.userPostsLoading && broadcastStore.userPosts.length === 0" :rows="4" animated />

      <el-empty
        v-else-if="broadcastStore.userPosts.length === 0"
        description="还没有发过帖子"
        :image-size="60"
      />

      <template v-else>
        <BroadcastPostCard
          v-for="item in broadcastStore.userPosts"
          :key="item.id"
          :item="item"
          clickable
          @open="openPost(item.id)"
          @open-user="openUser"
          @reply="openPost(item.id)"
        />

        <div class="profile__more">
          <el-button
            v-if="broadcastStore.userPostsHasMore"
            size="small"
            :loading="broadcastStore.userPostsLoading"
            @click="onLoadMore"
          >
            加载更多
          </el-button>
          <span v-else class="profile__end">没有更多了</span>
        </div>
      </template>
      </div>
    </div>

    <input
      ref="avatarInput"
      class="profile__input"
      type="file"
      accept="image/*"
      @change="onPickAvatar"
    />

    <!-- 头像放大查看 -->
    <el-dialog v-model="avatarPreviewVisible" width="60%" align-center>
      <img
        v-if="avatarDisplay"
        class="profile__avatar-preview"
        :src="avatarDisplay"
        :alt="displayName"
      />
    </el-dialog>
  </section>
</template>

<style scoped>
.profile {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  background: #f5f5f5;
}

.profile__header {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.profile__title {
  font-size: 15px;
  font-weight: 600;
}

.profile__body {
  flex: 1;
  min-height: 0;
  background: #fff;
}

.profile__card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
}

.profile__avatar {
  flex: none;
  background: #07c160;
  color: #fff;
  font-size: 20px;
}

.profile__avatar--zoom {
  cursor: zoom-in;
}

.profile__avatar-preview {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  margin: 0 auto;
}

.profile__main {
  flex: 1;
  min-width: 0;
}

.profile__name {
  font-size: 16px;
  font-weight: 600;
}

.profile__meta {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.profile__key {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
}

.profile__key-text {
  overflow: hidden;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  color: #a8abb2;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.profile__desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
  word-break: break-word;
}

.profile__form {
  padding: 8px 16px 16px;
  border-top: 1px solid #f0f2f5;
}

.profile__avatar-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.profile__hint {
  width: 100%;
  font-size: 12px;
  line-height: 1.6;
  color: #909399;
}

.profile__input {
  display: none;
}

.profile__section {
  padding: 10px 16px;
  border-top: 8px solid #f5f5f5;
  border-bottom: 1px solid #f0f2f5;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.profile__more {
  padding: 14px 0 20px;
  text-align: center;
}

.profile__end {
  font-size: 12px;
  color: #a8abb2;
}
</style>
