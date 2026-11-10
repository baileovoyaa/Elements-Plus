A comparator can compare two binary numbers. First, the <color=gold>1-bit Comparator</color>.

Note: a 1-bit comparator only distinguishes zero from non-zero, so any non-zero values, such as 1 and 15, are considered equal; a non-zero value is greater than 0.

- <color=gold>1-bit Comparator</color>: Inputs A, B, I0, and I1; outputs Y0 and Y1. When A<B, outputs Y0=15, Y1=0; when A>B, outputs Y0=0, Y1=15; when A=B, outputs Y0 and Y1 as the amplified values of I0 and I1 (15 if non-zero, otherwise 0).

```epp-experiments
elements-plus:comparator
```

Cascading the one-bit comparators above yields the <color=gold>8-bit Comparator</color>.

- <color=gold>8-bit Comparator</color>: Inputs A (8-bit), B (8-bit), I0 (1-bit), and I1 (1-bit); outputs Y0 (1-bit) and Y1 (1-bit). When the unsigned binary values represented by A and B satisfy A<B, outputs Y0=15, Y1=0; when A>B, outputs Y0=0, Y1=15; when A=B, outputs Y0 and Y1 as the amplified values of I0 and I1 (15 if non-zero, otherwise 0).

```epp-experiments
elements-plus:comparator_8
```

Inverting the sign bit introduces a fixed offset that is strictly order-preserving, so it can be used to compare signed numbers. Essentially, the sign-bit-inverted encoding is a form of “<color=blue>Offset Binary</color>” corresponding to the true value of two's complement.

- <color=gold>8-bit Signed Comparator</color>: Inputs A (8-bit), B (8-bit), I0 (1-bit), and I1 (1-bit); outputs Y0 (1-bit) and Y1 (1-bit). When the signed binary values represented by A and B satisfy A<B, outputs Y0=15, Y1=0; when A>B, outputs Y0=0, Y1=15; when A=B, outputs Y0 and Y1 as the amplified values of I0 and I1 (15 if non-zero, otherwise 0).

```epp-experiments
elements-plus:comparator_signed_8
```

There is also a comparator that compares analog signal strength. Unlike the digital 1-bit comparator, it treats signal strengths from 1 to 15 as distinct values; for example, 9 is considered greater than 8. Its implementation is relatively complex, so it is an optional experiment here.

- <color=gold>Analog Comparator</color>: Inputs A, B, I0, and I1; outputs Y0 and Y1. When A's signal strength is less than B's, outputs Y0=15, Y1=0; when greater, outputs Y0=0, Y1=15; when equal, outputs Y0 and Y1 as the amplified values of I0 and I1 (15 if non-zero, otherwise 0).

```epp-experiments
elements-plus:comparator_analog
```
