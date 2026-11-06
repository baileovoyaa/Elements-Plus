<script setup lang="ts">
import { computed, provide } from 'vue'
import { dataSymbol, useData } from 'vitepress'
import type { DefaultTheme } from 'vitepress'
import VitePressTheme from 'vitepress/theme'
import PageHeading from './components/PageHeading.vue'

const DefaultLayout = VitePressTheme.Layout
const data = useData()

/** themeConfig.categorySidebars，键是 frontmatter 里的 category */
const categorySidebars = computed<Record<string, DefaultTheme.SidebarItem[]>>(
  () =>
    (
      data.theme.value as DefaultTheme.Config & {
        categorySidebars?: Record<string, DefaultTheme.SidebarItem[]>
      }
    ).categorySidebars ?? {}
)

/**
 * 默认主题从 useData().theme 里读 sidebar。
 * theme 是只读的合并视图，改不了，所以这里换一份带 sidebar 的 data 往下传。
 */
const theme = computed(() => {
  const config = data.theme.value
  const sidebar = categorySidebars.value[data.frontmatter.value.category]
  return sidebar ? { ...config, sidebar } : config
})

provide(dataSymbol, { ...data, theme })
</script>

<template>
  <DefaultLayout>
    <template #doc-before>
      <PageHeading />
    </template>
  </DefaultLayout>
</template>
