电容器可以将信号储存1tick，但有时我们会想把信号储存更久。

因此就需要一种能持久保存信号的元件——寄存器。

首先是<color=gold>1位寄存器</color>。它有一个“写选通”，当写选通启用（非0）时，会存储输入的值，写选通未启用时，则会保持已存储的值。注意存储的是放大值而不是原始信号强度。

- <color=gold>1位寄存器</color>：输入A和WE，输出Y。当时钟步进时，若WE非0，Y会变为A的放大值，否则Y保持不变。

```epp-experiments
elements-plus:register
```

8位寄存器是其对应的8位版。

- <color=gold>8位寄存器</color>：输入A（8位）和WE（1位），输出Y（8位）。当时钟步进时，若WE非0，Y会变为A的逐通道放大值，否则Y保持不变。

```epp-experiments
elements-plus:register_8
```

还有一种可以保存模拟信号强度的“<color=gold>模拟寄存器</color>”，它是一种“采样/保持电路”，可以根据WE是否为0来决定是采样还是保持。

- <color=gold>模拟寄存器</color>：输入A和WE，输出Y。当时钟步进时，若WE非0，Y会变为A的信号强度，否则Y保持不变。

```epp-experiments
elements-plus:register_analog
```
