#!/usr/bin/env node
/**
 * 判断一次提交里改动的贴图有没有被文档引用，决定要不要重新部署站点。
 *
 * 用途：deploy.yml 把贴图目录也加进了触发路径，但改一张没被任何文档页引用的贴图
 * （比如只给英文版用的 intro.png）没必要重新部署站点。
 *
 * CI 用法：node script/check-docs-textures.mjs --since "$BEFORE_SHA"
 * 本地调试：node script/check-docs-textures.mjs --files "docs/a.md,src/.../intro.zh_cn.png"
 * 输出：向 $GITHUB_OUTPUT 写入 deploy=true|false，并打印判断依据。
 */
import { execFileSync } from 'node:child_process'
import { readdir, readFile } from 'node:fs/promises'
import { appendFileSync, existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { isImageFile, urlToSourcePath } from './docs-textures.mjs'
import { experimentIconSourcePaths } from '../docs/.vitepress/experiments.mjs'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

/** 扫描这些后缀的文档源文件里出现的图片引用 */
const DOC_EXTENSIONS = new Set(['.md', '.markdown', '.vue', '.ts', '.js', '.html'])
/** 这些目录不参与扫描（构建产物 / 生成物） */
const SKIP_DIRS = new Set(['node_modules', 'dist', 'cache', 'public'])
/** 这些文件本身影响站点构建，改了就直接部署 */
const SITE_FILES = new Set([
  'CONTRIBUTING.md', // 被 docs/contributing.md 内联
  'package.json',
  'package-lock.json',
  'script/docs-textures.mjs',
  'script/sync-docs-textures.mjs',
  'script/check-docs-textures.mjs'
])

const TEXTURE_URL_RE = /\/textures\/[^\s"'()<>`\]]+/g
const TRAILING_PUNCTUATION = /[.,;:!?。，；：！？]+$/

function parseArgs(argv) {
  const args = { since: '', sha: 'HEAD', files: null, force: false }
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i]
    if (arg === '--since') args.since = argv[++i] ?? ''
    else if (arg === '--sha') args.sha = argv[++i] ?? 'HEAD'
    else if (arg === '--files') {
      args.files = (argv[++i] ?? '')
        .split(',')
        .map((file) => file.trim())
        .filter(Boolean)
    } else if (arg === '--force') args.force = true
    else throw new Error(`未知参数: ${arg}`)
  }
  return args
}

function normalize(filePath) {
  return filePath.replace(/\\/g, '/').replace(/^\.\//, '')
}

async function collectDocFiles(dir, baseDir = dir, files = []) {
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      if (SKIP_DIRS.has(entry.name)) continue
      await collectDocFiles(full, baseDir, files)
    } else if (DOC_EXTENSIONS.has(path.extname(entry.name).toLowerCase())) {
      files.push(path.relative(baseDir, full))
    }
  }
  return files
}

/** 文档里引用到的贴图，映射回仓库里的源文件路径 */
async function referencedSourcePaths() {
  const docsDir = path.join(root, 'docs')
  if (!existsSync(docsDir)) return new Set()

  const referenced = new Set()
  for (const relPath of await collectDocFiles(docsDir)) {
    const content = await readFile(path.join(docsDir, relPath), 'utf8')
    for (const match of content.matchAll(TEXTURE_URL_RE)) {
      const urlPath = match[0].replace(TRAILING_PUNCTUATION, '')
      let decoded
      try {
        decoded = decodeURI(urlPath)
      } catch {
        continue
      }
      const sourcePath = urlToSourcePath(decoded)
      if (sourcePath) referenced.add(normalize(sourcePath))
    }
  }

  // 实验块的图标是构建期从 Java 注册表生成的，文档正文里搜不到这些地址
  try {
    for (const iconPath of experimentIconSourcePaths()) referenced.add(normalize(iconPath))
  } catch (error) {
    console.warn(`[docs-deploy] 读取实验图标失败: ${error.message}`)
  }

  return referenced
}

function gitChangedFiles(since, sha) {
  const output = execFileSync('git', ['diff', '--name-only', '--diff-filter=ACMRD', `${since}..${sha}`, '--'], {
    cwd: root,
    encoding: 'utf8'
  })
  return output.split('\n').map(normalize).filter(Boolean)
}

/** 新分支 / 强制推送时 github.event.before 是全 0 或取不到，退一步用 HEAD^ */
function fallbackChangedFiles() {
  try {
    execFileSync('git', ['rev-parse', '--verify', 'HEAD^'], { cwd: root, stdio: 'ignore' })
    return gitChangedFiles('HEAD^', 'HEAD')
  } catch {
    return null
  }
}

function classify(file, referenced) {
  if (file.startsWith('docs/')) return '文档源文件'
  if (SITE_FILES.has(file)) return '站点配置/脚本'
  if (isImageFile(file) && referenced.has(file)) return '被文档引用的贴图'
  return null
}

function report(deploy) {
  console.log(`[docs-deploy] 结论: deploy=${deploy}`)
  if (process.env.GITHUB_OUTPUT) {
    appendFileSync(process.env.GITHUB_OUTPUT, `deploy=${deploy}\n`)
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  const force = args.force || process.env.FORCE_DEPLOY === 'true'

  if (force) {
    console.log('[docs-deploy] 手动触发（workflow_dispatch），直接部署')
    return report(true)
  }

  let changed = args.files
  if (!changed && args.since && !/^0+$/.test(args.since)) {
    try {
      changed = gitChangedFiles(args.since, args.sha)
    } catch (error) {
      console.warn(`[docs-deploy] 无法对比 ${args.since}..${args.sha}: ${error.message}`)
    }
  }
  changed ??= fallbackChangedFiles()

  if (!changed) {
    console.warn('[docs-deploy] 拿不到改动列表，保守起见部署')
    return report(true)
  }

  const referenced = await referencedSourcePaths()
  console.log(`[docs-deploy] 文档引用了 ${referenced.size} 张贴图，本次改动 ${changed.length} 个文件`)

  let deploy = false
  for (const file of changed) {
    const reason = classify(file, referenced)
    if (reason) {
      deploy = true
      console.log(`  ✓ ${file} —— ${reason}`)
    } else if (isImageFile(file)) {
      console.log(`  - ${file} —— 忽略，没有文档引用`)
    } else {
      console.log(`  - ${file} —— 忽略`)
    }
  }

  return report(deploy)
}

await main()
