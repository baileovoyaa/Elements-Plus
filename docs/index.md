---
# https://vitepress.dev/reference/default-theme-home-page
layout: home
title: 文档索引

hero:
  name: "Elements Plus"
  text: "Elements Plus docs"
  tagline: My great project tagline
  actions:
    - theme: brand
      text: Markdown Examples
      link: /markdown-examples
    - theme: alt
      text: API Examples
      link: /api-examples

features:
  - title: Feature A
    details: Lorem ipsum dolor sit amet, consectetur adipiscing elit
  - title: Feature B
    details: Lorem ipsum dolor sit amet, consectetur adipiscing elit
  - title: Feature C
    details: Lorem ipsum dolor sit amet, consectetur adipiscing elit
---

# 用户文档索引

面向玩家、整合包作者和服务器管理员，提供**使用**此模组的详细操作指南和参考文档。

> 文档仍在不断完善中...

- [用户文档](user/index.md)
    - [快速开始](user/quick-start.md)
    - [配方一览](user/recipes.md)
    - [命令](user/commands.md)
    - [配置](user/configs.md)
    - [内置实验章节索引](user/experiment-chapters/index.md)
    - [自定义实验章节](user/custom-experiment-chapters.md)

# 开发文档索引

面向模组开发者，提供构建指导和代码实现细节等，也包含开发中计划。

## 构建、执行与部署

- [本地配置 Gradle 发行版镜像](dev/deploy/gradle-mirror.md)
- 自定义玩家名和 uuid

## 阶段性文档

- [v1.0.0](dev/phases/v1.0.0/index.md)

## 参考

### 电路

- [参考算法描述](dev/reference/电路/参考算法描述.md)
- [参考HTML实现](dev/reference/电路/电路（时序）（优化版）.html)

### 实验

- 电路
    - [8位加法器测例生成.py](dev/reference/%E5%AE%9E%E9%AA%8C/%E7%94%B5%E8%B7%AF/8%E4%BD%8D%E5%8A%A0%E6%B3%95%E5%99%A8%E6%B5%8B%E4%BE%8B%E7%94%9F%E6%88%90.py)

## 路线图

[路线图](dev/roadmap.md)