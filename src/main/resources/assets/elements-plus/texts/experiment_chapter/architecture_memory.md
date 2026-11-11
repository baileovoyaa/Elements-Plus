The previous two chapters could only operate between registers, immediates, and IO; they could not store large amounts of data. This chapter connects a 256-byte RAM to `R2`, allowing the computer to save and load data. It still has no ALU; it only adds data memory.

The program ROM remains external and is addressed by `PC`. This chapter adds **data RAM**, addressed by `R1` and read/written through `R2`. The program ROM and data RAM have separate address spaces; `PC` never accesses data RAM.

## Wiring

The RAM is the <font color=gold>256-byte random-access memory</font> built in the <font color=gold>Multi-byte Memory</font> chapter. Its pins are:

- `D` (8-bit): address input;
- `A` (8-bit): data input;
- `WE` (1-bit): write enable;
- `Y` (8-bit): data output.

Connections:

- The 8-bit output of `R1` goes to RAM `D` (address input).
- When writing `R2`, the data to write goes to RAM `A`, and `WE=1`.
- When reading `R2`, set `WE=0` and select RAM `Y` as the read data.
- RAM reads are combinational; writes happen on the clock edge.

Thus `R2` is not an independent register; it is an alias for "the memory cell currently addressed by `R1`". When `R1` changes, the cell that `R2` refers to changes too.

## Set the Address First

Memory access always follows this order:

1. Write the address into `R1` with `LDL`/`LDH` (or `MOV`).
2. Then use `MOV` with `R2`:
   - `MOV R0, R2`: read `mem[R1]` into `R0`;
   - `MOV R2, R0`: write `R0` to `mem[R1]`.

For example, write `0x5A` to address `0x20`, then read it back:

```text
; R1 = 0x20
LDL R1, #0x0    ; 0x50
LDH R1, #0x2    ; 0x92

; R0 = 0x5A
LDL R0, #0xA    ; 0x4A
LDH R0, #0x5    ; 0x85

MOV R2, R0      ; 0x32, mem[0x20] <- 0x5A
NOP
MOV R0, R2      ; 0x38, R0 <- mem[0x20]
MOV R3, R0      ; 0x33, output 0x5A
```

If you read or write before setting the address, you access the cell pointed to by the old `R1`. The address must be prepared before access and must remain stable during the access.

## Why R2 cannot use immediates

`R2` is a mapping of `mem[R1]`, not an independent register. If `LDL R2, #n` were allowed, its semantics would become:

1. Read `mem[R1]`.
2. Modify its low 4 bits.
3. Write it back to `mem[R1]`.

That requires one memory read and one memory write and changes memory contents; it is no longer "set half a register" but a complex read-modify-write instruction. To keep instructions simple, the `RR=10` encodings of `LDL`/`LDH` are reserved and the basic architecture treats them as NOP.

To put a constant into memory, load it into `R0` first and then use `MOV R2, R0`. `R2` itself has no independent storage, so it cannot preserve a nibble like `R0`/`R1`.

Likewise, `R3` is still the IO port and cannot use immediates, as explained in the previous chapter.

## Decode and Writeback

In this chapter, the `MOV` source/destination selection gains an `R2` path:

- `R2` as source: the read data comes from RAM `Y`, not an ordinary register.
- `R2` as destination: the write enable goes to RAM `WE`, and the write data goes to RAM `A`.
- `MOV R2, R2` has no practical effect and may be treated as a no-op.
- When writing `R2`, the source may be `R0`/`R1`/`R3`; if the source is `R3`, complete the input handshake first and then write the read data to memory.

RAM writes must only be enabled when the `MOV` destination is `R2`; all other instructions must keep `WE=0` to avoid accidental writes.

## Notes

- RAM contents only exist while the circuit is running and are lost on reset or power loss; test programs always write before reading.
- `R1` is the data address and `PC` is the program address; they do not affect each other.
- This chapter adds no new instructions; it still uses `MOV`, `LDL`, `LDH`, and `NOP`.
- There are still no jumps, so the program executes sequentially by `PC`; to access multiple addresses, set `R1` with `LDL`/`LDH` for each address.

```epp-experiments
elements-plus:architecture_memory
```

This experiment is still not associated with a component.
