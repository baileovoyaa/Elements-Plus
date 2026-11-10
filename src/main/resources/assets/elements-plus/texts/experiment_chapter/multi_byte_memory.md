A register can hold only one value at a time. When many values need to be saved and retrieved by number, a larger component is needed: <color=blue>memory</color>.

Memory is divided into many <color=blue>memory cells</color>, each storing one byte (8 bits, exactly one 8-bit bus). To distinguish the cells, each one is given an <color=blue>address</color>. When accessing data, giving the address finds the corresponding cell. Memory that can access any cell directly by address is called <color=gold>random-access memory</color> (RAM).

**“Random access” here means you can access any address directly instead of searching from front to back; it has nothing to do with random numbers.**

The memory components below store digital signals only. Inputs are treated as 0/non-zero, and outputs are 0 or 15; signal strength is not preserved.

---

First, look at the smallest <color=gold>4-byte RAM</color>.

- <color=gold>4-byte RAM</color>: inputs A (8-bit data), D (8-bit address, only the low 2 bits are valid), WE (1-bit write enable), output Y (8-bit data). It has 4 cells at addresses 0~3, corresponding to the low 2 bits of D; the upper 6 bits of D are ignored.

Its pins are:

- A (8-bit): data input, the data to write into memory;
- D (8-bit): address input, selects the memory cell to access;
- WE (1-bit): write enable, determines whether to write;
- Y (8-bit): data output, outputs the data currently stored at address D.

Its read/write rules are:

- **Reads are combinational**: during combinational evaluation, Y directly outputs the data at address D. That is, changing D immediately changes Y; no clock step is needed. WE does not participate in address selection, and Y always outputs the data at address D.
- **Writes are sequential**: on a clock step, if WE is non-zero, A's data is written to address D; if WE is zero, memory contents stay unchanged.
- Reads and writes use the same address D. On a clock step with WE non-zero, the data is written to address D and Y outputs the data at that address; because the write takes effect on the clock step, Y reflects the newly written value after the step.
- WE is not edge-triggered. As long as WE is non-zero on every clock step, a write occurs every tick; set WE to zero when you want to stop writing.

To use it, connect the address to D. For reads only, keep WE at 0 and take the result from Y; to write, put the data on A, make WE non-zero, step the clock, then set WE back to 0.

```epp-experiments
elements-plus:ram_4
```

Expanding the capacity gives 16-byte, 64-byte, and 256-byte versions. Their pins and read/write rules are identical; only the address width differs:

- <color=gold>16-byte RAM</color>: the low 4 bits of D are valid, giving 16 cells at addresses 0~15;
- <color=gold>64-byte RAM</color>: the low 6 bits of D are valid, giving 64 cells at addresses 0~63;
- <color=gold>256-byte RAM</color>: all 8 bits of D are valid, giving 256 cells at addresses 0~255.

All capacities use an 8-bit address so they can share the same address bus in a circuit; for smaller capacities, simply ignore the extra high bits.

Note that RAM is <color=blue>volatile memory</color>: its contents are valid only while the circuit is running. On reset or power loss all RAM data is lost, and it is not saved with the circuit diagram or chip. RAM is therefore suitable for temporary data during program execution; use ROM or a disk for data that must persist.

```epp-experiments
elements-plus:ram_16
elements-plus:ram_64
elements-plus:ram_256
```

The <color=gold>256-byte read-only memory</color> has fewer pins:

- D (8-bit): address input, selects the cell to read;
- Y (8-bit): data output, outputs the data stored at address D.

Unlike RAM, ROM has no WE and cannot be written while the circuit runs; it can only be read. Its contents are not runtime data but component configuration: they are written into the circuit diagram during design and are permanently preserved with the diagram or chip, surviving reset and power loss.

However, ROM is truly read-only: it cannot be erased or rewritten, and becomes immutable once compiled. The ROM contents in a compiled circuit diagram or chip are fixed; if there is a mistake, you must change the design and compile again.

Before compiling, you can double-click a ROM component in the design screen to open its editor and directly edit the 256 bytes. Since entering bytes one by one is tedious, the editor supports copying data from a disk: prepare the contents on a disk first, then import them all at once.

- <color=gold>256-byte ROM</color>: input D (8-bit address), output Y (8-bit data). It has 256 cells at addresses 0~255.

This experiment is optional; you can skip it for now if you do not need ROM yet.

---

Building a 256-byte ROM by hand is tedious, so you only need a 16-byte ROM to pass the experiment. Make its low 4 address bits valid. The contents are:

```epp-text
elements-plus:rom_values
```

When manufacturing the ROM chip itself, the required materials are multiplied by 16.

```epp-experiments
elements-plus:rom_256
```

Finally, let us introduce the <color=gold>disk</color>.

A disk is a craftable item. Unlike the two memory components above, its data is stored in the disk item itself, not in a component in the circuit diagram.

A disk component placed in a circuit diagram is only a <color=blue>placeholder</color>, indicating “a disk will be connected here.” The real data is stored in the disk item connected at the actual IO interface. Without a real disk item, the placeholder itself has no data.

Once a real disk item is connected, its data survives circuit reset and power loss and can store programs and data for a long time. Compared with RAM, a disk is <color=blue>non-volatile</color>; compared with ROM, a disk can be replaced and its contents can be modified, while ROM contents are fixed after compilation.

You can edit disk contents in the experiment table; the ROM editor mentioned above can also copy data directly from a disk.

In the later computer architecture chapters, we will put programs and data into memory and let the computer access them by address. You will then see that the two concepts “address + data” connect computation, storage, and control.
