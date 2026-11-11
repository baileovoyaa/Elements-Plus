The previous chapter's architecture could only move values already in registers; there was no way to write constants into registers. This chapter adds two <font color=blue>immediate</font> instructions so programs can produce constants directly, for setting addresses, counters, and later memory data.

Instruction fetch is unchanged: `PC` is sent to the virtual program ROM and `INS` is received from it; the architecture still executes one instruction per tick. This chapter still has no ALU or data memory; it only adds immediate instructions.

## LDL and LDH

An 8-bit instruction has no room for a full 8-bit immediate, so constants are written in two parts: one instruction writes the low 4 bits and another writes the high 4 bits.

| Assembly | Machine code | Hex | Effect |
| --- | --- | --- | --- |
| `LDL Rd, #n` | `01 RR nnnn` | `0x40`~`0x5F` | Low 4 bits of `Rd` <- `n`; high 4 bits unchanged |
| `LDH Rd, #n` | `10 RR nnnn` | `0x80`~`0x9F` | High 4 bits of `Rd` <- `n`; low 4 bits unchanged |

`RR` is the register number: `00`=`R0`, `01`=`R1`; `nnnn` is a 4-bit immediate (0~15).

- `LDL R0` covers `0x40`~`0x4F`; `LDL R1` covers `0x50`~`0x5F`.
- `LDH R0` covers `0x80`~`0x8F`; `LDH R1` covers `0x90`~`0x9F`.

For example, to load `0xA3` into `R0`:

```text
LDL R0, #0x3    ; 0100 0011 = 0x43, low 4 bits of R0 = 3
LDH R0, #0xA    ; 1000 1010 = 0x8A, high 4 bits of R0 = A
; result: R0 = 0xA3
```

The two instructions may be executed in either order; only the half that is written changes. If only one is executed, the other half keeps its previous value.

The assembly chapter later introduces the `LDI Rd, #imm8` shorthand, which expands to this `LDL`/`LDH` pair. The tests in this chapter still use the two instructions directly.

## Why R3 cannot use immediates

`R3` is not an ordinary storage register; it is the IO port:

- Writing `R3` outputs to `DO` and makes `VO` valid.
- Reading `R3` inputs from `DI` and raises `RI` for one tick.

Allowing `LDL R3, #n` / `LDH R3, #n` would mean writing only half a byte to the output:

- The other half of the output register may be waiting for `RO` to take it, so there is no stable high/low nibble to preserve.
- A nibble write could accidentally make `VO` valid, exposing an incomplete output to the outside.
- `R3` has no independent storage cell: reading `R3` is an input handshake, not a register read, so `LDL`/`LDH` cannot perform a read-modify-write on it as they can with `R0`/`R1`.

Therefore the `RR=11` encodings of `LDL`/`LDH` are reserved and the basic architecture treats them as NOP, with no IO side effects. To get a constant to `R3`, load it into `R0` or `R1` first, then use `MOV R3, R0` or `MOV R3, R1`.

`R2` also cannot use immediates, but for a different reason: `R2` is a memory mapping, not an independent register. A nibble write to `R2` would become a read-modify-write of memory. The memory chapter explains this in detail; the `RR=10` encodings are also reserved in this chapter.

## Decode and Writeback

On top of the previous chapter, the decoder needs to:

- Recognize `LDL` when the top two bits are `01`, and `LDH` when the top two bits are `10`.
- Check whether `RR` is `00`/`01`, applying the instruction only to `R0`/`R1`.
- Generate a low-nibble or high-nibble write enable for the selected register.
- Preserve the other half: `LDL` preserves the high 4 bits and `LDH` preserves the low 4 bits.

The `RR=10`/`11` encodings are treated as NOP: no memory write and no IO access.

## Example

The following program loads `0x2A` into `R0` and outputs it:

```text
LDL R0, #0xA    ; 0x4A
LDH R0, #0x2    ; 0x82, R0 = 0x2A
MOV R3, R0      ; 0x33, output 0x2A
NOP
```

You can also set `R1` with an immediate to prepare an address for the next chapter:

```text
LDL R1, #0x0    ; 0x50
LDH R1, #0x2    ; 0x92, R1 = 0x20
```

This chapter still has no jumps, so the program executes sequentially by `PC`. At the start of each test case, `PC`, `R0`, `R1`, and the output register should all be 0.

```epp-experiments
elements-plus:architecture_immediate
```

This experiment is still not associated with a component.
