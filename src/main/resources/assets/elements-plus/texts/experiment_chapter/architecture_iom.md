Starting with this chapter, we will build a computer. The computer architecture is divided into several chapters, each implementing one part; until the complete architecture is built, these experiments are not associated with a component.

The core of computer execution is the <font color=blue>instruction</font>: one instruction represents one operation. To make instructions easier to write and read, we use mnemonics, for example writing `0011 SS DD` as `MOV D, S`; this is <font color=blue>assembly</font>.

The architecture in this chapter contains only instruction fetch, two general-purpose registers, IO, and an output register; it does not yet have an ALU or data memory.

**Note: computer architecture is completely independent of signal strength. For convenience, these chapters use 1 to represent any non-zero signal strength.**

## Fetch and Execute

The instructions in this series of experiments are provided by an external virtual program ROM:

- Your architecture outputs `PC` (8-bit), which is the program ROM address;
- The program ROM returns `INS` (8-bit), the instruction byte at that address;
- The architecture decodes and executes `INS` during the current tick, updating registers and `PC` on the clock step.

Instruction fetch itself is combinational logic: `PC -> ROM address -> INS -> decode`; it needs no additional clock. The virtual ROM is provided by the test system, so you do not need to build it. The program ROM and data RAM address spaces are also separate; this chapter has no data RAM yet.

One instruction is executed per tick. During sequential execution, `PC` increments by 1 each tick and wraps back to 0 on overflow; `PC=0` at the start of each test case. The test system ends the test case automatically after the program finishes, so no halt instruction is needed.

The interface of this chapter's architecture:

| Direction | Signal | Width | Description |
| --- | --- | --- | --- |
| Input | `INS` | 8 | Instruction byte returned by the program ROM |
| Input | `VI` | 1 | Input data is valid |
| Input | `DI` | 8 | Input data |
| Input | `RO` | 1 | The outside is ready to receive output |
| Output | `PC` | 8 | Program ROM address |
| Output | `RI` | 1 | The circuit is ready to receive input |
| Output | `VO` | 1 | Output data is valid |
| Output | `DO` | 8 | Output data |

## Registers

This chapter implements only two ordinary registers:

- `R0` (`00`): 8-bit accumulator/general-purpose register;
- `R1` (`01`): 8-bit general-purpose register;
- `R2` (`10`): reserved for the later memory chapter; the tests in this chapter never read or write it;
- `R3` (`11`): IO port, not an ordinary register.

Reading and writing `R3` both involve external handshakes:

- Reading `R3`: read data from `DI` and raise `RI` for one tick;
- Writing `R3`: write data to `DO`, make `VO` valid, and wait for the outside to take it with `RO`.

An <font color=blue>output register</font> is also needed to hold `DO` and `VO`. It is not one of the instruction-set registers `R0`~`R3`; it is part of IO. `VO` stays valid until `RO` and `VO` are both 1; after the handshake completes, it is cleared to 0.

## Instructions in This Chapter: MOV and NOP

This chapter only needs two instructions:

- `NOP`: `0000 0000` (`0x00`), do nothing and only increment `PC`;
- `MOV D, S`: `0011 SS DD`, copy the value of source register `S` to destination register `D`.

`SS` and `DD` are both 2-bit register numbers: `00`=`R0`, `01`=`R1`, `10`=`R2`, `11`=`R3`. In assembly we write `MOV D, S`, destination first and source second; in machine code the source `SS` comes first, then the destination `DD`.

Common combinations in this chapter:

| Assembly | Machine code | Hex | Effect |
| --- | --- | --- | --- |
| `MOV R0, R1` | `0011 0100` | `0x34` | `R0 <- R1` |
| `MOV R1, R0` | `0011 0001` | `0x31` | `R1 <- R0` |
| `MOV R0, R3` | `0011 1100` | `0x3C` | `R0 <- DI`, raise `RI` |
| `MOV R1, R3` | `0011 1101` | `0x3D` | `R1 <- DI`, raise `RI` |
| `MOV R3, R0` | `0011 0011` | `0x33` | `DO <- R0`, raise `VO` |
| `MOV R3, R1` | `0011 0111` | `0x37` | `DO <- R1`, raise `VO` |
| `MOV R3, R3` | `0011 1111` | `0x3F` | `DI -> DO`, handle `RI`/`VO` together |
| `MOV R0, R0` | `0011 0000` | `0x30` | Empty operation |
| `MOV R1, R1` | `0011 0101` | `0x35` | Empty operation |

The combinations involving `R2` are reserved in this chapter and are not used by the tests; your decoder may ignore them.

In implementation, `PC` is an 8-bit register that outputs the current address; `INS` enters the decoder. The decoder distinguishes `MOV`/`NOP` by the high 4 bits of `INS`, then chooses source and destination from `SS`/`DD`: `R0`/`R1` each have write enables, `R2` is not connected in this chapter, and `R3` reads and writes through the IO handshake. Other encodings unused in this chapter may be treated as `NOP`.

## R3 Read/Write Rules

**Reading `R3` (`MOV destination, R3`)**:

- If `VI=1`, input data is valid: raise `RI` for one tick and save `DI` to the destination register on the clock step;
- If `VI=0`, there is no data to read: ignore this `MOV`, leave the destination register unchanged, and do not raise `RI`.

**Writing `R3` (`MOV R3, source`)**:

- If `VO=0`, the output is idle: write the source data to the output register; `VO` becomes 1 on the next tick;
- If `VO=1` and `RO=0`, the output is blocked: ignore this write; `DO`/`VO` stay unchanged;
- If `VO=1` and `RO=1`, the handshake completes on this tick and `VO` is cleared first; this write takes effect on the next tick.

**`MOV R3, R3`**:

- If `VI=1` and the output is not blocked, send `DI` directly to `DO`, raise `RI`, and make `VO` become 1 on the next tick;
- If `VI=0` or the output is blocked, ignore the whole instruction; do not raise `RI`, and do not change the output.

## A Simple Program

The following program reads input into `R0`, then writes it to output:

```text
NOP
MOV R0, R3      ; 0011 1100, read input
MOV R3, R0      ; 0011 0011, write output
NOP
```

This chapter has no jump instruction yet, so the program can only execute sequentially and cannot loop waiting for `VI`/`VO`. The test program supplies input and `RO` on a fixed schedule; use `NOP` for alignment when necessary. After jumps are introduced later, you can use wait loops to handle uncertain input/output timing.

At the start of each test case, `PC`, `R0`, `R1`, and the output register should all be 0.

```epp-experiments
elements-plus:architecture_iom
```

This experiment is still not associated with a component.
