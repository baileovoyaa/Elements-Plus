/**
 * 图片剧透的交互：点击取消模糊，再点一次恢复。
 * 标记和样式分别来自 .vitepress/spoiler.mjs 与 theme/style.css。
 */
const SPOILER_SELECTOR = '.epp-spoiler'
const HINT_TEXT = '点击显示剧透'
const REVEALED_CLASS = 'epp-spoiler--revealed'

function toggleSpoiler(spoiler: Element): void {
  const revealed = spoiler.classList.toggle(REVEALED_CLASS)
  spoiler.setAttribute('aria-pressed', String(revealed))
  spoiler.setAttribute('aria-label', revealed ? '隐藏剧透' : HINT_TEXT)
}

function closestSpoiler(target: EventTarget | null): Element | null {
  return target instanceof Element ? target.closest(SPOILER_SELECTOR) : null
}

export function setupSpoilers(): void {
  // enhanceApp 在 SSR 时也会执行，这里只处理浏览器环境
  if (typeof document === 'undefined') return

  // 用事件委托：SPA 切换页面后新插入的剧透图不需要重新绑定
  document.addEventListener('click', (event) => {
    const spoiler = closestSpoiler(event.target)
    if (spoiler) toggleSpoiler(spoiler)
  })

  document.addEventListener('keydown', (event) => {
    if (event.key !== 'Enter' && event.key !== ' ') return
    const spoiler = closestSpoiler(event.target)
    if (!spoiler) return
    event.preventDefault()
    toggleSpoiler(spoiler)
  })
}
