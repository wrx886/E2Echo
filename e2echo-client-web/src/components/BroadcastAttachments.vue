<script setup lang="ts">
import { computed, ref } from 'vue'
import { Close, Document, Headset } from '@element-plus/icons-vue'
import { broadcastFileUrl } from '@/api'
import type { BroadcastFileVo } from '@/api'
import ImagePreviewDialog from '@/components/ImagePreviewDialog.vue'
import { safeFileName } from '@/utils/filename'

/**
 * 展示用的附件：比上传结果多一个本地预览地址。
 *
 * <p>发帖/评论时附件是提交才上传的，选好之后还没有对象键，先用本地地址预览——排版和发布后完全一致。</p>
 */
export interface AttachmentItem extends BroadcastFileVo {
  /** 本地预览地址：还没上传的文件用它代替广播文件地址。 */
  localUrl?: string
}

/**
 * 广播消息的附件。
 *
 * <p>图片单独成一块并按张数排版：单张按大图展示，两张与四张分两列，其余按三列铺（一行三张，放不下
 * 就换行），像常见的九宫格。视频、音频、文件不做网格，各自纵向占一行。附件都不落本地，地址就是
 * 下载接口的地址。</p>
 *
 * <p>发帖页把 {@code removable} 打开、复用同一个组件做预览：预览看到的就是发布后的样子，只是每个
 * 附件多一个移除按钮。</p>
 */

const props = defineProps<{
  /** 附件列表。 */
  files?: AttachmentItem[] | null
  /** 是否显示移除按钮（发帖/评论时用）。 */
  removable?: boolean
}>()

const emit = defineEmits<{
  /** 移除某个附件。 */
  'remove': [file: AttachmentItem]
}>()

/** 正在放大查看的图片地址。 */
const previewUrl = ref('')

/** 正在放大查看的图片文件名，用作标题与下载名。 */
const previewTitle = ref('')

/** 是否显示图片放大弹窗。 */
const previewVisible = ref(false)

/** 附件列表：上游可能没有这个字段（别的客户端发的消息），统一按空列表处理。 */
const list = computed(() => props.files ?? [])

/** 图片附件：单独排成一块。 */
const images = computed(() => list.value.filter((file) => isImage(file)))

/** 其他附件：纵向各占一行。 */
const others = computed(() => list.value.filter((file) => !isImage(file)))

/**
 * 图片网格的列数：两张、四张分两列（四张就是 2×2），其余按三列铺。
 */
const imageColumns = computed(() => {
  const count = images.value.length
  return count === 2 || count === 4 ? 2 : 3
})

/**
 * 判断附件类型。
 *
 * @param file 附件
 * @returns 类型判断结果
 */
function isImage(file: BroadcastFileVo): boolean {
  return file.contentType.startsWith('image/')
}

function isVideo(file: BroadcastFileVo): boolean {
  return file.contentType.startsWith('video/')
}

function isAudio(file: BroadcastFileVo): boolean {
  return file.contentType.startsWith('audio/')
}

/**
 * 取附件地址：本地还没上传的用本地预览地址，已经上传的用广播文件地址。
 *
 * @param file 附件
 * @returns 可以直接给标签用的地址
 */
function urlOf(file: AttachmentItem): string {
  return file.localUrl ?? broadcastFileUrl(file)
}

/**
 * 放大查看图片。
 *
 * @param file 图片附件
 */
function openPreview(file: AttachmentItem): void {
  previewUrl.value = urlOf(file)
  previewTitle.value = file.filename
  previewVisible.value = true
}

/**
 * 把字节数转成可读的大小。
 *
 * @param size 字节数
 * @returns 展示用的大小文本
 */
