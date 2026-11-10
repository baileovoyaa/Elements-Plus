In the previous chapter we learned the concept of <color=blue>two's complement</color>. From now on, “<color=blue>signed number</color>”, unless stated otherwise, always refers to two's complement.

A shifter is a component that can shift a binary number left or right by some number of positions. For non-rotating shifts, shifting left by n is equivalent to multiplying by 2^n, and shifting right by n is equivalent to dividing by 2^n and rounding down; right-shifting a signed number requires an arithmetic right shift that preserves the sign bit.

For example, shifting 0000 1001 left by 3 gives 0100 1000.

---

Shift operations come in two kinds: <color=blue>logical shift</color> and <color=blue>arithmetic shift</color>. Logical shifts do not distinguish the sign bit and always fill vacated bits with 0; arithmetic right shifts preserve the sign bit and fill vacated high bits with the sign bit. For left shifts the results are the same (low bits are filled with 0), so only a separate arithmetic right shifter is needed.

The difference between logical and arithmetic right shift can be seen with 1001 0110: logical right shift by 3 gives 0001 0010, while arithmetic right shift by 3 gives 1111 0010 (the highest bit is 1, so vacated high bits are filled with 1).

To avoid overlapping functionality, the shifter is split by shift direction and fill rule into three unidirectional versions: <color=gold>8-bit Left Shifter</color>, <color=gold>8-bit Right Shifter</color> and <color=gold>8-bit Arithmetic Right Shifter</color>. The bidirectional version is kept as an optional experiment.

If A is a signed number, use the arithmetic right shifter when shifting right; a logical right shift treats the sign bit as ordinary data and may produce a large positive number.

Unidirectional shifters (including the later rotate and analog versions) use only the low 3 bits of B as the shift amount, so the amount is 0–7 and the high 5 bits of B are ignored. Non-rotating shifts by 8 or more move out all original bits, while rotations have a period of 8, so 3 bits are enough to represent every valid shift amount. For example, the low 3 bits of B=8 are 0, which is equivalent to no shift.

- <color=gold>8-bit Left Shifter</color>: Inputs A (8-bit) and B (8-bit, only the low 3 bits are effective); output Y (8-bit) is A shifted left by B bits (0 maps to 0, 1 maps to 15); low bits are filled with 0 and bits shifted out of the high end are discarded.
- <color=gold>8-bit Right Shifter</color>: Same inputs as above; output Y is A logically shifted right by B bits; high bits are filled with 0 and bits shifted out of the low end are discarded.
- <color=gold>8-bit Arithmetic Right Shifter</color>: Same inputs as above; output Y is A arithmetically shifted right by B bits; high bits are filled with the sign bit (0 for positive, 1 for negative) and bits shifted out of the low end are discarded.

The bidirectional versions do not use the low-3-bit rule. Instead, the whole 8-bit B represents both direction and amount as a signed number (two's complement): positive shifts left, negative shifts right.

- <color=gold>8-bit Shifter</color>: Inputs A (8-bit) and B (8-bit); output Y (8-bit). When B is positive, Y is A shifted left by B bits; when B is negative, Y is A shifted right by |B| bits. Bits shifted out are discarded, and missing bits are filled with 0 (0 maps to 0, 1 maps to 15).
- <color=gold>8-bit Arithmetic Shifter</color>: Same inputs and outputs as above; the difference is that missing bits are filled with 0 on left shifts and with the sign bit on right shifts.

```epp-experiments
elements-plus:bitwise_left_shift
elements-plus:bitwise_right_shift
elements-plus:bitwise_right_shift_arithmetic
elements-plus:bitwise_shift
elements-plus:bitwise_shift_arithmetic
```

There is also a <color=gold>rotator</color>, which does not discard bits shifted out but rotates them to the other side. For example, 1001 0110 rotated left by 6 becomes 1010 0101. Like ordinary shifters, the rotator is split into unidirectional left-rotate and right-rotate versions plus a bidirectional version. These are optional experiments.

- <color=gold>8-bit Left Rotator</color>: Inputs A (8-bit) and B (8-bit, only the low 3 bits are effective); output Y is A **circularly** rotated left by B bits (0 maps to 0, 1 maps to 15); bits shifted out of the high end wrap to the low end.
- <color=gold>8-bit Right Rotator</color>: Same inputs as above; output Y is A **circularly** rotated right by B bits; bits shifted out of the low end wrap to the high end.
- <color=gold>8-bit Rotator</color>: Inputs A (8-bit) and B (8-bit); output Y (8-bit). Bidirectional version. The whole 8-bit B is treated as a signed number; positive rotates left, negative rotates right.

```epp-experiments
elements-plus:bitwise_left_rotate
elements-plus:bitwise_right_rotate
elements-plus:bitwise_rotate
```

There is also an <color=gold>analog shifter</color>, which preserves the signal strength of non-zero bits instead of turning them all into 15. For example, 1 2 3 4 5 6 7 8 shifted left by 4 becomes 5 6 7 8 0 0 0 0, and rotated left by 4 becomes 5 6 7 8 1 2 3 4. The analog shifter is split into unidirectional left-shift, right-shift, left-rotate and right-rotate versions, plus two bidirectional versions. These are optional experiments.

Each output bit of a digital shifter is either 0 or 15; an analog shifter preserves the original 0–15 signal strength.

- <color=gold>8-bit Analog Left Shifter</color>: Inputs A (8-bit) and B (8-bit, only the low 3 bits are effective); output Y is A shifted left by B bits, preserving each channel's signal strength; low bits are filled with 0 and bits shifted out of the high end are discarded.
- <color=gold>8-bit Analog Right Shifter</color>: Same inputs as above; output Y is A logically shifted right by B bits, preserving each channel's signal strength; high bits are filled with 0 and bits shifted out of the low end are discarded.
- <color=gold>8-bit Analog Shifter</color>: Inputs A (8-bit) and B (8-bit); output Y (8-bit). Bidirectional version. The whole 8-bit B is treated as a signed number; positive shifts left, negative shifts right (high bits are filled with 0); each channel's signal strength is preserved.
- <color=gold>8-bit Analog Left Rotator</color>: Inputs A (8-bit) and B (8-bit, only the low 3 bits are effective); output Y is A circularly shifted left by B bits, preserving each channel's signal strength; bits shifted out of the high end wrap to the low end.
- <color=gold>8-bit Analog Right Rotator</color>: Same inputs as above; output Y is A circularly shifted right by B bits, preserving each channel's signal strength; bits shifted out of the low end wrap to the high end.
- <color=gold>8-bit Analog Rotator</color>: Inputs A (8-bit) and B (8-bit); output Y (8-bit). Bidirectional rotate version. The whole 8-bit B is treated as a signed number; positive rotates left, negative rotates right; each channel's signal strength is preserved.

```epp-experiments
elements-plus:bitwise_left_shift_analog
elements-plus:bitwise_right_shift_analog
elements-plus:bitwise_shift_analog
elements-plus:bitwise_left_rotate_analog
elements-plus:bitwise_right_rotate_analog
elements-plus:bitwise_rotate_analog
```
