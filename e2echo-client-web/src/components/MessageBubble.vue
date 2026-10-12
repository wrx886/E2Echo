<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Document, Download } from '@element-plus/icons-vue'
import {
  MESSAGE_TYPE_CHAT_FILE,
  MESSAGE_TYPE_CHAT_FILE_AUDIO,
  MESSAGE_TYPE_CHAT_FILE_IMAGE,
  MESSAGE_TYPE_CHAT_FILE_VIDEO,
  audioMessageOf,
  chatTextOf,
  chatFileUrl,
  fileMessageOf,
  groupKeyOf,
  imageMessageOf,
  isTrustedGroupKey,
  videoMessageOf,
} from '@/api'
import type { MessageVo } from '@/api'
import AudioMessage from '@/components/AudioMessage.vue'
import ChatUserCard from '@/components/ChatUserCard.vue'
import ImageMessage from '@/components/ImageMessage.vue'
import GroupKeyMessage from '@/components/GroupKeyMessage.vue'
import VideoMessage from '@/components/VideoMessage.vue'
import { safeFileName } from '@/utils/filename'
import { shortKey } from '@/utils/display'
import { useConversationStore } from '@/stores/conversation'

/**
 * 单条消息气泡。
 *
 * <p>自己发的靠右、绿色气泡；对方发的靠左、白色气泡，群聊里在气泡上方标出发送者（没有用户资料
 * 接口，只能取公钥末几位）。</p>
 */

const props = defineProps<{
  /** 消息。 */
  message: MessageVo
  /** 是否是自己发的。 */
  mine: boolean
  /** 是否群聊会话。 */
  group: boolean
  /** 自己的头像文字；为空时按发送者公钥算。 */
  mineAvatar?: string
  /**
   * 用户小卡片是否开在这条消息上。
   *
   * <p>整页同一时刻只有一张卡片，开在哪条消息上由聊天页决定：只有它的气泡会创建 el-popover。</p>
   */
  cardOpen?: boolean
}>()

const emit = defineEmits<{
  /** 点了对方的头像，请求把用户卡片开在这条消息上。 */
  'avatarClick': []
  /** 用户卡片自己关掉了（点外部、按 Esc 或点了卡片里的关闭）。 */
  'cardClose': []
}>()

const conversationStore = useConversationStore()

/**
 * 对方的展示名称：优先用会话别名，没有对应会话时退回公钥末 6 位。
 *
 * <p>别名存在会话记录上，和会话列表、会话设置、群成员列表用的是同一份数据，保证同一个人在哪都是
 * 同一个名字。</p>
 */
const senderName = computed(() =>
  conversationStore.aliasOf(props.message.from) ?? shortKey(props.message.from))

// 别名没查到就补一次查询（会话列表里没有这个人时会单独请求），取到后上面的 computed 会自动换成别名
watch(() => props.message.from, (from) => {
  if (!props.mine && from.length > 0 && !conversationStore.aliasOf(from)) {
    void conversationStore.resolveAlias(from)
  }
}, { immediate: true })

/** 已创建出来的用户小卡片是否显示，挂上 el-popover 即为打开。 */
const cardVisible = ref(false)

// 聊天页把卡片指到这条消息时，挂上来的 popover 要直接是打开状态，用户不用再点一次头像
watch(() => props.cardOpen === true, (open) => {
  if (open) {
    cardVisible.value = true
  }
}, { immediate: true })

// 任何方式关掉都要告诉聊天页清掉「卡片开在哪条消息上」，否则 v-if 不会卸载 popover
watch(cardVisible, (value) => {
  if (!value) {
    emit('cardClose')
  }
})

/** 文字正文，非文字消息为空。 */
const text = computed(() => chatTextOf(props.message))

/** 群聊密钥消息的正文，不是这类消息时为空。 */
const groupKey = computed(() => groupKeyOf(props.message))

/** 文件消息的正文，不是这类消息时为空。 */
const fileBody = computed(() => fileMessageOf(props.message))

/** 图片消息的正文，不是这类消息时为空。 */
const imageBody = computed(() => imageMessageOf(props.message))

/** 视频消息的正文，不是这类消息时为空。 */
const videoBody = computed(() => videoMessageOf(props.message))

/** 音频消息的正文，不是这类消息时为空。 */
const audioBody = computed(() => audioMessageOf(props.message))

