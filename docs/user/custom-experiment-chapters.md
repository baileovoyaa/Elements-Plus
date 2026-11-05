---
title: "自定义实验章节"
category: "用户文档"
---

# 自定义实验章节

实验章节的内容、段落、参数、实验块、特殊块和前置依赖现在都由数据包驱动，不再写死在模组代码中。

本文面向：

- 想修改章节内容或顺序的玩家；
- 想加入自己章节的自定义数据包作者；
- 想统一调整服务器实验流程的管理员。

关于数据包和资源包的基本结构和更多细节可参见 [Minecraft Wiki](https://zh.minecraft.wiki/w/%E6%95%B0%E6%8D%AE%E5%8C%85)，这里不再赘述。

## 文件位置

按普通数据包安装（数据包根目录需要有 `pack.mcmeta`）。

章节定义放在：

```text
data/<命名空间>/elements-plus/experiment_chapter/<章节名>.json
```

其中 `<命名空间>` 推荐使用你自己的命名空间，例如 `my_chapters`：

```text
data/my_chapters/elements-plus/experiment_chapter/hello_logic.json
```

这会得到一个章节 ID：

```text
my_chapters:hello_logic
```

只有需要**覆盖内置章节**时，才建议把文件放进 `elements-plus` 命名空间，并使用与内置章节相同的 ID，例如：

```text
data/elements-plus/elements-plus/experiment_chapter/first_gate.json
```

章节顺序由标签文件决定：

```text
data/elements-plus/tags/elements-plus/experiment_chapter/index.json
```

注意：章节定义可以使用任何命名空间，但这个**标签文件本身的命名空间固定为 `elements-plus`**，不要改成自定义命名空间。

一个典型的**数据包**结构：

```text
my_datapack/
├─ pack.mcmeta
└─ data/
   ├─ my_chapters/
   │  └─ elements-plus/experiment_chapter/hello_logic.json
   └─ elements-plus/
      └─ tags/elements-plus/experiment_chapter/index.json
```

章节文字仍然需要语言文件，而语言文件属于**资源包**，不属于数据包：

```text
my_resourcepack/
├─ pack.mcmeta
└─ assets/
   └─ my_chapters/lang/
      ├─ zh_cn.json
      └─ en_us.json
```

语言键本身是全局解析的，不要求语言文件所在命名空间与章节命名空间相同。如果你的自定义章节直接复用现成语言键，则不需要额外资源包。

## 章节 JSON

一个章节的基本结构：

```json
{
  "dependencies": [
    "elements-plus:intro"
  ],
  "sections": [
    {
      "type": "text",
      "section": 0,
      "lines": 2,
      "args": [
        {
          "line": 1,
          "params": [
            {
              "style": ["gold"]
            }
          ]
        }
      ]
    },
    {
      "type": "experiments",
      "experiments": [
        "elements-plus:and_gate"
      ],
      "optional": [
        "elements-plus:not_gate"
      ]
    }
  ]
}
```

### 顶层字段

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `dependencies` | 否 | 前置章节 ID 列表。所有前置章节完成后，此章节才会解锁。 |
| `sections` | 是 | 章节段落列表，按数组顺序显示。 |

依赖使用完整章节 ID。例如自定义章节依赖内置介绍章节：

```json
"dependencies": ["elements-plus:intro"]
```

自定义章节之间的依赖：

```json
"dependencies": ["my_chapters:hello_logic"]
```

依赖关系应保持无环。

## 段落类型

### text：文本段落

```json
{
  "type": "text",
  "section": 0,
  "lines": 3,
  "args": [
    {
      "line": 1,
      "params": [
        {
          "style": ["gold"]
        }
      ]
    },
    {
      "line": 2,
      "style": ["bold"],
      "params": [
        {
          "component": {
            "translate": "item.elements-plus.copper_wire"
          }
        }
      ]
    }
  ]
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `section` | 是 | 文本段落编号，用来组成语言键。不同段落可以使用 0、1、2……等编号。 |
| `lines` | 是 | 这个段落的行数。 |
| `args` | 否 | 只列出需要额外样式或参数的行。 |

章节文字本身仍然放在语言文件中，而不是章节 JSON 中。这样自定义章节也可以正常支持中英文。

`args.line` 和语言键中的行号都从 `0` 开始。

文本语言键规则：

```text
experiment.elements-plus.group.<章节名称>.section<段落编号>.text<行号>
```

对于自定义命名空间章节 `my_chapters:hello_logic`，章节名称使用完整 ID：

```text
experiment.elements-plus.group.my_chapters:hello_logic.section0.text0
experiment.elements-plus.group.my_chapters:hello_logic.section0.text1
experiment.elements-plus.group.my_chapters:hello_logic.section0.text2
```

章节标题键：

```text
experiment.elements-plus.group.my_chapters:hello_logic
```

语言文件需要放在资源包中：

```text
assets/my_chapters/lang/zh_cn.json
assets/my_chapters/lang/en_us.json
```

这是 Minecraft 的常规语言文件格式，语言键本身不要求属于当前资源命名空间。

#### 行样式

`args` 中每一项对应一行：

```json
{
  "line": 2,
  "style": ["bold", "italic"]
}
```

`style` 会应用到整行。可用名称就是 Minecraft 的格式名称，例如：

```text
bold italic underline strikethrough obfuscated reset
black dark_blue dark_green dark_aqua dark_red dark_purple
gold gray dark_gray blue green aqua red light_purple yellow white
```

#### 行内参数

`params` 按顺序对应这一行里的 `%s` 占位符：

```json
{
  "line": 1,
  "params": [
    { "style": ["gold"] },
    { "style": ["blue", "italic"] }
  ]
}
```

其中 `style` 参数会使用自动派生的参数语言键：

```text
experiment.elements-plus.group.<章节名称>.section<段落编号>.text<行号>.arg<参数序号>
```

`arg` 序号从 0 开始，并且按 `params` 数组中的位置计数。即使某个参数使用下面的 `component` 形式，它也会占据一个位置。

文本行示例：

```json
"experiment.elements-plus.group.my_chapters:hello_logic.section0.text1": "这一行显示 %s 和 %s。",
"experiment.elements-plus.group.my_chapters:hello_logic.section0.text1.arg0": "金色文字",
"experiment.elements-plus.group.my_chapters:hello_logic.section0.text1.arg1": "蓝色斜体文字"
```

#### 直接嵌入组件参数

如果参数需要直接显示一个组件，例如物品显示名，可以使用 `component`：

```json
{
  "line": 2,
  "params": [
    {
      "component": {
        "translate": "item.elements-plus.copper_wire"
      }
    }
  ]
}
```

`component` 使用 Minecraft 的文本组件 JSON 格式。它不会生成 `.argN` 语言键，但仍会占据参数位置。

### experiments：实验块

```json
{
  "type": "experiments",
  "experiments": [
    "elements-plus:and_gate",
    "elements-plus:or_gate"
  ],
  "optional": [
    "elements-plus:not_gate"
  ]
}
```

| 字段 | 说明 |
| --- | --- |
| `experiments` | 该段落中显示并计入章节完成条件的实验。 |
| `optional` | 选做实验，不计入章节的必做完成条件。 |

一个章节的完成条件，是该章节所有 `experiments` 中的实验都已完成；`optional` 不参与章节完成判定，但参与“全部实验”类进度。

实验 ID 使用完整资源位置。当前章节系统只支持模组内置实验；实验 ID 可以查看 [experiments.md](experiments.md)，或查看现有章节 JSON。

### image：图片段落

```json
{
  "type": "image",
  "image": "elements-plus:textures/gui/experiment_table/chapters/intro/intro.png",
  "width": 3167,
  "height": 1061
}
```

`width` / `height` 是原始图片尺寸，界面会按可用宽度缩放。

### dynamic_text：特殊动态文本块

```json
{
  "type": "dynamic_text",
  "provider": "elements-plus:rom_values"
}
```

目前支持的特殊文本提供器：

| provider | 说明 |
| --- | --- |
| `elements-plus:rom_values` | 根据当前存档世界种子生成 ROM 地址 0~15 的随机字节表。 |

动态文本块会跟随世界种子变化，不需要在语言文件中逐项维护。

## 章节顺序

在数据包中创建：

```text
data/elements-plus/tags/elements-plus/experiment_chapter/index.json
```

内容示例：

```json
{
  "replace": false,
  "values": [
    "my_chapters:hello_logic"
  ]
}
```

`values` 使用完整章节 ID，并按你希望显示的先后顺序排列。

### 追加到内置章节之后

使用 `"replace": false` 并只写自定义章节，通常会把自定义章节追加到内置章节标签的值之后：

```json
{
  "replace": false,
  "values": [
    "my_chapters:hello_logic",
    "my_chapters:another_chapter"
  ]
}
```

### 完全控制顺序

如果需要把章节插入内置章节中间，或确保顺序不受其他数据包影响，可以使用 `"replace": true`，并在这个列表中写出**所有**章节 ID，包括内置章节和自定义章节：

```json
{
  "replace": true,
  "values": [
    "elements-plus:intro",
    "elements-plus:amplifier",
    "my_chapters:hello_logic",
    "elements-plus:first_gate",
    "elements-plus:basic_gate"
  ]
}
```

没有列入 `values` 的章节定义虽然可能仍会从注册表加载，但不建议依赖未列入索引的顺序。

## 完整示例

下面添加一个自定义章节 `my_chapters:hello_logic`，依赖介绍章节，包含一段文字和一个实验块。

### 1. 章节定义

`data/my_chapters/elements-plus/experiment_chapter/hello_logic.json`：

```json
{
  "dependencies": [
    "elements-plus:intro"
  ],
  "sections": [
    {
      "type": "text",
      "section": 0,
      "lines": 3,
      "args": [
        {
          "line": 1,
          "params": [
            {
              "style": ["gold"]
            }
          ]
        },
        {
          "line": 2,
          "style": ["bold"],
          "params": [
            {
              "component": {
                "translate": "item.elements-plus.copper_wire"
              }
            }
          ]
        }
      ]
    },
    {
      "type": "experiments",
      "experiments": [
        "elements-plus:and_gate"
      ]
    }
  ]
}
```

### 2. 语言文件（资源包）

`assets/my_chapters/lang/zh_cn.json`：

```json
{
  "experiment.elements-plus.group.my_chapters:hello_logic": "我的逻辑章节",
  "experiment.elements-plus.group.my_chapters:hello_logic.section0.text0": "这是自定义章节的第一行。",
  "experiment.elements-plus.group.my_chapters:hello_logic.section0.text1": "这一行有一个 %s 参数。",
  "experiment.elements-plus.group.my_chapters:hello_logic.section0.text1.arg0": "金色文字",
  "experiment.elements-plus.group.my_chapters:hello_logic.section0.text2": "这里显示物品：%s"
}
```

英文语言文件同理，放在 `assets/my_chapters/lang/en_us.json`。

### 3. 章节顺序

`data/elements-plus/tags/elements-plus/experiment_chapter/index.json`：

```json
{
  "replace": false,
  "values": [
    "my_chapters:hello_logic"
  ]
}
```

### 4. 生效

完成数据包修改后：

- 服务器执行 `/reload`，或重启服务器；
- 多人游戏中建议重新连接客户端，以确保同步到最新章节数据；
- 服务端启动日志应出现：

```text
Loaded <数量> data-driven experiment chapters
```

数量应与你实际加载的章节数一致。

## 覆盖内置章节

如果需要修改内置章节内容而不是新增章节：

1. 把章节文件放到 `elements-plus` 命名空间；
2. 使用与内置章节相同的文件路径/ID；
3. 保持章节 ID 不变，以继续使用现有语言键、依赖和玩家进度。

例如覆盖第一个逻辑门章节：

```text
data/elements-plus/elements-plus/experiment_chapter/first_gate.json
```

如果只是新增章节，请使用自己的命名空间，避免与内置章节 ID 冲突。

## 注意事项

- 章节 JSON 负责“结构”和“参数”，具体文字仍由语言文件提供。
- `lines` 必须与语言文件中的 `text` 键数量一致。
- `params` 的顺序必须与文本中的 `%s` 顺序一致。
- `dependencies` 和 `experiments` 使用完整 ID，例如 `elements-plus:intro`、`elements-plus:and_gate`。
- 不建议建立循环依赖。
- 当前实验块只引用内置实验；暂不支持在数据包中定义新的实验逻辑。
- 目前动态文本只支持 `elements-plus:rom_values` 提供器。
