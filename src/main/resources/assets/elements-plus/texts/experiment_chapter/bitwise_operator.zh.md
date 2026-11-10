位运算器是一种能对总线按位进行某种逻辑运算的元件。包括与、或、非、与非、或非、异或、同或。

除<color=gold>按位非</color>外，都是两个8位输入A和B，一个8位输出Y。其中每一位分别进行对应逻辑运算。

<color=gold>按位非</color>只有一个8位输入A和一个8位输出Y，其中每一位分别取反。

```epp-experiments
elements-plus:bitwise_and
elements-plus:bitwise_or
elements-plus:bitwise_not
elements-plus:bitwise_nand
elements-plus:bitwise_nor
elements-plus:bitwise_xor
elements-plus:bitwise_xnor
```

上述运算是各位并行的。另外你偶尔可能会用到“任意一位非0”或“全非0”之类的判断，或是判断非0位数量是奇数还是偶数，每次都串联多个逻辑门比较麻烦，因此这里也提供8路逻辑门的元件实验。这些是选做的，如果你觉得不需要，可以跳过。

这些元件都有一个8位输入A和一个1位输出Y。

---

- <color=gold>8路或门</color>：输入任意一位为非0，就输出15，否则输出0。
- <color=gold>8路与门</color>：输入所有位同时非0，才输出15，否则输出0。
- <color=gold>8路异或门</color>：输入位奇数个非0，输出15；偶数个非0，输出0。
- <color=gold>8路同或门</color>：输入位奇数个非0，输出0；偶数个非0，输出15。

```epp-experiments
elements-plus:or_gate_8
elements-plus:and_gate_8
elements-plus:xor_gate_8
elements-plus:xnor_gate_8
```
