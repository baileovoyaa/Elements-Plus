---
title: "命令"
category: "用户文档"
---

# 命令

Elements Plus 的所有命令都位于 `/elements-plus` 之下，并且需要**权限等级 2**：单人游戏中需要开启作弊，多人服务器中需要 OP 等级 2 及以上。单独执行 `/elements-plus` 不会有任何效果，必须带上子命令。

约定：尖括号 `<...>` 为必填参数，方括号 `[...]` 为可选参数；`<uuid>` 指已编译电路的 UUID。

## 命令总览

| 命令 | 作用 |
| --- | --- |
| `/elements-plus experiments grant <players> <experiment>` | 将实验标记为已完成 |
| `/elements-plus experiments revoke <players> <experiment>` | 撤销实验的完成标记 |
| `/elements-plus experiments list <player>` | 列出玩家已完成的实验 |
| `/elements-plus circuit give <id> [diagram\|chip]` | 获取电路图（默认）或芯片 |
| `/elements-plus circuit list all` | 列出所有已编译电路 |
| `/elements-plus circuit list player <player>` | 列出某个玩家创建的电路 |
| `/elements-plus circuit list name <name>` | 按名称列出电路 |
| `/elements-plus circuit list id <uuid>` | 显示某个电路的详细信息 |
| `/elements-plus config` | 列出所有服务端配置项 |
| `/elements-plus config <option>` | 查询某个服务端配置项 |
| `/elements-plus config <option> <value>` | 修改某个服务端配置项 |
| `/elements-plus debug` | 开发调试用的临时入口（不受支持） |

## 实验进度：experiments

`experiments` 直接修改玩家的实验完成记录（保存在玩家数据中，死亡后保留），用于跳过或重置实验进度。修改会立即写入玩家数据，并影响实验桌中章节的解锁状态；重新打开实验桌即可看到变化。

`<experiment>` 参数为实验的注册名，例如 `and_gate`、`adder_8`、`counter_8`。输入时可以使用 Tab 补全查看全部可选值；`*` 表示全部内置实验。

### `/elements-plus experiments grant <players> <experiment>`

把一个或多个玩家的指定实验标记为已完成。

- `<players>`：目标玩家，支持 `@a`、`@p` 等实体选择器，可一次指定多名在线玩家。
- `<experiment>`：实验注册名，或 `*`（全部实验）。

```
/elements-plus experiments grant @s and_gate
/elements-plus experiments grant @a *
```

### `/elements-plus experiments revoke <players> <experiment>`

与 `grant` 相反，撤销一个或多个玩家的实验完成标记。参数含义相同，`*` 表示撤销全部实验。

### `/elements-plus experiments list <player>`

列出指定玩家已完成的实验（显示为本地化的实验名，以逗号分隔）。该玩家尚未完成任何实验时会给出提示。目标玩家需要在线。

```
/elements-plus experiments list Steve
```

## 电路：circuit

已编译电路是实验桌完成实验、以及编译自定义元件后保存在存档中的电路记录（存档目录 `data/elements-plus/circuits/<uuid>.dat`）。电路图物品只保存该记录的 UUID 引用；芯片物品记录对应的元件种类，自定义元件芯片还会记录电路 UUID。

### `/elements-plus circuit give <id> [diagram|chip]`

获取一条已编译电路对应的物品。

- `<id>`：电路 UUID 或名称。**UUID 优先**；不是 UUID 时按名称查找：
    - 内置元件使用注册名，如 `and_gate`，也接受完整 id `elements-plus:and_gate`。
    - 自定义元件使用创建时填写的名称，允许重名。
    - 名称区分大小写；名称包含空格时需要写成 `"..."`（例如 `"自动 开关"`）；同名者有多个时会报错，并列出每个同名电路的名称、作者和 UUID，点击其中的 UUID 可以直接执行对应命令（请求 `chip` 且同名者全部是内置元件时不会报错，见下文）。
- `diagram`（默认）：获得引用该电路的电路图物品。
- `chip`：获得芯片。
    - 内置元件：固定获得小芯片（`small_chip`）。内置元件的芯片只记录元件种类、不绑定具体电路，因此同一个元件的多条已编译电路完全等价；按名称查到多条内置电路时不会报错，直接取第一条。
    - 自定义元件：按该电路图的规模获得小、中、大或超大芯片（`small_chip` / `medium_chip` / `large_chip` / `huge_chip`），并绑定该电路的 UUID，以便区分同名元件。
    - 无法生成芯片时会给出提示，例如电路规模为无限、或记录缺少元件信息。

该命令直接给予物品，不消耗原料，区别于光刻机的芯片制造。物品会放进执行命令的玩家背包，背包已满时掉落在脚下，因此需要由游戏内的玩家执行（控制台无法执行）。

```
/elements-plus circuit give and_gate
/elements-plus circuit give 5f0c1f6e-0c2b-4f1e-9a4c-3d2b1a0e9f11 chip
/elements-plus circuit give 自动开关门控制器 diagram
```

> [!NOTE]
> 请求芯片时，如果同名电路的列表被列出，列表中的 UUID 点击后会执行带 `chip` 的命令，点击即可获得对应元件的芯片；`circuit list` 列出的 UUID 点击后仍是获取电路图。

### `/elements-plus circuit list all`

列出所有已编译电路，每条显示名称、作者和 UUID（UUID 可点击获取电路图）。

### `/elements-plus circuit list player <player>`

列出指定玩家作为作者编译的电路。

```
/elements-plus circuit list player Steve
```

### `/elements-plus circuit list name <name>`

按名称列出电路。内置元件用注册名（如 `and_gate`），自定义元件用创建时填写的名称。

```
/elements-plus circuit list name and_gate
```

### `/elements-plus circuit list id <uuid>`

只显示某个电路的详细信息（名称、作者、UUID、规模），不给予任何物品。

```
/elements-plus circuit list id 5f0c1f6e-0c2b-4f1e-9a4c-3d2b1a0e9f11
```

## 服务端配置：config

`config` 用于查看和修改当前存档的服务端配置，配置项的含义见[配置](configs.md)。配置项名称可以用 Tab 补全；布尔项会自动提示 `true` / `false`。

### `/elements-plus config`

列出全部服务端配置项的当前值及说明。

### `/elements-plus config <option>`

查询单个配置项的当前值。

```
/elements-plus config async_experiment_tests
```

### `/elements-plus config <option> <value>`

修改配置项并立即保存、生效，同时把新配置同步给所有在线客户端。

- `custom_component_nesting_limit`：整数。
- 其余配置项：`true` 或 `false`。

```
/elements-plus config allow_infinite_in_finite_circuit true
/elements-plus config custom_component_nesting_limit 0
```

修改失败（例如值不是合法整数/布尔值，或文件写入失败）时会给出相应提示，配置保持原值。

## 开发调试：debug

### `/elements-plus debug`

开发调试用的临时入口。它只是在开发过程中临时挂载调试语句的占位子命令，可能输出一些调试信息（具体内容随开发需要变化），没有稳定的、面向玩家的功能。

> [!WARNING]
> 该子命令的行为是临时的、未定义的，随时可能改变或移除，不属于正式功能，也不保证对游戏产生任何实际效果。将来可能会扩充调试功能，但这不在目前的计划中。请不要在正常游戏、地图或自动化流程中依赖它。
