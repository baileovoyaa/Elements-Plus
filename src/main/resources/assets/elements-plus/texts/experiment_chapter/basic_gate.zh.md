既然已经造出了第一个逻辑门，现在我们要来制作更多的逻辑门了。

这一章节的逻辑门将不再只有一个输入，而是通常有A和B两个输入。

- <color=gold>或门</color>：任意一个输入为非0，就输出15，否则输出0。
- <color=gold>与门</color>：两个输入同时非0，才输出15，否则输出0。
- <color=gold>或非门</color>：任意一个输入为非0，就输出0，否则输出15。
- <color=gold>与非门</color>：两个输入同时非0，才输出0，否则输出15。

```epp-experiments
elements-plus:or_gate
elements-plus:nor_gate
elements-plus:nand_gate
elements-plus:and_gate
```

给你一个提示：与非门和或非门并不总是与门或门加一个非门那么简单，例如，与非门需要的晶体管数量其实比与门更少。

后面的章节，我们将经常用到这些基本逻辑门。

---

另外这一章同样有一个选做挑战：

- <color=gold>短路与</color>：如果A非0，输出B，否则输出A
- <color=gold>短路或</color>：如果A为0，输出B，否则输出A

```epp-experiments
elements-plus:and_gate_sc
elements-plus:or_gate_sc
```
