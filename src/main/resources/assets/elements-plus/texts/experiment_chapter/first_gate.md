Good job, you should now have passed your first experiment and obtained a compiled circuit diagram.

A compiled circuit diagram can be used directly as a component in the lithography machine design page, and you can also add it to the component list.

You should have already noticed the lithography machine manufacturing page, and it is now time to understand how to use it.

The lithography machine manufacturing page can copy circuits, manufacture chips and circuit boards.

If you want to copy a circuit, please place the circuit in the first slot as a template, and the second slot with the uncompiled circuit to be covered. At this time, a copy of the template circuit will appear in the output slot. Take it away, and the template will not be consumed, and the second slot circuit will be consumed. (Note: This operation will permanently cover the second slot circuit, and it is recommended to use an empty circuit)

If you want to make a chip, please place the compiled circuit diagram in the first grid as a template, and place the empty chip that **greater than or equal to circuit diagram scale** in the second grid. At this time, the raw materials required for making the chip will be displayed.

Among them,

- Basic components usually need the corresponding amethyst component items
- Advanced components need corresponding chips of any scale (manufactured by this method)
- Wires need corresponding block number of `component::{"translate":"item.elements-plus.copper_wire"}` and `component::{"translate":"item.elements-plus.gold_wire"}` items (bus multiplied by bit width)
- Input/output and bus joiner/splitter does not require raw materials

When all raw materials are satisfied in the inventory, a chip will appear in the output slot. Take it away, and the empty chip and raw materials will be deducted.

---

Now that we have learned how to make chips, we will move on to making the first logic gate.

We have already introduced the amplification function of transistors, and now we introduce the side input of transistors. The side input will make the output linearly decay. 0 corresponds to 100% output, and 15 corresponds to 0% output (that is, completely shutting down the output).

If you are more comfortable using formulas, then in other words: the final output ratio of the transistor is `2*(1-side_input/15)`. That is, the output is `2*front_input*(1-side_input/15)`.

This time, we are going to design the component "<color=gold>NOT Gate</color>". It is very simple——it has only one input and one output. You just need to ensure that it outputs 15 when the input is 0, and outputs 0 when the input is not 0.

Hint: When the input is 0, the component cannot produce a signal out of thin air. Think of which basic component can produce a signal continuously?

```epp-experiments
elements-plus:not_gate
```

In fact, with this component, you can create any complex <color=blue>combinational logic circuit</color>.

What we call a <color=blue>combinational logic circuit</color> is a digital circuit whose output depends only on the current input state and not on the past state of the circuit. If you understand this, then congratulations, you still have some basic knowledge. If you find it difficult to understand, that's okay, you will gradually understand it later.

If you think it's too simple, here's a challenge for you: design a component that outputs `15-input`. We call it the "<color=gold>Analog NOT Gate</color>".

This experiment is optional, you can continue to the next chapter and come back to do it later.

```epp-experiments
elements-plus:analog_not
```
