---
title: "寄存器"
category: "用户文档"
---

> 前置章节：[触发器](flip-flop.md)

电容器可以将信号储存1tick，但有时我们会想把信号储存更久。

因此就需要一种能持久保存信号的元件——寄存器。

首先是<font color=gold>1位寄存器</font>。它有一个“写选通”，当写选通启用（非0）时，会存储输入的值，写选通未启用时，则会保持已存储的值。注意存储的是放大值而不是原始信号强度。

- <font color=gold>1位寄存器</font>：输入A和WE，输出Y。当时钟步进时，若WE非0，Y会变为A的放大值，否则Y保持不变。

```epp-experiments
1位寄存器
```

<font color=gold>8位寄存器</font>是其对应的8位版。

- <font color=gold>8位寄存器</font>：输入A（8位）和WE（1位），输出Y（8位）。当时钟步进时，若WE非0，Y会变为A的逐通道放大值，否则Y保持不变。

```epp-experiments
8位寄存器
```

还有一种可以保存模拟信号强度的“<font color=gold>模拟寄存器</font>”，它是一种“采样/保持电路”，可以根据WE是否为0来决定是采样还是保持。

- <font color=gold>模拟寄存器</font>：输入A和WE，输出Y。当时钟步进时，若WE非0，Y会变为A的信号强度，否则Y保持不变。

```epp-experiments
[OPTIONAL] 模拟寄存器
```