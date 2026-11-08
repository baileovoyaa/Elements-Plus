/**
 * 图片剧透：给图片加 {.spoiler} 属性，默认模糊，悬停显示提示，点击取消模糊。
 *
 * 用法：
 *   ![参考解法](../static/solution/tff.png){.spoiler}
 *
 * 属性语法由 VitePress 内置的 attrs 插件解析（class 会落到 <img> 上），
 * 这里把带 spoiler 类的图片包一层可点击容器，交互逻辑在 theme/spoiler.ts。
 */

const SPOILER_CLASS = 'spoiler'
const HINT_TEXT = '点击显示剧透'

export function spoilerPlugin(md) {
  const defaultImage = md.renderer.rules.image

  md.renderer.rules.image = (tokens, idx, options, env, self) => {
    const token = tokens[idx]
    const classes = (token.attrGet('class') ?? '').split(/\s+/).filter(Boolean)

    if (!classes.includes(SPOILER_CLASS)) {
      return defaultImage(tokens, idx, options, env, self)
    }

    // 渲染 <img> 时临时去掉标记类，避免和容器类的样式冲突
    const original = classes.join(' ')
    const rest = classes.filter((name) => name !== SPOILER_CLASS)
    const classIndex = token.attrIndex('class')
    if (rest.length > 0) token.attrSet('class', rest.join(' '))
    else if (classIndex >= 0) token.attrs.splice(classIndex, 1)
    const img = defaultImage(tokens, idx, options, env, self)
    token.attrSet('class', original)

    return (
      `<span class="epp-spoiler" role="button" tabindex="0" aria-pressed="false" aria-label="${HINT_TEXT}">` +
      img +
      `<span class="epp-spoiler-hint" aria-hidden="true">${HINT_TEXT}</span>` +
      `</span>`
    )
  }
}
