This is the last required experiment in the combinational-logic part. You have now learned some ways to operate on binary numbers. In a computer, we need to decide what operation to perform based on a stretch of binary bits in an instruction. That stretch of bits is called the “opcode”.

In this experiment we will build a circuit that implements 8 operations. The opcodes and their corresponding operations are as follows:

---

- `000`: Add (`A+B+CI`)
- `001`: Subtract (`A-B-CI`)
- `010`: Bitwise AND (`A&B`)
- `011`: Bitwise OR (`A|B`)
- `100`: Bitwise NOT (`~A`)
- `101`: Bitwise XOR (`A^B`)
- `110`: Bitwise logical left shift (`A<<B`) (only the low 3 bits of B are used as the shift amount)
- `111`: Bitwise logical right shift (`A>>>B`) (only the low 3 bits of B are used as the shift amount)

---

- <color=gold>ALU (Basic)</color>: Inputs are two operands A (8-bit) and B (8-bit), an optional carry/borrow input CI (1-bit), and an opcode OP (8-bit, only the low 3 bits are valid). Output Y (8-bit) is the operation result, and CO (1-bit) is the optional carry/borrow output. Shift operations use only the low 3 bits of B as the shift amount.

CO is the carry output for addition; for subtraction it is the borrow output (1 when A<B+CI, meaning the subtraction underflows); for all other operations it is 0. CI acts as the borrow input for subtraction.

```epp-experiments
elements-plus:alu
```
