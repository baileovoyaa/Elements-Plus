A capacitor can store a signal for one tick, but sometimes we want to store a signal for longer.

That is why we need a component that can preserve a signal for a long time: the register.

First, the <color=gold>1-bit register</color>. It has a write enable input. When write enable is active (non-zero), it stores the input value; when it is inactive, it holds the stored value. Note that it stores the amplified value, not the original signal strength.

- <color=gold>1-bit register</color>: Inputs A and WE, output Y. On each clock step, if WE is non-zero, Y becomes the amplified value of A; otherwise Y stays unchanged.

```epp-experiments
elements-plus:register
```

The 8-bit register is the 8-bit version of the same component.

- <color=gold>8-bit register</color>: Inputs A (8-bit) and WE (1-bit), output Y (8-bit). On each clock step, if WE is non-zero, Y becomes the per-lane amplified value of A; otherwise Y stays unchanged.

```epp-experiments
elements-plus:register_8
```

There is also an “<color=gold>analog register</color>” that can preserve analog signal strength. It is a sample-and-hold circuit that samples or holds depending on whether WE is zero.

- <color=gold>analog register</color>: Inputs A and WE, output Y. On each clock step, if WE is non-zero, Y becomes A's signal strength; otherwise Y stays unchanged.

```epp-experiments
elements-plus:register_analog
```
