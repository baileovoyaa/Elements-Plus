Before implementing multi-bit addition, let us introduce the concept of <color=blue>bus</color>.

A bus is a bundle of multiple wires, which simplifies wiring so you do not need to arrange multiple parallel wires.

The number on the right side of the wire button in the lithography machine tool bar is the current bit width. You can click or scroll the mouse wheel on it to switch the bit width. The bus will no longer change color according to the signal strength, but will display a fixed color corresponding to the bus bit width. Only wires with matching bit width can be connected to certain component pins.

Find “<color=gold>8-bit Input</color>” and “<color=gold>8-bit Output</color>”, as well as “<color=gold>8-bit Bus Joiner</color>” and “<color=gold>8-bit Bus Splitter</color>” in the component list. We will use them in this chapter.

We unify the rules for bus joiners and splitters, specifying that the side closer to the opposite pin (the upper side when placed upright) is the least significant bit.

In the following, “1-bit” and “8-bit” usually refer to the number of channels in a bus, or to the binary value formed by treating each channel as 0/1.

---

Try it:

- <color=gold>8-bit Constant</color>: No inputs; always outputs 8-bit Y=123 (a 0 bit corresponds to 0, a 1 bit to 15).
- <color=gold>8-bit Mask</color>: Input A (1-bit). When A is non-zero, outputs 8-bit Y=123 and each channel has the same signal strength as the input; otherwise outputs all zeros.

```epp-experiments
elements-plus:const_8
elements-plus:mask_8
```

The constant 123 in the test cases is only used to check whether you understand the concept. Both components have configurable properties; select the component and set any value in the attribute panel on the right, or double-click the component to enter a number directly.

---

- <color=gold>8-bit Bit Extractor</color>: Input A (8-bit); output Y (1-bit) is bit 3 of A (bit 0 is the least significant bit), preserving signal strength.
- <color=gold>8-bit Bit Indexer</color>: Inputs A (8-bit) and B (8-bit). The low 3 bits of B represent a binary number (0–7); output Y (1-bit) is the amplified value of that bit of A (0 maps to 0, non-zero maps to 15).

The bit extractor always takes a fixed bit; the bit 3 used in the test cases is only for testing. You can configure which bit to extract (0–7) in the component settings.

```epp-experiments
elements-plus:bit_extract_8
elements-plus:bit_index_8
```

In some analog circuits you may care about the signal strength of each channel. Here is an optional experiment for producing an arbitrary 8-bit signal.

- <color=gold>8-bit Analog Constant</color>: No inputs; outputs 8-bit Y with channel strengths (high to low): 15 13 11 9 0 2 4 6
- <color=gold>8-bit Analog Bit Indexer</color>: Inputs A (8-bit) and B (8-bit). The low 3 bits of B represent a binary number (0–7); output Y (1-bit) is that bit of A, preserving signal strength.

```epp-experiments
elements-plus:const_analog_8
elements-plus:bit_index_analog_8
```

It is also configurable.
