Until the previous chapter, `PC` only incremented by 1 each tick, so a program could only run straight through from beginning to end. This chapter adds <font color=blue>jumps</font>, allowing loops, branches, and waiting for input/output.

Instruction fetch is unchanged: `PC` goes to the virtual program ROM and `INS` is received from it; the architecture still executes one instruction per tick.

## Jump Target Register

An 8-bit instruction has no room for a full 8-bit address, so jumps do not encode the target in the instruction. Instead, the target is first written into a separate 8-bit register `TR` (the jump target register).

- `SETTARGET Rs`: `1110 00RR`, copies register `Rs` into `TR`.
- `Rs` may be `R0`, `R1`, or `R2`; `R3` is IO and its encoding is reserved.
- `SETTARGET R0` = `0xE0`, `SETTARGET R1` = `0xE1`, `SETTARGET R2` = `0xE2`; `SETTARGET R3` = `0xE3` is reserved.
- `SETTARGET R2` reads `mem[R1]` and can be used for jump tables.
- `TR` updates on the clock edge, so the next tick's jump can use the new value.

Every jump performs:

```text
if condition is true: PC <- TR
else:                 PC <- PC + 1
```

`SETTARGET` itself does not change `PC`; it only sets the target. The actual jump is performed by a later `JMP` or conditional jump.

## Jump Instructions

`JMP` is unconditional; the other instructions jump to `TR` when their condition is true.

| Assembly | Hex | Condition |
| --- | --- | --- |
| `JMP` | `0xC0` | Unconditional |
| `JZ` | `0xC1` | `R0 == 0` |
| `JNZ` | `0xC9` | `R0 != 0` |
| `JN` | `0xC2` | `R0 < 0` (signed) |
| `JNN` | `0xCA` | `R0 >= 0` (signed) |
| `JP` | `0xC3` | `R0 > 0` (signed) |
| `JNP` | `0xCB` | `R0 <= 0` (signed) |
| `JEQ` | `0xC4` | `R0 == mem[R1]` |
| `JNE` | `0xCC` | `R0 != mem[R1]` |
| `JLT.U` | `0xC5` | `R0 < mem[R1]` (unsigned) |
| `JGT.U` | `0xC6` | `R0 > mem[R1]` (unsigned) |
| `JGE.U` | `0xCD` | `R0 >= mem[R1]` (unsigned) |
| `JLE.U` | `0xCE` | `R0 <= mem[R1]` (unsigned) |
| `JLT` | `0xE4` | `R0 < mem[R1]` (signed) |
| `JGT` | `0xE5` | `R0 > mem[R1]` (signed) |
| `JGE` | `0xE6` | `R0 >= mem[R1]` (signed) |
| `JLE` | `0xE7` | `R0 <= mem[R1]` (signed) |
| `JVI` | `0xD0` | `VI == 1` |
| `JNVI` | `0xD8` | `VI == 0` |
| `JVO` | `0xD1` | `VO == 1` |
| `JNVO` | `0xD9` | `VO == 0` |

About comparisons:

- `JN`/`JP`/`JNN`/`JNP` interpret `R0` as an 8-bit two's-complement value and compare only with 0.
- `JLT.U`/`JGT.U`/`JGE.U`/`JLE.U` compare with `mem[R1]` as **unsigned** 8-bit values.
- `JLT`/`JGT`/`JGE`/`JLE` compare with `mem[R1]` as **signed two's-complement** values.
- `JEQ`/`JNE` compare equality regardless of sign.

Use the unsigned versions for non-negative values such as addresses and lengths; use the signed versions for values that may be negative.

## Setting the Jump Target

The jump target must first be placed in a register, then `SETTARGET` is used. For example, to jump to address `0x1A`:

```text
LDL R0, #0xA    ; 0x4A
LDH R0, #0x1    ; 0x81, R0 = 0x1A
SETTARGET R0    ; 0xE0, TR = 0x1A
JMP             ; 0xC0, PC = TR
```

In assembly, labels represent addresses; in the actual program, the target address is split into low and high 4 bits and loaded with `LDL`/`LDH`. The later assembly chapter explains how labels and pseudo-instructions simplify this.

Preparing the target occupies a register (note that `SETTARGET` itself does not modify its source register):

- Loading the target into `R0` overwrites the previous value in `R0`.
- Loading it into `R1` overwrites the address register.
- The target can also be placed in memory and loaded with `SETTARGET R2`; this does not overwrite a general-purpose register, but `R1` must already point to the target table.

`SETTARGET Rs` only copies `Rs` into `TR`; it does not modify `Rs`. A common pattern is therefore: **prepare and set TR first, then prepare comparison values/data**. If the data is already computed, store it in memory first or use another register for the target.

## Waiting for Input

`JNVI` jumps when `VI=0` and can wait for valid input. Suppose the loop label `wait_vi` is at address `0x1A`:

```text
; First load the address 0x1A of wait_vi into R0 and set TR
LDL R0, #0xA    ; 0x4A
LDH R0, #0x1    ; 0x81, R0 = 0x1A
SETTARGET R0    ; 0xE0

wait_vi:
    JNVI            ; 0xD8, jump back to wait_vi when VI=0
    MOV R0, R3      ; 0x3C, when VI=1 read DI and raise RI for one tick
```

When `VI=0`, `JNVI` sets `PC` back to `TR` and checks again next tick. When `VI=1`, the condition is false and the next `MOV` executes. The `MOV` raises `RI` in the same tick and reads `DI` into `R0`.

If `R0` already holds data you need to keep, use `R1` for the target, use `SETTARGET R2` with the target in memory, or store the data in memory first.

## Waiting for Output

`JVO` jumps when `VO=1` and can wait for the previous output to be taken by `RO`. Suppose the result is in `R0` and the loop label `wait_vo` is at address `0x24`:

```text
; R0 holds the result; R1 is temporarily used to set the target
LDL R1, #0x4    ; 0x54
LDH R1, #0x2    ; 0x92, R1 = 0x24
SETTARGET R1    ; 0xE1

wait_vo:
    JVO             ; 0xD1, jump back to wait_vo when VO=1
    MOV R3, R0      ; 0x33, write output when VO=0
```

`VO=1` means the previous output is still waiting for `RO`; `JVO` loops until it is taken. When `VO=0`, the condition is false and `MOV R3, R0` writes the result to the output register; `VO` becomes 1 next tick.

If `R1` is also needed for a data address, store the result in `mem[R1]` first, set the target with `R0`, and finally output `mem[R1]`.

`JVO`/`JNVO` sample `VO` during the execution phase. If a `VO && RO` handshake happens in the same tick, `VO` is cleared on the clock edge, but the jump still uses the value sampled before clearing, so it may loop one extra time and exit on the next iteration.

## Notes

- Jump targets come from `TR`, not from an immediate in the instruction; a loop usually only needs `TR` set once outside the loop.
- `SETTARGET` updates `TR` on the clock edge and it becomes visible next tick; an immediately following jump uses the new value.
- Conditional jumps only compare `R0` with 0, compare `R0` with `mem[R1]`, or test `VI`/`VO`; there are no flags.
- This chapter has no call/return or stack; jumps rely only on `TR` and conditions.
- Conditional jumps may go forward or backward, so `PC` is no longer monotonic; test programs use loops and branches to verify this.

```epp-experiments
elements-plus:architecture_jump
```

This experiment is still not associated with a component.
