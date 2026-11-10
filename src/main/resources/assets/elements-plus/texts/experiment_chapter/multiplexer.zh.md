<color=gold>多路选择器</color>可以在多条输入中选择一个作为输出，这在后面设计多功能运算器时很有帮助。

<color=red>注意：由于这部分电路逻辑较复杂，信号强度难以保留，故下面的实验涉及的输入D、D0、D1、D2、D3都只考虑0和非0两种情况，非0即视为15，而不是输出原始信号强度。下面称为“<color=blue>放大值</color>”。</color>

首先是<color=gold>2选1多路选择器</color>：输入A、D0、D1，输入A为0时输出Y为D0的放大值，输入A为非0时输出Y为D1的放大值。

如果你还不清楚怎么做，可以想想，在我们做的<color=blue>基础逻辑门</color>里，什么元件的作用最像一个“开关”？

```epp-experiments
elements-plus:mux_2
```

然后我们把输入扩展到4条，就有了<color=gold>4选1多路选择器</color>。

输入A0、A1、D0、D1、D2、D3，输出Y。

- 当A1为0，A0为0时，输出D0的放大值。
- 当A1为0，A0为非0时，输出D1的放大值。
- 当A1为非0，A0为0时，输出D2的放大值。
- 当A1为非0，A0为非0时，输出D3的放大值。

提示：四选一，就相当于先四选二再二选一。

```epp-experiments
elements-plus:mux_4
```

然后我们再堆叠8条线，就有了<color=gold>8位多路选择器</color>。它可以对<color=blue>8位总线</color>进行选择。

<color=dark_purple>还不了解<color=blue>总线</color>的概念？可以先完成“<color=gold>多位加法器</color>”章节后再来。</color>

8位多路选择器的特性和1位的一样，只是输入D0~D4和输出Y变成了<color=blue>8位</color>。

```epp-experiments
elements-plus:mux_2_8
elements-plus:mux_4_8
```

还有一个比较实用的元件是“<color=gold>8位三态缓冲器</color>”，它的作用类似开关，仅在“启用”时会输出信号，否则输出0。因此通过这个元件可以轻松的将多个数据输出接到同一条共享总线上，只要确保任意时刻只有单个有效，数据就不会互相干扰。虽然使用开头所述的元件的8位运算对应版本也能实现，但稍微繁琐。封装为单个元件可以简化逻辑。

它有两个输入：使能输入A和数据输入D，一个输出Y。其中D和Y都是<color=blue>8位</color>。当A为非0时，输出Y为D的放大值；当A为0时，输出Y为全0。

```epp-experiments
elements-plus:tri_state_buffer_8
```

另外或许你也想尝试能够保留原始信号强度的“<color=gold>模拟多路选择器</color>”，这里作为可选实验留给你。完成“<color=blue>基础逻辑门</color>”中的可选实验“<color=gold>短路与</color>”和“<color=gold>短路或</color>”后再做这个可能会更容易一些。

```epp-experiments
elements-plus:mux_analog_2
elements-plus:mux_analog_4
elements-plus:mux_analog_2_8
elements-plus:mux_analog_4_8
elements-plus:tri_state_buffer_analog_8
```
