**Note: both experiments below use unsigned numbers.**

A <color=gold>multiplier</color> can multiply two numbers and output their product. Since the product of two 8-bit numbers may exceed 8 bits, the high 8 bits and the low 8 bits are output separately.

There are usually several ways to implement a <color=gold>multiplier</color>. Its logic can be somewhat complex, and you may skip it, but then you will not be able to build the advanced ALU. By comparison, the basic version only lacks multiplication, which does not affect the later construction of a computer architecture.

---

- <color=gold>Multiplier (Simple)</color>: Inputs A (8-bit) and B (8-bit); outputs Y0, containing only the low 8 bits of A*B.
- <color=gold>Multiplier</color>: Inputs A (8-bit) and B (8-bit); outputs Y0 (8-bit) and Y1 (8-bit). Y0 and Y1 pack the 16-bit result of <color=gold>A\*B</color>, where Y0 is the low 8 bits and Y1 is the high 8 bits.

```epp-experiments
elements-plus:multiplier_lite
elements-plus:multiplier
```

By the congruence property of multiplication, Y0 is exactly the signed product of A and B as long as no overflow occurs.

---

A combinational-logic divider is theoretically possible but not recommended: its area and component count are huge, and even if you draw the circuit, it is expensive. It is better to use <color=blue>sequential logic</color> and implement it over multiple clock cycles.

If you want to try it, you can do the experiment below, but note that this experiment is **purely combinational**: you must produce the result within the same clock cycle and cannot use sequential components.

---

- <color=gold>Divider</color>: Inputs A (8-bit) and B (8-bit); outputs the integer part Q (8-bit) and remainder R (8-bit) of A/B.

By convention, when the divisor is 0, both Q and R output 0.

```epp-experiments
elements-plus:divider
```
