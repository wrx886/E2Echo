import { ElMessage } from 'element-plus'
import { isAuthFailed } from '@/api'

/**
 * 统一的错误提示。
 *
 * <p>会话失效由接口层统一提示（引导用户回到客户端重新打开网页），这里跳过它，避免同一次失败弹出
 * 两条提示。</p>
 *
 * @param error    捕获到的异常
 * @param fallback 异常没有可读信息时的兜底提示
 */
export function showError(error: unknown, fallback = '操作失败'): void {
  if (isAuthFailed(error)) {
    return
  }
  ElMessage.error(error instanceof Error && error.message ? error.message : fallback)
}
