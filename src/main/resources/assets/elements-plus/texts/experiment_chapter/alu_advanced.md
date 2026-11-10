You have completed the <color=gold>multiplier</color> and the <color=gold>ALU (Basic)</color>; now you can give your ALU multiplication as well.

The advanced version uses a 4-bit opcode. 0000~0111 are the same as the basic version, 1000 is multiplication, and 1001~1111 are reserved:

---

- `0000`: Add (`A+B+CI`)
- `0001`: Subtract (`A-B-CI`)
- `0010`: Bitwise AND (`A&B`)
- `0011`: Bitwise OR (`A|B`)
- `0100`: Bitwise NOT (`~A`)
- `0101`: Bitwise XOR (`A^B`)
- `0110`: Bitwise logical left shift (`A<<B`) (only the low 3 bits of B are used as the shift amount)
- `0111`: Bitwise logical right shift (`A>>>B`) (only the low 3 bits of B are used as the shift amount)
- `1000`: Multiply (`A*B`)
- `1001~1111`: Reserved for now; outputs all 0.

---

- <color=gold>ALU (Advanced)</color>: Inputs are two operands A (8-bit) and B (8-bit), an optional carry/borrow input CI (1-bit), and an opcode OP (8-bit, only the low 4 bits are valid). Output Y0 (8-bit) is the operation result (the low 8 bits of a product), Y1 is the high 8 bits of a product (all 0 for other operations), and CO (1-bit) is the optional carry/borrow output. Shift operations use only the low 3 bits of B as the shift amount.

The CO convention is the same as the basic version: carry for addition; borrow for subtraction (1 when A<B+CI, meaning the subtraction underflows); 0 otherwise. CI acts as the borrow input for subtraction.

```epp-experiments
elements-plus:alu_advanced
```
