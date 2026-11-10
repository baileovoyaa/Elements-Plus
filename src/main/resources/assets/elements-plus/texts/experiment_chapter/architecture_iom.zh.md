## 从 A+B 到计算机

A+B 实验解决了“如何让电路与外界交换数据”。这一章开始，我们把这些电路组合起来，尝试搭建一台最简单的计算机。

本章只实现取指、寄存器和输入输出，暂时没有 ALU 和内存。

执行操作的依据是指令。为方便书写和阅读，本章用汇编形式表示指令，例如 `MOV R0, R1`；机器中实际保存的是 8 位编码。

## 取指执行

指令由外部的程序 ROM 提供。

- `PC`（8 位）是程序 ROM 的地址。
- 程序 ROM 在与 `PC` 对应的地址输出 `INS`（8 位）。
- 每个 tick 执行一条指令；顺序执行时 `PC` 每 tick 加 1。

取指通路可以写成：`PC -> ROM 地址 -> INS -> 解码执行`。

## 架构接口

| 方向 | 信号 | 位宽 | 说明 |
| --- | --- | --- | --- |
| 输入 | `INS` | 8 | 程序 ROM 在 `PC` 指向地址返回的指令字节 |
| 输入 | `VI` | 1 | 外部数据有效 |
| 输入 | `DI` | 8 | 外部输入数据 |
| 输入 | `RO` | 1 | 外部准备读取输出 |
| 输出 | `PC` | 8 | 程序 ROM 地址 |
| 输出 | `RI` | 1 | 电路准备接收输入 |
| 输出 | `VO` | 1 | 输出数据有效 |
| 输出 | `DO` | 8 | 输出数据 |

## 寄存器

本章使用 4 个 2 位编号的寄存器：`R0`、`R1`、`R2`、`R3`。

- `R0`、`R1` 是普通 8 位寄存器。
- `R2` 暂时保留，后续内存章节再使用。
- `R3` 是 IO 端口；读 `R3` 从 `DI` 取数，写 `R3` 把数据送到 `DO`。

## 指令编码

本章只实现两条指令：`NOP` 和 `MOV D, S`。

- `NOP`：`0000 0000`（`0x00`），只让 `PC` 加 1。
- `MOV D, S`：`0011 SS DD`，把源寄存器 `S` 的值送到目标寄存器 `D`。

| 指令 | 二进制 | 十六进制 | 效果 |
| --- | --- | --- | --- |
| `MOV R0, R1` | `0011 0100` | `0x34` | `R0 <- R1` |
| `MOV R1, R0` | `0011 0001` | `0x31` | `R1 <- R0` |
| `MOV R0, R3` | `0011 1100` | `0x3C` | `R0 <- DI` |
| `MOV R3, R0` | `0011 0011` | `0x33` | `DO <- R0` |
| `MOV R3, R3` | `0011 1111` | `0x3F` | `DI -> DO` |

## R3 的读写握手

读 `R3` 时，如果 `VI=1`，电路在下一个时钟采样 `DI`，同时拉高 `RI`；如果 `VI=0`，目标寄存器保持不变。

写 `R3` 时，如果 `VO=0`，源寄存器写入输出寄存器，并让 `VO=1`；当 `VO=1` 且 `RO=1` 时，本 tick 完成输出，下一 tick 才可以写入新数据。

## 一个简单程序

下面这段程序把外部输入原样送回：

```text
NOP
MOV R0, R3      ; 0011 1100，读入；VI 与 RI 同时为 1 时完成传输
MOV R3, R0      ; 0011 0011，写出；等待 RO 取走后再写下一拍
NOP
```

由于还没有跳转指令，程序会按顺序执行到末尾。测试系统会按固定时序提供输入并读取输出。

每轮开始时，`PC`、`R0`、`R1` 等寄存器都应为 0。

## 文本格式示例

这里的 <color=red>红色文字</color>、<color=blue>蓝色文字</color> 和 <color=#FFAA00>自定义橙色</color> 是行内颜色；也可以写成 <font color=gold>金色文字</font>。

这一段包含 *斜体文本*，也包含 **粗体文本**。

**这一整行是粗体，里面有 <color=red>红色</color>、<color=green>绿色</color> 和 <color=#FFAA00>自定义橙色</color>。**

---

![章节总览](../../textures/gui/experiment_table/chapters/intro/intro.png)

![命名空间图片](elements-plus:textures/gui/experiment_table/chapters/intro/intro.zh_cn.png)

![缺失图片](../../textures/gui/experiment_table/chapters/intro/missing.png)

内嵌组件：`component::{"translate": "item.elements-plus.copper_wire"}`。

## 实验

```epp-experiments
elements-plus:tff
elements-plus:tff_prime
```
