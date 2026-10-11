<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import type { BroadcastFileVo } from '@/api'
import BroadcastComposer from '@/components/BroadcastComposer.vue'
import { useBroadcastStore } from '@/stores/broadcast'
import { showError } from '@/utils/feedback'

/**
 * 发帖页。
 *
 * <p>单独一页而不是弹窗：头部与内容区的样式和列表、详情保持一致。输入区不占满整屏，随内容自己长高
 * （超过上限内部滚动），附件预览就是发布后的排版。</p>
 */

const router = useRouter()
const broadcastStore = useBroadcastStore()

/** 发布框。 */
const composer = ref<InstanceType<typeof BroadcastComposer>>()

/** 是否正在发布。 */
const publishing = ref(false)

/**
 * 返回到时间线。
 */
function onBack(): void {
  void router.push({ name: 'broadcast-feed' })
}

/**
 * 发布帖子：成功后回到时间线，那边会重新加载，新帖就在最前面。
 *
 * @param text  正文
 * @param files 附件
 */
async function onPublish(text: string, files: BroadcastFileVo[]): Promise<void> {
  publishing.value = true
  try {
    await broadcastStore.createPost(text, files)
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
      <span class="create__title">发帖</span>
    </header>

    <div class="create__body">
      <BroadcastComposer
        ref="composer"
        autosize
        placeholder="分享点什么…"
        submit-text="发布"
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
</style>
