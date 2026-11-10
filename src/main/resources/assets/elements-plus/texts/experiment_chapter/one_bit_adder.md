Have you ever wondered how we represent numbers in circuits?

Indeed, the signal strength of 0~15 can represent 16 numbers. However, signal strength also has its drawbacks: it is difficult to read and process, and it will attenuate in macro circuits.

To address this, we will introduce a more stable and easy-to-use number representation method——<color=blue>Binary</color>.

The numbers we use daily are base 10 (<color=blue>Decimal</color>), which means we carry over every 10. Binary, on the other hand, is base 2, meaning each digit in a binary number can only be 0 or 1. When 1+1, it equals (10)₂. This way, we can represent a binary number using multiple wires, and we don't care about the signal strength of each digit, only whether it 'has' or 'does not have' a signal, corresponding to 1 and 0 respectively.

---

In this experiment, we will handle the addition of one-bit binary numbers. First, we need to do the addition of two one-bit binary numbers. This component is called the “<color=gold>Half adder</color>”.

- <color=gold>Half adder</color>: Inputs A and B, add A and B as binary numbers (non-zero corresponds to 1, zero corresponds to 0), output sum S and carry C (1 corresponds to 15, 0 corresponds to 0).

To make it more intuitive, we list all the cases of the half adder below:

- A=0, B=0 → C=0, S=0
- A=0, B≠0 → C=0, S=15
- A≠0, B=0 → C=0, S=15
- A≠0, B≠0 → C=15, S=0

```epp-experiments
elements-plus:half_adder
```

Handling the addition of two one-bit binary numbers is not enough, because if we want to perform addition of multi-bit binary numbers, we need to handle the carry problem. Therefore, we need to handle the addition of three-bit binary numbers, which is called the “<color=gold>Full adder</color>”.

- <color=gold>Full adder</color>: Inputs A, B, and CI, add A and B as binary numbers (non-zero corresponds to 1, zero corresponds to 0), output sum S and carry CO (1 corresponds to 15, 0 corresponds to 0).

Similarly, we list all the cases of the full adder below:

- A=0, B=0, CI=0 → CO=0, S=0
- A=0, B≠0, CI=0 → CO=0, S=15
- A≠0, B=0, CI=0 → CO=0, S=15
- A≠0, B≠0, CI=0 → CO=15, S=0
- A=0, B=0, CI≠0 → CO=0, S=15
- A=0, B≠0, CI≠0 → CO=15, S=0
- A≠0, B=0, CI≠0 → CO=15, S=0
- A≠0, B≠0, CI≠0 → CO=15, S=15

```epp-experiments
elements-plus:full_adder
```
