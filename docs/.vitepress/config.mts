import {defineConfig} from 'vitepress'
import type {DefaultTheme} from 'vitepress'
import {experimentsPlugin} from './experiments.mjs'
import {spoilerPlugin} from './spoiler.mjs'

/** 站点部署路径，实验块的链接和图标地址都要带上它 */
const base = '/Elements-Plus/'

/** 用户文档侧边栏 */
const userSidebar: DefaultTheme.SidebarItem[] = [
    {
        text: '开始',
        items: [
            {text: '概览', link: '/user/'},
            {text: '快速开始', link: '/user/quick-start'},
            {text: '命令', link: '/user/commands'},
            {text: '配置', link: '/user/configs'}
        ]
    },
    {
        text: '核心机制',
        items: [
            {text: '电路', link: '/user/core/circuit'},
            {text: '实验', link: '/user/core/experiments'}
        ]
    },
    {
        text: '实验内容',
        items: [
            {
                text: '实验章节',
                link: '/user/experiment-chapters/',
                collapsed: false,
                items: [
                    {text: '介绍', link: '/user/experiment-chapters/intro'},
                    {text: '放大器', link: '/user/experiment-chapters/amplifier'},
                    {text: '第一个逻辑门', link: '/user/experiment-chapters/first-gate'},
                    {text: '基础逻辑门', link: '/user/experiment-chapters/basic-gate'},
                    {text: '高级逻辑门', link: '/user/experiment-chapters/advanced-gate'},
                    {text: '三路逻辑门', link: '/user/experiment-chapters/three-way-logic-gate'},
                    {text: '多路选择器', link: '/user/experiment-chapters/multiplexer'},
                    {text: '编码器和译码器', link: '/user/experiment-chapters/encoder-decoder'},
                    {text: '一位加法器', link: '/user/experiment-chapters/one-bit-adder'},
                    {text: '总线', link: '/user/experiment-chapters/bus'},
                    {text: '多位加法器', link: '/user/experiment-chapters/multi-bit-adder'},
                    {text: '位运算器', link: '/user/experiment-chapters/bitwise-operator'},
                    {text: '比较器', link: '/user/experiment-chapters/comparator'},
                    {text: '多位减法', link: '/user/experiment-chapters/multi-bit-subtract'},
                    {text: '位移器', link: '/user/experiment-chapters/bitwise-shifter'},
                    {text: '乘除器', link: '/user/experiment-chapters/multiplier-divider'},
                    {text: '多功能运算器（基础版）', link: '/user/experiment-chapters/alu-basic'},
                    {text: '多功能运算器（高级版）', link: '/user/experiment-chapters/alu-advanced'},
                    {text: '自定义元件', link: '/user/experiment-chapters/custom-component'},
                    {text: '触发器', link: '/user/experiment-chapters/flip-flop'},
                    {text: '寄存器', link: '/user/experiment-chapters/register'},
                    {text: '模数转换器', link: '/user/experiment-chapters/analog-digital-converter'},
                    {text: '计数器', link: '/user/experiment-chapters/counter'},
                    {text: '多字节存储器', link: '/user/experiment-chapters/multi-byte-memory'},
                    {text: 'A+B问题', link: '/user/experiment-chapters/a-plus-b'},
                    {text: '计算机架构-输入输出指令集', link: '/user/experiment-chapters/architecture-iom'},
                    {text: '计算机架构-立即数', link: '/user/experiment-chapters/architecture-immediate'},
                    {text: '计算机架构-连接内存', link: '/user/experiment-chapters/architecture-memory'},
                    {text: '计算机架构-运算', link: '/user/experiment-chapters/architecture-compute'},
                    {text: '计算机架构-跳转', link: '/user/experiment-chapters/architecture-jump'}
                ]
            },
            {text: '自定义实验章节', link: '/user/custom-experiment-chapters'}
        ]
    },
    {
        text: '界面',
        items: [
            {text: '光刻机', link: '/user/ui/lithography-machine'}
        ]
    },
    {
        text: '物品与方块',
        items: [
            {text: '总览', link: '/user/items-and-blocks/'},
            {text: '机器', link: '/user/items-and-blocks/machines'},
            {text: '材料', link: '/user/items-and-blocks/materials'},
            {text: '管道', link: '/user/items-and-blocks/pipes'},
            {text: '杂项', link: '/user/items-and-blocks/misc'}
        ]
    },
    {
        text: '参考',
        items: [
            {text: '配方一览', link: '/user/recipes'},
            {text: '进度一览', link: '/user/advancements'}
        ]
    }
]