function sizeText(size: number): string {
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>

<template>
  <div v-if="list.length > 0" class="attachments">
    <!-- 单张图片：按大图展示 -->
    <div v-if="images.length === 1 && images[0]" class="attachments__item">
      <img
        class="attachments__image"
        :src="urlOf(images[0])"
        :alt="images[0].filename"
        @click.stop="openPreview(images[0])"
      />
      <button
        v-if="removable"
        type="button"
        class="attachments__remove"
        title="移除"
        @click.stop="emit('remove', images[0])"
      >
        <el-icon :size="12"><Close /></el-icon>
      </button>
    </div>

    <!-- 多张图片：按张数分列排成网格 -->
    <div
      v-else-if="images.length > 1"
      class="attachments__grid"
      :style="{ gridTemplateColumns: `repeat(${imageColumns}, 1fr)` }"
    >
      <div v-for="file in images" :key="urlOf(file)" class="attachments__item">
        <img
          class="attachments__image-tile"
          :src="urlOf(file)"
          :alt="file.filename"
          @click.stop="openPreview(file)"
        />
        <button
          v-if="removable"
          type="button"
          class="attachments__remove"
          title="移除"
          @click.stop="emit('remove', file)"
        >
          <el-icon :size="12"><Close /></el-icon>
        </button>
      </div>
    </div>

    <!-- 视频、音频、文件：纵向各占一行 -->
    <div v-for="file in others" :key="urlOf(file)" class="attachments__item">
      <video
        v-if="isVideo(file)"
        class="attachments__video"
        :src="urlOf(file)"
        controls
        preload="metadata"
        @click.stop
      />

      <div v-else-if="isAudio(file)" class="attachments__audio">
        <div class="attachments__audio-head">
          <el-icon :size="16"><Headset /></el-icon>
          <span class="attachments__name" :title="file.filename">{{ file.filename }}</span>
        </div>
        <audio
          class="attachments__audio-player"
          :src="urlOf(file)"
          controls
          preload="metadata"
          @click.stop
        />
      </div>

      <div v-else class="attachments__file">
        <el-icon class="attachments__file-icon" :size="20"><Document /></el-icon>
        <div class="attachments__file-main">
          <div class="attachments__name" :title="file.filename">{{ file.filename }}</div>
          <div class="attachments__size">{{ sizeText(file.size) }}</div>
        </div>
        <a
          class="attachments__download"
          :href="urlOf(file)"
          :download="safeFileName(file.filename)"
          @click.stop
        >
          下载
        </a>
      </div>

      <button
        v-if="removable"
        type="button"
        class="attachments__remove"
        title="移除"
        @click.stop="emit('remove', file)"
      >
        <el-icon :size="12"><Close /></el-icon>
      </button>
    </div>

    <!-- 放大看原图：和聊天里的图片消息共用同一个查看器（自带下载/缩放/旋转/复位） -->
    <ImagePreviewDialog
      v-model:visible="previewVisible"
      :src="previewUrl"
      :title="previewTitle"
    />
  </div>
</template>

<style scoped>
.attachments {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  margin-top: 8px;
}

.attachments__item {
  position: relative;
  width: fit-content;
  max-width: 100%;
}

/* 图片网格：宽度固定，按列数均分；窄窗口下收缩 */
.attachments__grid {
  display: grid;
  gap: 4px;
  width: 320px;
  max-width: 100%;
}

.attachments__grid .attachments__item {
  width: 100%;
}

.attachments__remove {
  position: absolute;
  top: 4px;
  right: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  cursor: pointer;
}

.attachments__remove:hover {
  background: rgba(0, 0, 0, 0.7);
}

.attachments__image {
  display: block;
  max-width: min(320px, 100%);
  max-height: 320px;
  border-radius: 4px;
  cursor: zoom-in;
}

/* 网格里的图片：正方形、居中裁切 */
.attachments__image-tile {
  display: block;
  width: 100%;
  aspect-ratio: 1;
  border-radius: 4px;
  object-fit: cover;
  cursor: zoom-in;
}

.attachments__video {
  display: block;
  max-width: min(320px, 100%);
  max-height: 320px;
  border-radius: 4px;
  background: #000;
}

.attachments__audio {
  width: fit-content;
  max-width: 100%;
}

.attachments__audio-head {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #409eff;
}

.attachments__audio-player {
  display: block;
  width: 360px;
  max-width: 100%;
  margin-top: 6px;
}

.attachments__file {
  display: flex;
  align-items: center;
  gap: 10px;
  width: fit-content;
  max-width: 100%;
  padding: 8px 12px;
  border-radius: 4px;
  background: #f5f7fa;
}

.attachments__file-icon {
  flex: none;
  color: #409eff;
}

.attachments__file-main {
  min-width: 0;
}

.attachments__name {
  overflow: hidden;
  font-size: 13px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.attachments__size {
  font-size: 12px;
  color: #909399;
}

.attachments__download {
  flex: none;
  padding: 2px 10px;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  line-height: 20px;
  text-decoration: none;
}

</style>
