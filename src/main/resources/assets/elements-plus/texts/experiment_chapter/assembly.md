You have built a computer architecture with storage, computation, and control. It can execute any calculation in principle. In this chapter you will write assembly programs for it and solve data-processing tasks without rebuilding a circuit for every program.

First compile the architecture from the previous chapters — the one with input/output, storage, immediates, computation, and jumps — into the <font color=gold>8-bit Basic MCU</font>. It executes the assembly programs in this chapter. The associated experiment covers all instructions and requires each instruction to finish in one tick. Reserved instruction encodings must behave as NOP and must not disturb execution; the tests also insert reserved bytes.

```epp-experiments
elements-plus:mcu_8_basic
```

The architecture is enough for simple programs and is also sufficient for the upcoming experiments. For your own system, You can also design your own architecture, instruction set, and custom components. You can expand functionality, use more bit addresses, variable length instructions, and even specialized circuits to break the limitations of 256 and unleash the infinite potential of architecture.

The optional targets are an <font color=gold>8-bit Advanced MCU</font> and a <font color=gold>16-bit MCU</font>; those chapters will add instructions for more complex operations. These can be very useful when you use them to control interfaces and robots, deal with various situations, and design complex algorithms. It can be selectively done according to actual needs.

---

## Pseudo Instructions

The assembler supports a few <font color=blue>pseudo instructions</font> that expand into multiple real instructions:

| Pseudo instruction | Expansion | Notes |
| --- | --- | --- |
| `LDI Rd, #imm8` | `LDL Rd, #lo`; `LDH Rd, #hi` | `Rd` must be `R0` or `R1` |
| `SETTR #imm8` | `LDI R0, #imm8`; `SETTR R0` | Overwrites `R0` |
| `JMP #label` | `SETTR #label`; `JMP` | Overwrites `R0` |

- There is no conditional `Jcc #label` pseudo instruction because setting a target overwrites `R0`, which is usually also the condition value. Set the target first, then prepare `R0`, then execute the conditional jump. 
- You can use `R1` as the target register if you need to preserve `R0`.

---

## Programming Experiments

These experiments do not use a circuit diagram. 

Put a disk or punched file containing the **program bytes** in the device slot and press Run. The program is interpreted directly. The tests only care about output, not the exact tick at which it appears. In order to maximize performance, most experiments, unless explicitly stated, will immediately prepare input data and receive output, and your program does not need to wait.

Note: changing program file contents while an experiment is running aborts the experiment.

Experiments usually provide input-output examples and data ranges. There is also a requirement for the maximum number of ticks. You need to complete the output within the given tick. As long as all expected outputs are completed, they will be considered correct; if one byte is output incorrectly, the test case will be considered as an error.

Some experiments do not require every test to pass. They run all tests and calculate a score. A failing test does not stop later tests. If the score reaches the required threshold, the experiment succeeds. Whether or not it succeeds, if there is a failed test, the status button can be clicked to inspect the first failure.

Next, we will implement a simple function that will help you quickly understand the relevant operations.

---

## Echo

Read input bytes and output them unchanged.

**Example input (hex):**

```
48 65 6C 6C 6F 20 77 6F 72 6C 64 21
```

**Example output (hex):**

```
48 65 6C 6C 6F 20 77 6F 72 6C 64 21
```

**Data range:** every test has at most 100 input bytes.

**Time limit:** 512 ticks per test.

**Pass requirement:** all tests.

---

Create a file in **Experiment Table Memory** or **Client Files**, enter the program below, After the semicolon is the <font color=blue>comment</font>, which explains the previous instructions and does not require input.

```
start:
    MOV R3, R3              ; DI -> DO
    JMP #start              ; loop
```

`JMP #start` is a pseudo instruction that expands to:

```
MOV R3, R3
LDL R0, #0x0
LDH R0, #0x0
SETTR R0
JMP
```

Place a disk or paper in the slot, click the assemble button to write the assembled program bytes to the disk or punched file, then select the experiment below and click the run button.

```epp-experiments
elements-plus:asm_echo
```

---

Now that you are familiar with the relevant operations, let's try the following experiment.

## Single-byte A+B

Read two bytes, output their sum with 8-bit wraparound, then return to the beginning for the next pair.

**Example input (hex):**

```
01 02 0A 0B FF 01 AA BB
```

**Example output (hex):**

```
03 15 00 65
```

**Data range**

- 60% of tests: at most 10 pairs (20 bytes) of input.
- 80% of tests: at most 40 pairs.
- 100% of tests: at most 80 pairs.

**Time limit:** 512 ticks per test.

**Pass requirement:** pass at least 60% of tests.

```epp-experiments
elements-plus:asm_add_byte
```
