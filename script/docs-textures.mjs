/**
 * 文档贴图的共享配置与工具，被下面两个脚本使用：
 *   - script/sync-docs-textures.mjs   把贴图同步到 docs/public
 *   - script/check-docs-textures.mjs  判断改动的贴图是否被文档引用
 */
import { readdir } from 'node:fs/promises'
import path from 'node:path'

/**
 * 需要同步的贴图目录：
 *   from —— 模组里的源目录（仓库相对路径）
 *   to   —— 同步到 docs/public 下的位置（站点里以 to 去掉 docs/public 后的路径访问）
 */
export const SOURCES = [
  {
    from: 'src/main/resources/assets/elements-plus/textures/gui/experiment_table/chapters',
    to: 'docs/public/textures/gui/experiment_table/chapters'
  },
  {
    // 实验块的图标
    from: 'src/main/resources/assets/elements-plus/textures/circuit',
    to: 'docs/public/textures/circuit'
  },
  {
    // 少数实验的图标是物品贴图（如 自定义元件 -> medium_chip）
    from: 'src/main/resources/assets/elements-plus/textures/item',
    to: 'docs/public/textures/item'
  }
]

/** 只处理图片，避免把 psd、备份之类的杂物发到站点上 */
export const IMAGE_EXTENSIONS = new Set([
  '.png',
  '.jpg',
  '.jpeg',
  '.webp',
  '.gif',
  '.svg',
  '.avif'
])

export function isImageFile(filePath) {
  return IMAGE_EXTENSIONS.has(path.extname(filePath).toLowerCase())
}

export async function collectImages(dir, baseDir = dir, files = []) {
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      await collectImages(full, baseDir, files)
    } else if (isImageFile(entry.name)) {
      files.push(path.relative(baseDir, full))
    }
  }
  return files
}

/** docs/public 下的目录对应的站点 URL 前缀，例如 docs/public/textures/x -> /textures/x */
export function publicUrlPrefix({ to }) {
  return '/' + to.replace(/^docs\/public\/?/, '').replace(/\/$/, '')
}

/** 把文档里的 /textures/... 地址映射回仓库里的源文件路径；映射不上返回 null */
export function urlToSourcePath(urlPath) {
  for (const source of SOURCES) {
    const prefix = publicUrlPrefix(source)
    if (urlPath === prefix || urlPath.startsWith(prefix + '/')) {
      return path.posix.join(source.from, urlPath.slice(prefix.length + 1))
    }
  }
  return null
}
