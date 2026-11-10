## From A+B to a Computer

The A+B experiment showed how a circuit can exchange data with the outside world. In this chapter we combine those circuits and try to build a minimal computer.

This chapter only implements instruction fetch, registers, and input/output. ALU and memory are not included yet.

Instructions determine what the computer does. For readability, this chapter writes instructions in assembly form, such as `MOV R0, R1`; the machine actually stores an 8-bit encoding.

## Instruction Fetch

Instructions are provided by an external program ROM.

- `PC` (8-bit) is the address sent to the program ROM.
- The program ROM outputs `INS` (8-bit) at the address selected by `PC`.
- One instruction is executed per tick; during sequential execution, `PC` increments by 1 each tick.

The fetch path can be written as `PC -> ROM address -> INS -> decode/execute`.

## Architecture Interface

| Direction | Signal | Width | Description |
| --- | --- | --- | --- |
| Input | `INS` | 8 | Instruction byte returned by the program ROM at the address selected by `PC` |
| Input | `VI` | 1 | External data is valid |
| Input | `DI` | 8 | External input data |
| Input | `RO` | 1 | The outside is ready to read output |
| Output | `PC` | 8 | Program ROM address |
| Output | `RI` | 1 | The circuit is ready to receive input |
| Output | `VO` | 1 | Output data is valid |
| Output | `DO` | 8 | Output data |

## Registers

This chapter uses four registers with 2-bit numbers: `R0`, `R1`, `R2`, and `R3`.

- `R0` and `R1` are ordinary 8-bit registers.
- `R2` is reserved for the later memory chapter.
- `R3` is the IO port; reading `R3` takes data from `DI`, and writing `R3` sends data to `DO`.

## Instruction Encoding

This chapter implements only two instructions: `NOP` and `MOV D, S`.

- `NOP`: `0000 0000` (`0x00`); it only increments `PC`.
- `MOV D, S`: `0011 SS DD`; it copies the value of source register `S` to destination register `D`.

| Instruction | Binary | Hex | Effect |
| --- | --- | --- | --- |
| `MOV R0, R1` | `0011 0100` | `0x34` | `R0 <- R1` |
| `MOV R1, R0` | `0011 0001` | `0x31` | `R1 <- R0` |
| `MOV R0, R3` | `0011 1100` | `0x3C` | `R0 <- DI` |
| `MOV R3, R0` | `0011 0011` | `0x33` | `DO <- R0` |
| `MOV R3, R3` | `0011 1111` | `0x3F` | `DI -> DO` |

## R3 Read/Write Handshake

When reading `R3`, if `VI=1`, the circuit samples `DI` at the next clock and raises `RI`; if `VI=0`, the destination register stays unchanged.

When writing `R3`, if `VO=0`, the source register is copied to the output register and `VO` becomes 1. When `VO=1` and `RO=1`, the output completes on this tick; new data may be written only on the next tick.

## A Simple Program

The following program sends the external input back unchanged:

```text
NOP
MOV R0, R3      ; 0x3C, read input; transfer completes when VI and RI are both 1
MOV R3, R0      ; 0x33, write output; wait for RO before the next write
NOP
```

Because there is no jump instruction yet, the program runs sequentially to the end. The test system supplies input and reads output on a fixed schedule.

At the start of each run, `PC`, `R0`, `R1`, and the other registers should all be 0.

## Text Format Examples

The <color=red>red text</color>, <color=blue>blue text</color>, and <color=#FFAA00>custom orange text</color> here demonstrate inline colors; <font color=gold>gold text</font> uses the alternative tag form.

This paragraph contains *italic text* and **bold text**.

**This entire line is bold, with <color=red>red</color>, <color=green>green</color>, and <color=#FFAA00>custom orange</color> text nested inside.**

---

![Chapter overview](../../textures/gui/experiment_table/chapters/intro/intro.png)

![Namespaced image](elements-plus:textures/gui/experiment_table/chapters/intro/intro.png)

![Missing image](../../textures/gui/experiment_table/chapters/intro/missing.png)

Inline component: `component::{"translate": "item.elements-plus.copper_wire"}`.

## Experiments

```epp-experiments
elements-plus:tff
elements-plus:tff_prime
```
