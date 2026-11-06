#!/usr/bin/env node
/**
 * 把模组里的贴图同步到 docs/public，供 VitePress 当静态资源引用。
 *
 * 为什么需要：VitePress 的 dev server 只认 srcDir（docs/）里的文件，
 * 直接引用 src/main/resources 下的图在 dev 下会 404。
 *
 * 产物目录（docs/public/textures/）是生成物，已被 .gitignore 忽略；
 * 通过 package.json 的 predocs:dev / predocs:build 自动执行。
 *
 * 手动执行：node script/sync-docs-textures.mjs
 */
import { copyFile, mkdir, rm, stat } from 'node:fs/promises'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { SOURCES, collectImages } from './docs-textures.mjs'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

async function sync({ from, to }) {
  const srcDir = path.join(root, from)
  const destDir = path.join(root, to)

  if (!existsSync(srcDir)) {
    console.warn(`[docs-textures] 跳过，源目录不存在: ${from}`)
    return 0
  }

  const files = await collectImages(srcDir)

  // 先清空：贴图改名/删除后，旧文件不应该继续留在站点里
  await rm(destDir, { recursive: true, force: true })

  let bytes = 0
  for (const relPath of files) {
    const target = path.join(destDir, relPath)
    await mkdir(path.dirname(target), { recursive: true })
    await copyFile(path.join(srcDir, relPath), target)
    bytes += (await stat(target)).size
  }

  console.log(
    `[docs-textures] ${from} -> ${to}，${files.length} 个文件，${(bytes / 1024 / 1024).toFixed(2)} MB`
  )
  return files.length
}

let total = 0
for (const source of SOURCES) {
  total += await sync(source)
}
if (total === 0) {
  console.warn('[docs-textures] 没有同步任何文件')
}