/**
 * 是文件类消息（文件、图片）但正文解析不出来时给的提示，不是这类消息时为空。
 */
const attachmentBroken = computed(() => {
  if (imageBody.value !== null || fileBody.value !== null) {
    return ''
  }
  if (props.message.type === MESSAGE_TYPE_CHAT_FILE_IMAGE) {
    return '[图片消息异常]'
  }
  if (props.message.type === MESSAGE_TYPE_CHAT_FILE_VIDEO) {
    return '[视频消息异常]'
  }
  if (props.message.type === MESSAGE_TYPE_CHAT_FILE_AUDIO) {
    return '[音频消息异常]'
  }
  if (props.message.type === MESSAGE_TYPE_CHAT_FILE) {
    return '[文件消息异常]'
  }
  return ''
})

/**
 * 群聊密钥消息的来源是否可信。
 *
 * <p>判定规则见 {@link isTrustedGroupKey}。这里之所以要再判一次：client 拒收一条密钥消息时它已经
 * 落库了，伪造的消息仍然会出现在聊天记录里，界面上得能看出来。</p>
 */
const keyTrusted = computed(() => {
  const key = groupKey.value
  return key !== null && isTrustedGroupKey(key.group, props.message.from, props.message.channel)
})

/** 头像文字：自己的消息用自己的名称，对方的用对方名称首字。 */
const avatar = computed(() =>
  props.mine && props.mineAvatar ? props.mineAvatar : senderName.value.slice(0, 1))

/**
 * 点头像：请求聊天页把用户卡片开在这条消息上。
 *
 * <p>自己的头像没有卡片可看，直接忽略。</p>
 */
function onAvatarClick(): void {
  if (!props.mine) {
    emit('avatarClick')
  }
}

/**
 * 下载文件消息：直接用文件接口的地址触发一次浏览器下载。
 */
function onDownloadFile(): void {
  const file = fileBody.value
  if (!file) {
    return
  }
  const anchor = document.createElement('a')
  anchor.href = chatFileUrl(file)
  anchor.download = safeFileName(file.filename)
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
}
</script>

<template>
  <div class="bubble-row" :class="mine ? 'bubble-row--mine' : 'bubble-row--other'">
    <!--
      点对方头像弹出用户卡片：看别名与公钥，并按有没有会话给出对应操作。

      这里用 v-if 把 el-popover 限定在「卡片正开在这条消息上」时才创建：el-popover 挂的是
      ElTooltip + 触发器 + 定位 + FocusTrap 一整套组件，一条消息一套的话，切换会话要卸载再挂载
      整个消息列表，200 条消息就能在主线程上卡出 600ms 的长任务（实测 200 条约 700ms）。
      整页同时最多只会有一张卡片，所以只会多出一个 popover。
    -->
    <el-popover
      v-if="cardOpen === true"
      v-model:visible="cardVisible"
      :width="300"
      placement="top-start"
      trigger="click"
    >
      <template #reference>
        <el-avatar
          :size="32"
          class="bubble-row__avatar"
          :class="{ 'bubble-row__avatar--mine': mine }"
          :title="mine ? '' : '查看对方信息'"
        >
          {{ avatar }}
        </el-avatar>
      </template>

      <ChatUserCard :uid="message.from" @close="cardVisible = false" />
    </el-popover>

    <el-avatar
      v-else
      :size="32"
      class="bubble-row__avatar"
      :class="{ 'bubble-row__avatar--mine': mine }"
      :title="mine ? '' : '查看对方信息'"
      @click="onAvatarClick"
    >
      {{ avatar }}
    </el-avatar>

    <div class="bubble-row__body">
      <div v-if="group && !mine" class="bubble-row__name" :title="senderName">{{ senderName }}</div>

      <!--
        特殊类型消息的来源标记放在气泡外面：普通文字消息的正文里无论写什么都不会有这一行，
        看到它才能确认这是客户端按消息类型识别出来的密钥消息，而不是别人抄了一遍格式的文字。
      -->
      <el-tooltip
        v-if="groupKey"
        placement="top"
        :content="keyTrusted
          ? '来源可信：群标识以发送者公钥开头，且通过私聊通道分发'
          : '来源可疑：群标识与发送者不匹配，或不是通过私聊通道分发，可能是伪造的密钥消息'"
      >
        <el-tag
          class="bubble-row__tag"
          size="small"
          effect="plain"
          :type="keyTrusted ? 'success' : 'danger'"
        >
          {{ keyTrusted ? '群主分发' : '来源可疑' }}
        </el-tag>
      </el-tooltip>

      <!-- 群聊密钥：群主分发密钥时发的消息，密钥本身不展示，只说明是哪个群、哪个版本 -->
      <div v-if="groupKey" class="bubble" :class="{ 'bubble--untrusted': !keyTrusted }">
        <GroupKeyMessage :group="groupKey.group" :publish-time="groupKey.publishTime" />
      </div>

      <!-- 聊天图片：正文与文件相同，只是直接渲染出来 -->
      <div v-else-if="imageBody" class="bubble bubble--media">
        <ImageMessage :file="imageBody" />
      </div>

      <!-- 聊天视频：内嵌播放器，右上角可以点开弹窗放大播放 -->
      <div v-else-if="videoBody" class="bubble bubble--media">
        <VideoMessage :file="videoBody" />
      </div>

      <!-- 聊天音频：直接用浏览器原生播放器 -->
      <div v-else-if="audioBody" class="bubble">
        <AudioMessage :file="audioBody" />
      </div>

      <!-- 聊天文件：正文只带文件名与下载信息，文件本体由客户端取回解密后再交给浏览器保存 -->
      <div v-else-if="fileBody" class="bubble">
        <div class="file">
          <el-icon class="file__icon" :size="22"><Document /></el-icon>
          <span class="file__name" :title="fileBody.filename">{{ fileBody.filename }}</span>
          <el-button
            size="small"
            type="primary"
            :icon="Download"
            @click="onDownloadFile"
          >
            下载
          </el-button>
        </div>
      </div>

      <div v-else-if="attachmentBroken" class="bubble">{{ attachmentBroken }}</div>

      <div v-else class="bubble">{{ text ?? '[暂不支持的消息类型]' }}</div>
    </div>
  </div>
