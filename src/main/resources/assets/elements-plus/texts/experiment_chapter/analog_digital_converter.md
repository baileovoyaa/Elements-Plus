This is an optional chapter. If you have trouble, you can skip it and come back later.

In the previous chapters, we usually cared only about whether a signal is present, not about its exact strength. Such circuits are called <color=blue>digital circuits</color>.

The counterpart is “<color=blue>analog circuits</color>”. Although digital signals can represent richer information and are easier to compute with, in some situations you can use analog circuits to simplify calculations. You may also need to process an input signal's strength, or output a specific strength to drive redstone devices; in those cases, this kind of circuit can be useful.

In this chapter we will convert between signal strength and binary signals. A strength of 0~15 can be represented by exactly 4 binary bits. To save component area, the 4-bit output is packed into an 8-bit bus, with only the low 4 bits valid.

- <color=gold>Analog-to-Digital Converter</color>: Input A (1-bit), output Y (8-bit). Y is the binary representation of A's signal strength (0 maps to 0, 1 maps to 15).
- <color=gold>Digital-to-Analog Converter</color>: Input A (8-bit), output Y (1-bit). Y's signal strength is the binary number represented by the low 4 bits of A (0 maps to 0, non-zero maps to 1; only the low 4 bits are valid).

```epp-experiments
elements-plus:adc
elements-plus:dac
```
