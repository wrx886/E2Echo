<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, DocumentCopy, Edit, Picture, Refresh } from '@element-plus/icons-vue'
import {
  FILE_MAX_SIZE_BYTE,
  FILE_MAX_SIZE_MESSAGE,
  avatarSrc,
  getBroadcastSubscribe,
  uploadBroadcastFile,
} from '@/api'
import type { BroadcastSubscribeDto, BroadcastUserinfo } from '@/api'
import BroadcastPostCard from '@/components/BroadcastPostCard.vue'
import ImagePreviewDialog from '@/components/ImagePreviewDialog.vue'
import { useBroadcastStore } from '@/stores/broadcast'
import { useSubscribeStore } from '@/stores/subscribe'
import { useUserStore } from '@/stores/user'
import { useAnchorScroll } from '@/utils/anchorScroll'
import { copyText } from '@/utils/clipboard'
import { shortKey } from '@/utils/display'
import { showError } from '@/utils/feedback'

/**
 * 用户主页：昵称、头像、简介，以及 TA 的帖子；看自己的主页时还能编辑资料。
 *
 * <p>别人的主页上还能订阅 TA（订阅只存在 client 本地库，别名只在本机展示），自己的主页上能编辑
 * 资料。头像也走广播文件通道：选图时只本地预览，到提交时才上传、把拿到的文件 ID 存进资料里——中途
 * 取消就不会留下没人引用的孤儿文件。</p>
 */

const route = useRoute()
const router = useRouter()
const broadcastStore = useBroadcastStore()
const subscribeStore = useSubscribeStore()
const userStore = useUserStore()

/** 主页所属的用户公钥。 */
const uid = computed(() => String(route.params.uid ?? ''))

/** 是不是自己的主页。 */
const isSelf = computed(() => uid.value.length > 0 && uid.value === userStore.publicKey)

/** 资料。 */
const profile = computed(() => broadcastStore.profileOf(uid.value))

/** 当前用户对 TA 的订阅，null 表示没有订阅。 */
const subscribe = ref<BroadcastSubscribeDto | null>(null)

/** 是否正在查询或修改订阅。 */
const subscribeBusy = ref(false)

/** 订阅状态是否已经查完：查之前先不给点，免得把"还没查"显示成"没订阅"。 */
const subscribeReady = ref(false)

/** TA 的本地别名（订阅别名），没有就是空串。 */
const subscribeAlias = computed(() => subscribe.value?.alias?.trim() ?? '')

/** 资料里的昵称。 */
const nickname = computed(() => profile.value?.nickname ?? '')

/**
 * 展示名：订阅别名优先（本机给 TA 起的名字，和时间线、订阅流里保持一致），其次资料昵称，最后公钥
 * 末几位。
 */
const displayName = computed(() =>
  subscribeAlias.value || nickname.value || shortKey(uid.value, 5))

/** 展示的是别名、而且和对方自己的昵称不一样时，额外汇总一行昵称，免得看不出他自称什么。 */
const showNickname = computed(() =>
  subscribeAlias.value.length > 0 && nickname.value.length > 0 && nickname.value !== subscribeAlias.value)

/** 是否正在编辑资料。 */
const editing = ref(false)

/** 是否正在刷新（刷新按钮的 loading）。 */
const refreshing = ref(false)

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

const anchor = useAnchorScroll({
  container: listEl,
  content: contentEl,
  anchorId: pendingAnchor,
})

watch(uid, async (value) => {
  if (value.length === 0) {
    return
  }
  editing.value = false
  subscribe.value = null
  subscribeReady.value = false
  // 换个人看时把没提交的头像丢掉，免得误传到别人名下
  clearAvatarFile()
  try {
    await broadcastStore.loadProfile(value, true)
    fillForm()
    await broadcastStore.loadUserPosts(value, true)
  } catch (error) {
    showError(error, '加载主页失败')
  }
  // 订阅状态单独查：查询失败也要把按钮放出来（真的重复订阅时后端会提示），不能因为这一步卡死按钮
  if (value !== userStore.publicKey) {
    try {
      subscribe.value = await getBroadcastSubscribe(value)
    } catch (error) {
      showError(error, '查询订阅状态失败')
    }
  }
  subscribeReady.value = true
  // 把上次从主页点开的那条帖子放回可视区中间（换人看时锚点不套用）；取走即清，避免重复跳
  pendingAnchor.value = broadcastStore.consumeProfileAnchor(value)
  await anchor.start()
}, { immediate: true })

watch(() => userStore.publicKey, () => {
  // 公钥是异步取到的：先打开自己的主页时补一次
  if (isSelf.value && profile.value === null) {
    void broadcastStore.loadProfile(uid.value, true).then(fillForm).catch(() => {})
  }
}, { immediate: true })

onUnmounted(() => {
  clearAvatarFile()
})

