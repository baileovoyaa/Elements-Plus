We have already built some logic gates, and sometimes we need to handle data that is three or more. By connecting two components in series, we can achieve this, but we can also integrate it into a more efficient component.

In this experiment, we will build a three-way logic gate, which takes inputs A, B, and C, and produces an output Y:

- <color=gold>Three-way OR Gate</color>: If any one of the inputs is non-zero, it outputs 15, otherwise it outputs 0.
- <color=gold>Three-way AND Gate</color>: If all three inputs are non-zero, it outputs 15, otherwise it outputs 0.
- <color=gold>Three-way XOR Gate</color>: If an odd number of inputs are non-zero, it outputs 15; if an even number of inputs are non-zero, it outputs 0.
- <color=gold>Three-way XNOR Gate</color>: If an odd number of inputs are non-zero, it outputs 0; if an even number of inputs are non-zero, it outputs 15.

This chapter is optional, you can choose to directly proceed to the next chapter, but completing this chapter first may be helpful for your future design.

```epp-experiments
elements-plus:or_gate_3
elements-plus:and_gate_3
elements-plus:xor_gate_3
elements-plus:xnor_gate_3
```
