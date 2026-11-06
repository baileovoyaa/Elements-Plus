---
title: "配置"
category: "用户文档"
---

# 配置

Elements Plus 的配置分为两类：

- **客户端配置**：只影响本机的界面与操作，保存在客户端运行目录，不随存档或服务器同步。
- **服务端配置**：属于某个存档（世界），影响该存档内的玩法规则；由管理员修改，并会同步给客户端用于界面预览。

## 客户端配置

文件位置：客户端运行目录下的 `config/elements-plus.json`（Fabric 配置目录）。文件不存在时使用默认值，在游戏内修改选项后会生成或更新该文件。

默认内容如下：

```json
{
  "darkMode": true,
  "muted": false,
  "inspect": false,
  "digitalNumberFormat": "UNSIGNED",
  "analogNumberFormat": "UNSIGNED"
}
```

这些选项都可以在**光刻机设计页**的工具栏中直接切换，切换后立即保存；手动修改文件后需要重启客户端才会生效。

| 配置项 | 类型 | 默认值 | 说明 | 修改方式 |
| --- | --- | --- | --- | --- |
| `darkMode` | 布尔 | `true` | 光刻机设计页电路图区域的深色主题。 | 光刻机设计页工具栏的「明暗切换」按钮（太阳/月亮图标）。 |
| `muted` | 布尔 | `false` | 是否静音电路图编辑器的操作音效，包括放置、移动、擦除元件与导线，以及旋转、镜像等操作的提示音。 | 光刻机设计页工具栏的「静音」按钮（喇叭图标）。 |
| `inspect` | 布尔 | `false` | 「检查」模式：在光刻机设计页悬停元件或导线时，显示详细信息。元件会显示名称、旋转/镜像状态、输入与输出引脚及其实时信号值，自定义元件还会显示 UUID 与作者；导线会显示铜/金材质及其通道信号值。 | 光刻机设计页工具栏的「检查」按钮。 |
| `digitalNumberFormat` | 枚举 | `UNSIGNED` | 8 位总线的数字显示格式，影响检查提示、存储器编辑器和数值输入框等处的显示。 | 光刻机设计页工具栏的总线格式按钮；实验桌中也有入口。点击切换到下一项，按住 Shift 点击切换到上一项。 |
| `analogNumberFormat` | 枚举 | `UNSIGNED` | 1 位信号强度（0~15）的数字显示格式。 | 光刻机设计页工具栏的信号强度格式按钮；实验桌中也有入口。操作同上。 |

`digitalNumberFormat` / `analogNumberFormat` 的取值：

| 值 | 8 位总线显示 | 信号强度显示 | 说明 |
| --- | --- | --- | --- |
| `UNSIGNED` | `0`~`255` | `0`~`15` | 无符号十进制，默认值。 |
| `COMPLEMENT` | `-128`~`127` | `-8`~`7` | 补码表示。 |
| `HEX` | `00`~`FF` | `0`~`F` | 十六进制，总线固定两位，信号强度一位。 |

> [!NOTE]
> 客户端配置只对当前客户端生效，服务器无法强制修改，也不会同步给其他玩家。

## 服务端配置

文件位置：存档目录下的 `serverconfig/elements-plus-server.json`，例如 `saves/<存档名>/serverconfig/elements-plus-server.json`；专用服务器位于世界目录下的同名路径。首次加载存档时会自动创建默认配置文件，旧文件中缺失的配置项会在加载时自动补全。

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `custom_component_nesting_limit` | 整数 | `1` | 自定义元件的规模嵌套限制：父电路规模至少要比被放置的自定义元件大多少级。规模由小到大依次为小、中、大、超大。`1` 表示必须大一级（例如小规模元件只能放进中、大、超大电路），`0` 允许同级，负数允许放进规模更小的电路，`4` 及以上会禁止所有有限规模之间的嵌套。 |
| `allow_infinite_in_finite_circuit` | 布尔 | `false` | 是否允许把无限规模的自定义元件放入有限规模的电路。无论此项如何，任何元件都可以放入无限规模电路。 |
| `async_experiment_tests` | 布尔 | `false` | 实验测例是否异步运行。`false` 时测例随服务器 tick 同步推进；`true` 时在独立线程全速运行，不阻塞主循环，每 tick 最多同步一次进度。适合测例数量很多的实验。 |
| `allow_survival_creative_component` | 布尔 | `false` | 是否允许生存模式玩家使用「创造模式元件」。关闭时只有创造模式玩家可以使用；创造模式元件可以代替任意规模（包括无限规模）的空芯片参与芯片制造，且不需要其他原料。（注：即使开启，生存模式默认无获取创造模式元件途径，仅供特殊玩法需求） |
| `circuit_album_capacity` | 整数 | `50` | 电路图册最多可保存的电路数量。达到上限后无法继续插入；调低该值不会移除图册中已有的电路，只会阻止继续插入。`0` 表示不允许插入。 |

默认内容如下：

```json
{
  "custom_component_nesting_limit": 1,
  "allow_infinite_in_finite_circuit": false,
  "async_experiment_tests": false,
  "allow_survival_creative_component": false,
  "circuit_album_capacity": 50
}
```

### 修改服务端配置

- **游戏内/控制台**：使用命令 `/elements-plus config`（列出全部配置项）、`/elements-plus config <配置项>`（查询）或 `/elements-plus config <配置项> <值>`（修改）。需要权限等级 2（管理员/OP）。修改会立即生效、写入存档配置文件，并同步给所有在线客户端。
- **手动编辑**：直接修改 `serverconfig/elements-plus-server.json` 后重启服务器或重新加载存档生效。`custom_component_nesting_limit` 和 `circuit_album_capacity` 为整数，其余配置项为 `true` / `false`。
- 客户端加入服务器时会收到一份配置副本，仅用于放置或编译自定义元件时在界面上提前显示校验结果；实际以服务端校验为准。

命令的完整用法见[命令](commands.md)。
