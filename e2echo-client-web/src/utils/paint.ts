/**
 * 绘制相关的调度小工具。
 */

/**
 * 等浏览器画完当前这一帧之后再执行。
 *
 * <p>切换会话要重建消息区（重活），如果和点击反馈放在同一个任务里，浏览器只能等重活做完才绘制，
 * 于是连「点到了」这一下都跟着一起晚——这就是 INP 里那 1 秒多的展示延迟。把重活挪到下一帧之后，
 * 反馈先画出来，重活再干。</p>
 *
 * <p>先 requestAnimationFrame 再 setTimeout：rAF 回调本身还在这一帧的渲染之前执行，只有把它丢进
 * 下一个宏任务，才真的排在这一帧绘制之后。</p>
 *
 * @param action 要在绘制之后执行的动作
 */
export function afterPaint(action: () => void): void {
  requestAnimationFrame(() => {
    window.setTimeout(action, 0)
  })
}