/** 开发文档侧边栏 */
const devSidebar: DefaultTheme.SidebarItem[] = [
    {
        text: '构建、执行与部署',
        items: [
            {text: '本地配置 Gradle 发行版镜像', link: '/dev/deploy/gradle-mirror'},
        ],
    },
    { text: '贡献指南', link: '/contributing' },
    {
        text: 'v1.0.0 阶段',
        collapsed: false,
        items: [
            {text: '开发计划 v1.0.0', link: '/dev/phases/v1.0.0/plan'},
            {
                text: '开发阶段索引 v1.0.0',
                link: '/dev/phases/v1.0.0/',
                collapsed: false,
                items: [
                    {
                        text: '电路说明与元件列表',
                        link: '/dev/phases/v1.0.0/001-电路说明与元件列表/电路说明与元件列表'
                    },
                    {
                        text: '关于玩家元件列表',
                        link: '/dev/phases/v1.0.0/002-关于玩家元件列表/关于玩家元件列表'
                    },
                    {
                        text: '实验桌基本功能',
                        link: '/dev/phases/v1.0.0/003-实验桌基本功能/实验桌基本功能'
                    },
                    {
                        text: '光刻机制造页',
                        items: [
                            {
                                text: '光刻机芯片制造功能',
                                link: '/dev/phases/v1.0.0/004-光刻机制造页/光刻机芯片制造功能'
                            },
                            {
                                text: '光刻机电路图复制功能',
                                link: '/dev/phases/v1.0.0/004-光刻机制造页/光刻机电路图复制功能'
                            }
                        ]
                    },
                    {
                        text: '实验桌8位组合实验支持',
                        link: '/dev/phases/v1.0.0/005-实验桌8位组合实验支持/实验桌8位组合实验支持'
                    },
                    {text: '加压机', link: '/dev/phases/v1.0.0/006-加压机/加压机'},
                    {
                        text: '自定义元件与分离存储',
                        link: '/dev/phases/v1.0.0/007-自定义元件与分离存储/自定义元件与分离存储'
                    }
                ]
            }
        ]
    },
    {
        text: '参考',
        items: [
            {text: '参考算法描述', link: '/dev/reference/电路/参考算法描述'},
            {text: '8位指令集', link: '/dev/reference/8位指令集'}
        ]
    },
    {text: '路线图', link: '/dev/roadmap'}
]

/**
 * 按页面 frontmatter 的 `category` 切换的侧边栏。
 * 页面里写 `category: 用户文档` 就用 userSidebar，写 `category: 开发文档` 就用 devSidebar。
 * 由 .vitepress/theme/Layout.vue 读取。
 */
const categorySidebars: Record<string, DefaultTheme.SidebarItem[]> = {
    用户文档: userSidebar,
    开发文档: devSidebar
}

const themeConfig = {
    nav: [
        {text: 'Home', link: '/'},
        {text: '用户文档', link: '/user/'},
        {text: '开发文档', link: '/dev/'}
    ],

    /**
     * 右侧 “On this page” 的标题层级，写成 [最高层级, 最低层级]。
     * VitePress 2 默认只取 h2，这里放开到 h2~h4（'deep' 等价于 [2, 6]）。
     * 单页可以再用 frontmatter 的 `outline` 覆盖。
     */
    outline: {
        level: [2, 4],
        label: 'On this page'
    },

    /**
     * 兜底：没有（或没匹配上）`category` 的页面按路径前缀取侧边栏。
     * 这是 VitePress 原生的多侧边栏写法。
     */
    sidebar: {
        '/user/': userSidebar,
        '/dev/': devSidebar
    } as DefaultTheme.Sidebar,

    categorySidebars,

    socialLinks: [
        {icon: 'github', link: 'https://github.com/baileovoyaa/Elements-Plus'}
    ]
} as DefaultTheme.Config

// https://vitepress.dev/reference/site-config
export default defineConfig({
    base,
    title: 'Elements Plus',
    description: 'Elements Plus docs',
    themeConfig,
    markdown: {
        config(md) {
            // ```epp-experiments 渲染成带图标/可选标志的实验条目
            md.use(experimentsPlugin, {base})
            // ![alt](src){.spoiler} 渲染成可点击的剧透图
            md.use(spoilerPlugin)
        }
    },
    ignoreDeadLinks: true
})
