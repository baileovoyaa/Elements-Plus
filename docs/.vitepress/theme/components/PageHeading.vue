<!-- .vitepress/theme/components/PageHeading.vue -->

<template>
  <div class="custom-page-header">
    <div class="custom-page-title">{{ $frontmatter.title }}</div>
    <div class="custom-page-author">{{ $frontmatter.author ? '@' + $frontmatter.author : '' }}</div>
  </div>
</template>

<style scoped>
.custom-page-header {
  display: flex;
  flex-wrap: wrap;          /* 空间不足时换行 */
  align-items: flex-end;    /* 同行时底部对齐 */
  gap: 0.25em 1em;          /* 行间距 0.25em，列间距 1em */
  margin-bottom: 2rem;
  border-bottom: 1px solid #ccc;
}
.custom-page-title {
  font-size: 3rem; /* 远大于默认 h1 的尺寸 */
  flex: 1 1 12em;           /* 关键：12em 是换行阈值 */
  min-width: 0;             /* 允许收缩，防止长单词撑破 */
  overflow-wrap: anywhere;  /* 长标题可断行，可选 */
}
.custom-page-author {
  flex: 0 0 auto;           /* 作者不压缩、不拉伸 */
  color: #666;
  margin-bottom: 0.5em;
}
</style>