</template>

<style scoped>
.bubble-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 4px 16px;
}

.bubble-row--mine {
  flex-direction: row-reverse;
}

.bubble-row__avatar {
  flex: none;
  background: #d8d8d8;
  color: #555;
}

.bubble-row__avatar--mine {
  background: #07c160;
  color: #fff;
}

/* 对方头像可点：打开与 TA 的会话 */
.bubble-row--other .bubble-row__avatar {
  cursor: pointer;
}

.bubble-row__body {
  display: flex;
  flex-direction: column;
  /* 消息最多占聊天区宽度的 70%，再宽阅读体验会变差 */
  max-width: min(70%, 560px);
}

.bubble-row--mine .bubble-row__body {
  align-items: flex-end;
}

.bubble-row__name {
  margin-bottom: 2px;
  /* 别名可能比公钥末几位长，放不下就省略，别把气泡撑变形 */
  max-width: 100%;
  overflow: hidden;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.bubble-row__tag {
  /* 纵向 flex 容器里的子项默认会被拉伸到整列宽，标签要按内容宽度贴在自己这一侧 */
  align-self: flex-start;
  margin-bottom: 4px;
}

.bubble-row--mine .bubble-row__tag {
  align-self: flex-end;
}

.bubble {
  padding: 8px 12px;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-row--mine .bubble {
  background: #95ec69;
}

.file {
  display: flex;
  align-items: center;
  gap: 10px;
  /* 卡片不设最小宽度：聊天区很窄时也要能跟着收缩，否则会顶出边界 */
  min-width: 0;
  max-width: 100%;
}

/*
  图片、视频这类媒体气泡：不留底色与阴影，只留一点内边距，让媒体本身成为气泡。
  宽度上限放在气泡上、而不是媒体上：媒体的 max-width 百分比要相对父级算，而父级是按内容
  收缩的，浏览器算内容宽度时会忽略这个百分比，结果气泡被媒体的原始尺寸撑宽、右侧空出一块。
  328 = 内容 320 + 左右各 4px 内边距。
*/
.bubble.bubble--media {
  width: fit-content;
  max-width: 328px;
  padding: 4px;
  background: transparent;
  box-shadow: none;
}

.file__icon {
  flex: none;
  color: #409eff;
}

.file__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  font-size: 13px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 来源没通过校验的密钥消息：红框突出，避免被当成真的群密钥 */
.bubble--untrusted {
  border: 1px solid #f56c6c;
}
</style>
