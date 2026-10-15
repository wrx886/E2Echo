import { nextTick, onBeforeUnmount, watch, type Ref } from 'vue'

/**
 * 列表的"返回后定位"。
 *
 * <p>从列表点进详情、返回时把当时那条滚回可视区中间。锚点记的是元素 ID 而不是像素位置：附件是
 * 浏览器异步加载的，内容高度随时会变，像素位置很容易漂，按元素定位则不受影响——加载完内容变高时
 * 继续把它拉回中间即可。</p>
 *
 * <p>定位只在"从详情页返回"时做：从别处（侧边栏、中间栏入口）重新进这个页面不该被拽到中间。
 * 判断依据是 vue-router 记在 {@code history.state.back} 里的来源路径。</p>
 */

/**
 * 上一次导航是不是"回退"。
 *
 * <p>定位只在返回时做：从侧边栏、中间栏入口重新进页面不该被拽到中间。判断方向不能用
 * {@code history.state.back}（那是"这条记录当初从哪来"，回退后再进并不会更新），要看 vue-router 在
 * 历史记录里维护的 {@code position}——回退时它会变小。</p>
 *
 * <p>由路由的 {@code afterEach} 调用 {@link recordNavigation} 维护。</p>
 */
let lastPosition = Number.NaN
let backNavigation = false

/**
 * 记录一次导航结束时的历史位置，判断这次是不是回退。
 */
export function recordNavigation(): void {
  const position = (window.history.state as { position?: number } | null)?.position
  if (typeof position !== 'number') {
    backNavigation = false
    return
  }
  backNavigation = !Number.isNaN(lastPosition) && position < lastPosition
  lastPosition = position
}

/**
 * 这一次导航是不是回退。
 *
 * @returns 回退返回 true
 */
export function isBackNavigation(): boolean {
  return backNavigation
}

export interface AnchorScrollOptions {
  /** 滚动容器。 */
  container: Ref<HTMLDivElement | undefined>
  /** 内容容器，观察它的高度变化。 */
  content: Ref<HTMLDivElement | undefined>
  /** 锚点元素 ID，空串表示不需要定位。 */
  anchorId: Ref<string>
  /** 跟随定位的最长时间（毫秒），超时后交给用户，默认 3 秒。 */
  followMs?: number
}

export interface AnchorScroll {
  /** 开始定位：等渲染完把锚点滚到中间，并在内容变高时继续跟随。 */
  start(): Promise<void>
  /** 用户自己滚动了：不要再把视图拽回锚点。 */
  stop(): void
  /** 刷新用：放弃定位并把列表滚回顶部（刷新就是要看最新的）。 */
  reset(): void
}

/**
 * 创建一套定位逻辑。
 *
 * @param options 容器与锚点
 * @returns 定位控制器
 */
export function useAnchorScroll(options: AnchorScrollOptions): AnchorScroll {
  const { container, content, anchorId, followMs = 3000 } = options

  /** 跟随定位的截止时间。 */
  let deadline = 0

  /** 内容高度观察器。 */
  let observer: ResizeObserver | null = null

  /**
   * 把锚点元素滚到可视区中间。
   */
  function apply(): void {
    const id = anchorId.value
    const element = container.value
    if (id.length === 0 || !element) {
      return
    }
    const target = element.querySelector(`[data-item-id="${id}"]`)
    target?.scrollIntoView({ block: 'center' })
  }

  /**
   * 观察内容高度：附件加载完内容会变高，锚点的位置跟着变，所以在这段时间里持续把它拉回中间。
   */
  function observe(): void {
    const element = content.value
    if (!element || typeof ResizeObserver === 'undefined') {
      return
    }
    observer = new ResizeObserver(() => {
      if (anchorId.value.length === 0 || Date.now() > deadline) {
        return
      }
      apply()
    })
    observer.observe(element)
  }

  async function start(): Promise<void> {
    // 不是回退进来的（比如从侧边栏、中间栏入口重新进页面），就不定位
    if (!isBackNavigation()) {
      anchorId.value = ''
      return
    }
    deadline = Date.now() + followMs
    await nextTick()
    apply()
    observe()
  }

  function stop(): void {
    anchorId.value = ''
  }

  function reset(): void {
    anchorId.value = ''
    deadline = 0
    observer?.disconnect()
    observer = null
    const element = container.value
    if (element) {
      element.scrollTop = 0
    }
  }

  // 内容容器是 v-if/v-else 里的，切换时会换元素，重新挂观察器
  watch(content, (element, previous) => {
    if (element === previous) {
      return
    }
    observer?.disconnect()
    observer = null
    if (element && anchorId.value.length > 0 && Date.now() <= deadline) {
      observe()
    }
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    observer = null
  })

  return { start, stop, reset }
}
