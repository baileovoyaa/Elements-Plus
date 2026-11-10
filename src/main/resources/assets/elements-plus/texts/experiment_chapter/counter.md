Next, we will build a sequential component: the counter.

When you think of a counter, you may think of cascaded <color=gold>T' flip-flops</color>. That is indeed a simple counter: the ripple counter.

- <color=gold>8-bit Ripple Counter</color>: Input C (1-bit), output Y (8-bit, binary 0/15).

On each clock step, if C changes from 0 to non-zero (0 last tick, non-zero this tick), the output starts increasing by 1, updating from the low bit to the high bit over successive ticks (so it may need up to 8 ticks to settle).

To simplify implementation, the initial state is all 15s (the inverted initial state of T' flip-flops), meaning counting starts from 255; only after the first trigger does it become 0.

```epp-experiments
elements-plus:ripple_counter_8
```

This is a common simple counter in redstone circuits, but its drawbacks are obvious: it can only count up cyclically, cannot be cleared or loaded, and cannot have a custom step size, so its applications are limited. It is also far from sufficient for the architectures we will build later. In addition, as an asynchronous structure, a ripple counter needs multiple ticks to complete one count, and its inherent delay can be risky under strict timing.

A more robust solution is a synchronous counter based on registers and adders:

- <color=gold>8-bit Addition Counter</color>: Inputs A (8-bit) and E (1-bit), output Y (8-bit, binary 0/15).

On each clock step:

- When E is non-zero, it is in load mode: Y is set to the binary value of A;
- When E is zero, it is in increment mode: Y adds the binary value of A (wrapping on overflow).

```epp-experiments
elements-plus:counter_8
```

You may also find that, with a specially configured resonator and capacitor, an analog signal can cycle from 0 to 15. This is usually not called a counter, but a <color=gold>sawtooth generator</color>. It is a circuit that uses the linear charging and rapid discharging of a capacitor to produce a periodic sawtooth signal strength.

- <color=gold>Sawtooth Generator</color>: No inputs, output Y. Y's signal strength cycles in one direction from 0 to 15 (returning to 0 after 15), with an initial output of 0.

```epp-experiments
elements-plus:sawtooth_generator
```

To pass the test, you need to set the initial phase to 0. After passing, the obtained component is configurable. To learn more, see the <color=gold>Waveform Generator</color> chapter.
