A bitwise operator is a component that can perform some logical operation on a bus bit by bit. These include AND, OR, NOT, NAND, NOR, XOR and XNOR.

Except for the <color=gold>Bitwise NOT</color>, they all have two 8-bit inputs A and B and one 8-bit output Y, and each bit performs the corresponding logical operation independently.

The <color=gold>Bitwise NOT</color> has only one 8-bit input A and one 8-bit output Y, and each bit is inverted independently.

```epp-experiments
elements-plus:bitwise_and
elements-plus:bitwise_or
elements-plus:bitwise_not
elements-plus:bitwise_nand
elements-plus:bitwise_nor
elements-plus:bitwise_xor
elements-plus:bitwise_xnor
```

The operations above are performed on all bits in parallel. In addition, you may occasionally need judgements such as “any bit is non-zero” or “all bits are non-zero”, or need to know whether the number of non-zero bits is odd or even. Chaining multiple logic gates each time is troublesome, so here we also provide 8-way logic gate component experiments. These are optional; if you feel you don't need them, you can skip them.

These components all have one 8-bit input A and one 1-bit output Y.

---

- <color=gold>8-way OR Gate</color>: If any input bit is non-zero, output 15; otherwise output 0.
- <color=gold>8-way AND Gate</color>: Only when all input bits are non-zero at the same time, output 15; otherwise output 0.
- <color=gold>8-way XOR Gate</color>: If an odd number of input bits are non-zero, output 15; if an even number are non-zero, output 0.
- <color=gold>8-way XNOR Gate</color>: If an odd number of input bits are non-zero, output 0; if an even number are non-zero, output 15.

```epp-experiments
elements-plus:or_gate_8
elements-plus:and_gate_8
elements-plus:xor_gate_8
elements-plus:xnor_gate_8
```
