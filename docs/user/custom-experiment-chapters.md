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

旧式段落章节的文字仍然需要语言文件，而语言文件属于**资源包**，不属于数据包：

```text
my_resourcepack/
├─ pack.mcmeta
└─ assets/
   ├─ my_chapters/lang/
   │  ├─ zh_cn.json
   │  └─ en_us.json
   └─ my_chapters/texts/experiment_chapter/
      └─ hello_logic.md
```

语言键本身是全局解析的，不要求语言文件所在命名空间与章节命名空间相同。如果你的自定义章节直接复用现成语言键，则不需要额外语言文件；Markdown 驱动的章节则把正文放在资源包的 `texts` 路径下。

## 章节 JSON

### Markdown 驱动章节

章节可以把正文交给资源包中的 Markdown 文件。数据包 JSON 只负责资源路径、允许的特殊文本提供器和实验列表：

```json
{
  "dependencies": ["elements-plus:a_plus_b"],
  "markdown": "my_chapters:texts/experiment_chapter/hello_logic.md",
  "special_text": [],
  "experiments": [
    "elements-plus:and_gate"
  ],
  "optional": [
    "elements-plus:not_gate"
  ]
}
```

`markdown` 使用资源位置（命名空间 + 路径），实际文件位于：

```text
assets/<命名空间>/<路径>
```

例如 `my_chapters:texts/experiment_chapter/hello_logic.md` 对应：

```text
assets/my_chapters/texts/experiment_chapter/hello_logic.md
assets/my_chapters/texts/experiment_chapter/hello_logic.zh.md
```

默认文件是不带语言后缀的 `.md`；当前语言为 `zh_cn` 时会优先查找 `.zh.md`，再查找完整语言代码 `.zh_cn.md`，最后回退到默认文件。资源包可以分别覆盖不同语言的 Markdown。

Markdown 的空行表示段落分隔，连续空行只会分隔一次；同一段落内的普通换行按空格合并，行尾两个空格表示强制换行。Markdown 支持标题、表格、行内代码和代码块。实验块使用 `epp-experiments` 代码块，每行写一个实验 ID：

````text
```epp-experiments
elements-plus:and_gate
elements-plus:not_gate
```
````

Markdown 中的实验 ID 必须同时出现在数据包 JSON 的 `experiments` 或 `optional` 中，否则不会显示。可选状态由数据包 JSON 决定，Markdown 不写可选标记。数据包中定义但 Markdown 没有提到的实验，会按 JSON 中的顺序追加到章节末尾。

特殊文本使用 `epp-text` 代码块，块内写提供器 ID；只有同时列在 JSON 的 `special_text` 中才会显示：

````text
```epp-text
elements-plus:rom_values
```
````

### 旧式段落结构

旧章节仍然可以使用 `sections`：

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
| `markdown` | 否 | Markdown 资源位置。存在时使用 Markdown 驱动章节。 |
| `special_text` | 否 | Markdown 中允许使用的特殊文本提供器 ID 列表。 |
| `experiments` | 否 | 本章实验 ID 列表；Markdown 中的实验 ID 必须在这里出现。 |
| `optional` | 否 | `experiments` 中的可选实验 ID 列表。 |
| `sections` | 否 | 旧式段落列表。没有 `markdown` 时使用。 |

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

#### 富文本行

章节文本行还支持一组轻量的 Markdown 风格格式。它们仍然按普通文本行填写，因此代码块和表格可以跨连续的多个语言键。

标题行以 1~6 个 `#` 加空格开头，显示为粗体：

```text
## 标题
```

行内代码使用一对反引号，会跟随普通文字换行，并带深色背景：

```text
这一段里有 `行内代码`。
```

如果行内代码以 `component::` 开头，后面的内容会作为 JSON 文本组件解析并嵌入正文：

```text
`component::{"translate": "item.elements-plus.copper_wire"}`
```

行内粗体使用 `**粗体**`，斜体使用 `*斜体*`。行内颜色可以使用 Minecraft 颜色名或十六进制颜色：

```text
这是 **粗体**，这是 *斜体*，这是 <color=red>红色</color>。

也可以写成 <font color=#FFAA00>自定义橙色</font>。
```

可用的颜色名与 Minecraft 格式颜色一致，例如 `red`、`blue`、`gold`、`green`、`aqua`、`light_purple`、`white`；十六进制颜色写作 `#RRGGBB`。

表格使用连续的 `|` 行，第一行是表头，第二行是分隔行：

```text
| 名称 | 位宽 | 说明 |
| --- | --- | --- |
| `PC` | 8 | 程序地址 |
| `INS` | 8 | 指令字节 |
```

代码块以三个反引号开始和结束：

```text
MOV R0, R3
MOV R3, R0
```

表格和代码块超过可用宽度时会各自出现横向滚动条。鼠标滚轮在它们上面时仍然纵向滚动整个章节；按住 `Shift` 滚动滚轮或拖动底部滚动条可以横向查看。

代码块中间的行不会自动换行，也不会额外增加行间距。行内代码只使用一对反引号。

单独一行 `---` 会显示为分隔线。

块级图片使用 `![图片描述](图片路径)`。路径可以相对于当前 Markdown 文件所在目录、使用 `../` 回退，或使用完整的命名空间路径：

```text
![章节总览](../../textures/gui/experiment_table/chapters/intro/intro.png)
![命名空间图片](elements-plus:textures/gui/experiment_table/chapters/intro/intro.png)
```

相对路径不能越过 `assets/<命名空间>/` 根目录。图片不存在时，章节会显示 16×16 的缺失图标和图片描述，并把解析后的资源路径写入日志。

行内 JSON 文本组件使用带 `component::` 前缀的行内代码：

```text
`component::{"translate": "item.elements-plus.copper_wire"}`
```

内置的“计算机架构——输入输出寄存器”章节包含这几种格式的示例。

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
