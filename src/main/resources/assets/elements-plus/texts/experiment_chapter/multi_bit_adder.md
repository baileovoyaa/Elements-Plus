Now we will implement the addition of multi-bit binary numbers.

---

In the previous chapter, you already built a component that can handle the addition of one-bit binary numbers. We can recall the process of adding by columns: the carry is placed in the higher position as an addend. This also applies to binary.

In this experiment, we will build an “<color=gold>8-bit Adder</color>”: it takes two 8-bit binary numbers and a carry input, and outputs an 8-bit binary sum and a 1-bit carry output.

- <color=gold>8-bit Adder</color>: Inputs A (8-bit), B (8-bit), CI (1-bit). Treat them as binary numbers and add them, outputting the sum S (8-bit) and carry output CO (1-bit).

```epp-experiments
elements-plus:adder_8
```
