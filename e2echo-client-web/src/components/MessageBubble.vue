<script setup lang="ts">
import { computed } from 'vue'
import { chatTextOf, groupKeyOf, isTrustedGroupKey } from '@/api'
import type { MessageVo } from '@/api'
import GroupKeyMessage from '@/components/GroupKeyMessage.vue'
import { shortKey } from '@/utils/display'

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
}>()

/** 文字正文，非文字消息为空。 */
const text = computed(() => chatTextOf(props.message))

/** 群聊密钥消息的正文，不是这类消息时为空。 */
const groupKey = computed(() => groupKeyOf(props.message))

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

/** 头像文字：自己的消息用自己的名称，对方的按公钥末几位。 */
const avatar = computed(() =>
  props.mine && props.mineAvatar ? props.mineAvatar : shortKey(props.message.from, 2))
</script>

<template>
  <div class="bubble-row" :class="mine ? 'bubble-row--mine' : 'bubble-row--other'">
    <el-avatar
      :size="32"
      class="bubble-row__avatar"
      :class="{ 'bubble-row__avatar--mine': mine }"
    >
      {{ avatar }}
    </el-avatar>

    <div class="bubble-row__body">
      <div v-if="group && !mine" class="bubble-row__name">{{ shortKey(message.from) }}</div>

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

.bubble-row__body {
  display: flex;
  flex-direction: column;
  max-width: min(70%, 560px);
}

.bubble-row--mine .bubble-row__body {
  align-items: flex-end;
}

.bubble-row__name {
  margin-bottom: 2px;
  font-size: 12px;
  color: #909399;
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

/* 来源没通过校验的密钥消息：红框突出，避免被当成真的群密钥 */
.bubble--untrusted {
  border: 1px solid #f56c6c;
}
</style>
