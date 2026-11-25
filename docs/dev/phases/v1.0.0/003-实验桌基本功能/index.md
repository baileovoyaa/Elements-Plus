---
title: "实验桌基本功能"
category: "开发文档"
date: 2026-9-20
revision: 110a603c5b93026e9e0f5561f604be444f11458d
---

> [实验桌](../../../reference/%E5%AE%9E%E9%AA%8C%E6%A1%8C/index.md)
> [电路实验](../../../reference/%E5%AE%9E%E9%AA%8C/%E7%94%B5%E8%B7%AF%E5%AE%9E%E9%AA%8C.md)

在实现时考虑未来兼容性：

- 目前只需要实现组合逻辑测例（`CombinationalTestCase`），而未来会加入需要多个tick的测例（时序测例）。
- 目前只包含常量输入组合逻辑测例（`ConstantCombinationalTestCase`），而未来会有包含随机输入的测例（如8位加法器，65536种可能输入都测一遍并不现实）。

上述未来特性目前不需要刻意实现，但是兼容性要保留。

## 附带说明

- 目前很多实验相关的类都还没定型，存在大量TODO，可根据实际需要进行重构。重构时保留可扩展性。