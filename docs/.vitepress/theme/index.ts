import type { Theme } from 'vitepress'
import DefaultTheme from 'vitepress/theme'
import Layout from './Layout.vue'
import { setupSpoilers } from './spoiler'
import './style.css'

export default {
  extends: DefaultTheme,
  Layout,
  enhanceApp() {
    setupSpoilers()
  }
} satisfies Theme