/**
 * 用户自己滚动：不要再把视图拽回锚点。
 */
function onUserScroll(): void {
  anchor.stop()
}

/**
 * 刷新：资料和 TA 的帖子都按第一次打开那样重新拉一遍，然后回到顶部。
 */
async function onRefresh(): Promise<void> {
  const value = uid.value
  if (value.length === 0) {
    return
  }
  refreshing.value = true
  try {
    await broadcastStore.loadProfile(value, true)
    fillForm()
    await broadcastStore.loadUserPosts(value, true)
    anchor.reset()
  } catch (error) {
    showError(error, '刷新失败')
  } finally {
    refreshing.value = false
  }
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
 * 订阅 TA。
 */
async function onSubscribe(): Promise<void> {
  if (subscribeBusy.value || uid.value.length === 0) {
    return
  }
  subscribeBusy.value = true
  try {
    await subscribeStore.subscribe(uid.value)
    subscribe.value = await getBroadcastSubscribe(uid.value)
    ElMessage.success('已订阅，可以在「订阅」里看 TA 的帖子')
  } catch (error) {
    showError(error, '订阅失败')
  } finally {
    subscribeBusy.value = false
  }
}

/**
 * 修改订阅别名：留空表示清除。
 */
async function onEditSubscribeAlias(): Promise<void> {
  if (subscribeBusy.value || subscribe.value === null) {
    return
  }
  let value = ''
  try {
    const result = await ElMessageBox.prompt(
      '别名只在本机展示，订阅流、时间线和 TA 的主页都会优先显示这个名字；留空表示清除。',
      `给「${displayName.value}」起个别名`,
      {
        inputValue: subscribe.value?.alias ?? '',
        inputPlaceholder: '留空表示清除别名',
        confirmButtonText: '保存',
        cancelButtonText: '取消',
      },
    )
    value = result.value ?? ''
  } catch {
    // 取消或关闭弹窗，什么都不做
    return
  }
  subscribeBusy.value = true
  try {
    await subscribeStore.setAlias(uid.value, value)
    subscribe.value = await getBroadcastSubscribe(uid.value)
    ElMessage.success(value.trim().length > 0 ? '已保存别名' : '已清除别名')
  } catch (error) {
    showError(error, '保存别名失败')
  } finally {
    subscribeBusy.value = false
  }
}

/**
 * 取消订阅 TA。
 */
async function onUnsubscribe(): Promise<void> {
  if (subscribeBusy.value || subscribe.value === null) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `取消订阅后，「${displayName.value}」的帖子不再出现在订阅流里，别名也会一起删掉。`,
      '取消订阅',
      { type: 'warning', confirmButtonText: '取消订阅', cancelButtonText: '再想想' },
    )
  } catch {
    return
  }
  subscribeBusy.value = true
  try {
    await subscribeStore.unsubscribe(uid.value)
    subscribe.value = null
    ElMessage.success('已取消订阅')
  } catch (error) {
    showError(error, '取消订阅失败')
  } finally {
    subscribeBusy.value = false
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
      <el-button
        class="profile__refresh"
        :icon="Refresh"
        circle
        size="small"
        title="刷新"
        :loading="refreshing"
        @click="onRefresh"
      />
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
          <div class="profile__name-row">
            <span class="profile__name">{{ displayName }}</span>
            <!-- 别名只在本机展示，入口就放在名字旁边 -->
            <el-button
              v-if="!isSelf && subscribe"
              class="profile__alias-edit"
              :icon="Edit"
              text
              circle
              size="small"
              title="设置别名"
              @click="onEditSubscribeAlias"
            />
          </div>
          <div v-if="showNickname" class="profile__meta">昵称：{{ nickname }}</div>
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

        <!-- 别人的主页：这个按钮只管订阅与取消订阅，改别名在名字旁边的编辑图标上 -->
        <el-button
          v-else-if="!isSelf && subscribe"
          size="small"
          type="success"
          plain
          :loading="subscribeBusy"
          @click="onUnsubscribe"
        >
          已订阅
        </el-button>
        <el-button
          v-else-if="!isSelf"
          size="small"
          type="primary"
          :loading="subscribeBusy || !subscribeReady"
          :disabled="!subscribeReady"
          @click="onSubscribe"
        >
          订阅
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

    <!-- 头像放大查看：和聊天、广播附件的图片共用同一个查看器 -->
    <ImagePreviewDialog
      v-model:visible="avatarPreviewVisible"
      :src="avatarDisplay"
      :title="displayName + ' 的头像'"
    />
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

.profile__refresh {
  margin-left: auto;
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

.profile__main {
  flex: 1;
  min-width: 0;
}

.profile__name-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.profile__name {
  font-size: 16px;
  font-weight: 600;
}

.profile__alias-edit {
  flex: none;
  color: #909399;
}

.profile__alias-edit:hover {
  color: #409eff;
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
