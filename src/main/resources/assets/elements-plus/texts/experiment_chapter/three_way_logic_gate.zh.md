之前我们做出来了一些逻辑门，而有时我们需要处理的数据是三个或以上。串联两个元件可以实现，但我们也可以集成到一个高效的元件里。

本次我们要做的是“3路”逻辑门，输入A、B、C，输出Y：

- <color=gold>三路或门</color>：任意一个输入为非0，就输出15，否则输出0。
- <color=gold>三路与门</color>：三个输入同时非0，才输出15，否则输出0。
- <color=gold>三路异或门</color>：奇数个输入非0，输出15；偶数个输入非0，输出0。
- <color=gold>三路同或门</color>：奇数个输入非0，输出0；偶数个输入非0，输出15。

这一章是可选的，你可以选择直接进行后续章节，不过先完成这一章可能会对你后续有所帮助。

```epp-experiments
elements-plus:or_gate_3
elements-plus:and_gate_3
elements-plus:xor_gate_3
elements-plus:xnor_gate_3
```
