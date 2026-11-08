<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { BROADCAST_POST_TO } from '@/api'
import type { BroadcastFileVo, BroadcastMessageRespVo } from '@/api'
import BroadcastComposer from '@/components/BroadcastComposer.vue'
import { useBroadcastStore } from '@/stores/broadcast'
import { useSubscribeStore } from '@/stores/subscribe'
import { shortKey } from '@/utils/display'
import { showError } from '@/utils/feedback'

/**
 * 发帖页。
 *
 * <p>单独一页而不是弹窗：头部与内容区的样式和列表、详情保持一致。输入区不占满整屏，随内容自己长高
 * （超过上限内部滚动），附件预览就是发布后的排版。</p>
 */

const route = useRoute()
const router = useRouter()
const broadcastStore = useBroadcastStore()
const subscribeStore = useSubscribeStore()

/** 被转发广播的 ID，由发帖页 URL 传入。 */
const forwardId = computed(() => String(route.query.forward ?? ''))

/** 是否处于转发模式：转发必定创建一级帖子。 */
const forwarding = computed(() => forwardId.value.length > 0)

/** 被转发的原广播。 */
const forwardTarget = ref<BroadcastMessageRespVo | null>(null)

/** 原广播是否正在加载。 */
const forwardLoading = ref(false)

/** 原广播是否不存在或读取失败。 */
const forwardMissing = ref(false)

/** 被转发广播的作者展示名。 */
const forwardName = computed(() => {
  const target = forwardTarget.value
  if (!target) {
    return ''
  }
  return subscribeStore.aliasOf(target.from)
    || broadcastStore.profileOf(target.from)?.nickname
    || shortKey(target.from, 5)
})

/** 发布框。 */
const composer = ref<InstanceType<typeof BroadcastComposer>>()

/** 是否正在发布。 */
const publishing = ref(false)

watch(forwardId, async (id) => {
  forwardTarget.value = null
  forwardMissing.value = false
  if (!id) {
    return
  }
  forwardLoading.value = true
  try {
    const target = await broadcastStore.findMessage(id)
    const post = target?.to === BROADCAST_POST_TO ? target : null
    forwardTarget.value = post
    forwardMissing.value = post === null
    if (post) {
      void broadcastStore.loadProfile(post.from).catch(() => {})
    }
  } catch {
    forwardMissing.value = true
  } finally {
    forwardLoading.value = false
  }
}, { immediate: true })

/**
 * 返回到时间线。
 */
function onBack(): void {
  if (window.history.state?.back) {
    router.back()
    return
  }
  void router.push({ name: 'broadcast-feed' })
}

/**
 * 发布帖子：成功后回到时间线，那边会重新加载，新帖就在最前面。
 *
 * @param text  正文
 * @param files 附件
 */
async function onPublish(text: string, files: BroadcastFileVo[]): Promise<void> {
  if (forwarding.value && !forwardTarget.value) {
    ElMessage.warning('被转发的广播不可用，暂时不能转发')
    return
  }
  publishing.value = true
  try {
    await broadcastStore.createPost(text, files, forwardTarget.value?.id)
    composer.value?.clear()
    void router.push({ name: 'broadcast-feed' })
  } catch (error) {
    showError(error, '发布失败')
  } finally {
    publishing.value = false
  }
}
</script>

<template>
  <section class="create">
    <header class="create__header">
      <el-button :icon="ArrowLeft" circle size="small" title="返回" @click="onBack" />
      <span class="create__title">{{ forwarding ? '转发' : '发帖' }}</span>
    </header>

    <div class="create__body">
      <div v-if="forwarding" class="create__forward">
        <template v-if="forwardLoading">正在加载被转发的广播…</template>
        <template v-else-if="forwardMissing">被转发的帖子不存在、不是一级帖子或暂时无法读取</template>
        <template v-else-if="forwardTarget">
          <div class="create__forward-head">转发 @{{ forwardName }}</div>
          <div class="create__forward-text">{{ forwardTarget.message?.text }}</div>
        </template>
      </div>

      <BroadcastComposer
        ref="composer"
        autosize
        :placeholder="forwarding ? '说说转发理由…' : '分享点什么…'"
        :submit-text="forwarding ? '转发' : '发布'"
        :submitting="publishing"
        @submit="onPublish"
      />
    </div>
  </section>
</template>

<style scoped>
.create {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  background: #f5f5f5;
}

.create__header {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.create__title {
  font-size: 15px;
  font-weight: 600;
}

.create__body {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  background: #fff;
}

.create__forward {
  margin: 12px 16px 0;
  padding: 10px 12px;
  border-left: 3px solid #a0cfff;
  border-radius: 4px;
  background: #f5f7fa;
  font-size: 13px;
  line-height: 1.6;
  color: #909399;
}

.create__forward-head {
  margin-bottom: 3px;
  color: #409eff;
}

.create__forward-text {
  overflow: hidden;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
