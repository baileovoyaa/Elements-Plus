/**
 * 把文档里的 ```epp-experiments 代码块渲染成实验条目。
 *
 * 写法（每行一个实验，可加 [OPTIONAL] 前缀）：
 *
 *   ```epp-experiments
 *   非门
 *   [OPTIONAL] 模拟非门
 *   ```
 *
 * 数据来源都是仓库里的资源文件，改 Java / lang / 贴图后重启 dev 即可：
 *   - 实验 id 与图标：src/main/java/.../BuiltinExperiments.java
 *   - 图标文件：    src/main/java/.../BuiltinCircuitComponents.java + textures/circuit/*.png
 *   - 中文名：      assets/elements-plus/lang/zh_cn.json 里的 experiment.elements-plus.*
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const VITEPRESS_DIR = path.dirname(fileURLToPath(import.meta.url))
export const PROJECT_ROOT = path.resolve(VITEPRESS_DIR, '../..')

const ASSETS_DIR = path.join(PROJECT_ROOT, 'src/main/resources/assets/elements-plus')
const COMPONENTS_SRC = path.join(
  PROJECT_ROOT,
  'src/main/java/com/elementsplus/core/circuit/BuiltinCircuitComponents.java'
)
const EXPERIMENTS_SRC = path.join(
  PROJECT_ROOT,
  'src/main/java/com/elementsplus/core/experiment/BuiltinExperiments.java'
)
const LANG_FILE = path.join(ASSETS_DIR, 'lang/zh_cn.json')

/** 代码块里支持的行首标记 */
const TAGS = {
  OPTIONAL: '可选'
}

let cache

/**
 * @returns {{ byName: Map<string, {id: string, icon: string | null}>, byId: Map<string, {name: string, icon: string | null}> }}
 */
export function loadExperimentIndex() {
  if (cache) return cache

  const byName = new Map()
  const byId = new Map()

  try {
    const lang = JSON.parse(fs.readFileSync(LANG_FILE, 'utf8'))
    const componentIcons = parseComponentIcons(fs.readFileSync(COMPONENTS_SRC, 'utf8'))
    const experiments = parseExperiments(fs.readFileSync(EXPERIMENTS_SRC, 'utf8'), componentIcons)

    for (const [id, iconPath] of experiments) {
      const name = lang[`experiment.elements-plus.${id}`]
      if (!name) continue

      const icon =
        iconPath && fs.existsSync(path.join(ASSETS_DIR, iconPath)) ? `/${iconPath}` : null
      byId.set(id, { name, icon })
      byName.set(name, { id, icon })
    }
  } catch (error) {
    console.warn(`[epp-experiments] 读取实验数据失败，实验块只显示名称：${error.message}`)
  }

  cache = { byName, byId }
  return cache
}

/** CONST -> textures/xxx.png */
function parseComponentIcons(source) {
  const icons = new Map()
  const re =
    /CircuitComponent\s+([A-Z0-9_]+)\s*=\s*register\(\s*new CircuitComponent\(\s*ElementsPlus\.id\("[^"]+"\)[\s\S]{0,600}?ElementsPlus\.id\("(textures\/[^"]+\.png)"\)/g
  for (const match of source.matchAll(re)) {
    icons.set(match[1], match[2])
  }
  return icons
}

/** 实验 id -> 图标贴图路径（可能为 null） */
function parseExperiments(source, componentIcons) {
  const result = new Map()
  const re =
    /register\("([^"]+)",\s*(?:BuiltinCircuitComponents\.([A-Z0-9_]+)\.getIcon\(\)|ElementsPlus\.id\("(textures\/[^"]+\.png)"\))/g

  for (const match of source.matchAll(re)) {
    const [, id, componentName, directIcon] = match
    result.set(id, directIcon ?? componentIcons.get(componentName) ?? null)
  }
  return result
}

/** 实验块里用到的图标，在仓库里的源文件路径（供脚本判断“图标改了要不要重新部署”） */
export function experimentIconSourcePaths() {
  const { byId } = loadExperimentIndex()
  return [...byId.values()]
    .map((experiment) => experiment.icon)
    .filter(Boolean)
    .map((icon) => path.relative(PROJECT_ROOT, path.join(ASSETS_DIR, icon.slice(1))).split(path.sep).join('/'))
}

/** 与 VitePress 标题锚点完全一致的 slug 规则（@mdit-vue/shared） */
const rControl = /[\u0000-\u001f]/g
const rSpecial = /[\s~`!@#$%^&*()\-_+=[\]{}|\\;:"'“”‘’<>,.?/]+/g
const rCombining = /[\u0300-\u036F]/g

export function slugify(str) {
  return str
    .normalize('NFKD')
    .replace(rCombining, '')
    .replace(rControl, '')
    .replace(rSpecial, '-')
    .replace(/-{2,}/g, '-')
    .replace(/^-+|-+$/g, '')
    .replace(/^(\d)/, '_$1')
    .toLowerCase()
}

const escapeHtml = (str) =>
  str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')

const warned = new Set()

/**
 * markdown-it 插件：接管 ```epp-experiments
 * 用法：md.use(experimentsPlugin, { base })
 * @param {import('markdown-it')} md
 * @param {{ base?: string }} options
 */
export function experimentsPlugin(md, options = {}) {
  const base = options.base ?? '/'
  const defaultFence = md.renderer.rules.fence

  md.renderer.rules.fence = (tokens, idx, renderOptions, env, self) => {
    const token = tokens[idx]
    if ((token.info || '').trim() !== 'epp-experiments') {
      return defaultFence(tokens, idx, renderOptions, env, self)
    }
    return renderExperiments(token.content, base)
  }
}

function renderExperiments(content, base) {
  const { byName } = loadExperimentIndex()
  const items = content
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => renderItem(line, byName, base))

  return `<div class="epp-experiments">\n${items.join('\n')}\n</div>\n`
}

function renderItem(line, byName, base) {
  const tagMatch = line.match(/^\[([A-Za-z]+)\]\s*(.+)$/)
  const tag = tagMatch ? tagMatch[1].toUpperCase() : null
  const name = tagMatch ? tagMatch[2].trim() : line
  const experiment = byName.get(name)

  if (!experiment && !warned.has(name)) {
    warned.add(name)
    console.warn(`[epp-experiments] 未知实验名「${name}」，只渲染名称（检查 lang/zh_cn.json）`)
  }

  // 用 :src 绑定而不是静态 src：静态 src 会被 Vue 当资源 import 去解析，
  // public 下带 base 前缀的地址解析不到，构建会报 UNRESOLVED_IMPORT
  const icon = experiment?.icon
    ? `<img class="epp-experiment-icon" :src="'${escapeHtml(base + experiment.icon.slice(1))}'" alt="" width="20" height="20" loading="lazy">`
    : ''
  const badge = tag ? `<span class="epp-experiment-badge">${escapeHtml(TAGS[tag] ?? tag)}</span>` : ''
  const href = `${base}user/experiments#${slugify(name)}`

  return (
    `<a class="epp-experiment" href="${escapeHtml(href)}">` +
    icon +
    `<span class="epp-experiment-name">${escapeHtml(name)}</span>` +
    badge +
    `</a>`
  )
}
