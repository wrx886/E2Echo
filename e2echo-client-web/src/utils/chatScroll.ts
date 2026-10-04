import type { InjectionKey } from 'vue'

/**
 * 聊天页提供给下层组件的「贴回底部」能力。
 *
 * <p>消息里的内容变高（最典型的就是图片取回后撑开气泡）时，由内容的组件在布局完成后调用它，
 * 让视图重新贴住最新消息。聊天页会自己判断用户是不是已经往上翻了，没贴底就不会动。</p>
 */
export const CHAT_PIN_TO_BOTTOM: InjectionKey<() => void> = Symbol('chatPinToBottom')
