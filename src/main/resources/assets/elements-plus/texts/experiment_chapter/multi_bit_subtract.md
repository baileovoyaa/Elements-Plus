Previously we learned to represent numbers in binary and to add binary numbers. In this chapter we will subtract multi-bit binary numbers. The most direct idea is, like the decimal column method, to subtract bit by bit from the low position to the high position, borrowing 1 from the next higher bit when needed. But computers are better at addition, and designing a separate subtraction circuit is not worthwhile. So we want to turn subtraction into addition as well: `A − B = A + (−B)`.

The problem then becomes: with a fixed number of bits containing only 0 and 1, how do we represent a negative number? The answer is — <color=blue>two's complement</color>.

Take 8-bit binary as an example. There are 2^8=256 states in total, which we can view as a cycle “modulo 256”. The value -x is the number that satisfies `x + (-x) = 00000000` while discarding the carry out of the highest bit; that number is 256-x.

---

Since `256 = 11111111 + 1`, and 11111111-x is exactly the bitwise inversion of every bit of x, we get the core mnemonic of two's complement:

**Invert every bit, then add one to the last bit.**

Note: all 8 bits must take part in the inversion; the +1 starts from the least significant bit and carries bit by bit; if the highest bit produces a 9th carry, it is simply discarded.

---

For example, to find the 8-bit two's complement of -10, the negation of +10:

- +10 = 00001010
- Bitwise inversion: 11110101
- Add one to the last bit: 11110110

So the 8-bit two's complement of -10 is 11110110. Verify: `00001010 + 11110110 = 1 00000000`; discarding the carry out of the highest bit gives 00000000, which is exactly 0.

Conversely, if we invert 11110110 and add one again, we get back +10. This shows that the “invert and add one” of two's complement is reversible. The two's complement of a positive number is itself, and 0 has only one representation: 00000000.

---

With two's complement, multi-bit subtraction can be unified into multi-bit addition. To compute A-B:

1. Write both A and B as the same 8 bits;

2. Find the negation of B: invert all 8 bits of B, then add one to the last bit, to get -B;

3. Perform ordinary binary addition A + (-B), adding bit by bit and propagating carries, discarding the carry out of the highest bit;

4. The result is the two's complement of A-B.

For example, computing 12-5: `00001100 + 11111011 = 1 00000111`, which gives 00000111, i.e. 7.

The range representable by 8-bit two's complement is -128 to +127. A highest bit of 0 indicates a positive number or 0, and a highest bit of 1 indicates a negative number. Going beyond this range causes overflow; for example, 127+1 gives 10000000, which reads as -128 in 8-bit two's complement, not +128.

In short, two's complement turns subtraction into addition: `A-B = A + (~B + 1)`, where ~B means bitwise inversion.

---

In this experiment we will build the “<color=gold>8-bit Negator</color>”.

Input A (8-bit); output the two's complement representation of its negation Y (8-bit).

```epp-experiments
elements-plus:neg
```

Combined with the <color=gold>8-bit Adder</color>, you can then implement 8-bit subtraction.
