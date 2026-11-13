The architecture can now fetch instructions, read registers, and read/write memory, but it cannot calculate yet. This chapter connects the <font color=gold>Multi-function ALU (Basic)</font> and adds eight ALU instructions.

Instruction fetch is unchanged: `PC` goes to the virtual program ROM and `INS` is received from it; the architecture still executes one instruction per tick.

## Connecting the ALU

The basic ALU pins are:

- `A` (8-bit), `B` (8-bit): the two operands;
- `OP` (8-bit): operation code; only the low 3 bits are used;
- `CI` (1-bit): carry input;
- `Y` (8-bit): result;
- `CO` (1-bit): carry output.

Connections:

- `A` to `R0`;
- `B` to `R2`, which is `mem[R1]`;
- `OP` to the low 3 bits of `INS` (`INS[2:0]`);
- `CI` to 0;
- `Y` to the `R0` writeback data;
- `CO` unconnected; this chapter does not use carry.

The ALU is combinational: the result is computed during the current tick and `R0` updates on the clock edge. The `R0` write enable must cover every instruction that writes `R0`: `MOV`, `LDL`/`LDH`, and the ALU instructions. The writeback data also needs a mux selecting between the `MOV` source, the immediate value, and the ALU `Y` output.

## Instructions in This Chapter

| Assembly | Machine code | Hex | Effect |
| --- | --- | --- | --- |
| `ADD` | `0010 0000` | `0x20` | `R0 <- R0 + mem[R1]` |
| `SUB` | `0010 0001` | `0x21` | `R0 <- R0 - mem[R1]` |
| `AND` | `0010 0010` | `0x22` | `R0 <- R0 & mem[R1]` |
| `OR` | `0010 0011` | `0x23` | `R0 <- R0 \| mem[R1]` |
| `NOT` | `0010 0100` | `0x24` | `R0 <- ~R0` |
| `XOR` | `0010 0101` | `0x25` | `R0 <- R0 ^ mem[R1]` |
| `SHL` | `0010 0110` | `0x26` | `R0 <- R0 << (mem[R1] & 7)` |
| `SHR` | `0010 0111` | `0x27` | `R0 <- R0 >> (mem[R1] & 7)` |

Notes:

- The two operands are always `R0` and `mem[R1]`, and the result is always written back to `R0`; `mem[R1]` is not modified.
- `SUB` computes `R0 - mem[R1]`, not the other way around.
- `NOT` is unary and ignores the value of `mem[R1]`; the result does not depend on it and memory is not modified.
- `SHL`/`SHR` take the shift amount from the low 3 bits of `mem[R1]`, in the range 0~7. Shifted-out bits are discarded and vacated bits are filled with 0. These are **logical shifts**.
- Addition and subtraction wrap modulo 8 bits; carry/borrow is not preserved and there are no flags. Conditional tests compare values directly in the jump chapter.
- `0010 1XXX` (`0x28`~`0x2F`) is ALU page two, reserved for advanced features; the basic architecture treats it as NOP.

## Examples

Assume `R0=0x0A`, `R1=0x20`, and `mem[0x20]=0x03`:

```text
ADD             ; 0x20, R0 <- 0x0A + 0x03 = 0x0D
MOV R3, R0      ; 0x33, output 0x0D
NOP
```

Then a subtraction:

```text
; R0=0x0A, mem[0x20]=0x03
SUB             ; 0x21, R0 <- 0x0A - 0x03 = 0x07
MOV R3, R0      ; 0x33
NOP
```

The address in `R1` still has to be set in advance with `LDL`/`LDH` or `MOV`. The ALU only reads `mem[R1]`; it does not change the address automatically.

## Notes

- ALU `OP` uses only the low 3 bits, matching the eight instructions. `CI` is fixed at 0 and `CO` is ignored.
- The ALU result is combinational and must settle within the same tick. The ALU result should only be selected as `R0` writeback data for ALU instructions; the overall `R0` write enable must still cover `MOV`, `LDL`/`LDH`, and ALU instructions.
- This chapter uses only the basic ALU; advanced features such as multiplication are outside the required computer architecture.
- There are still no jumps, so the program executes sequentially by `PC`.

```epp-experiments
elements-plus:architecture_compute
```

This experiment is still not associated with a component.
