Now that we have built our first logic gate, let's move on to building more logic gates.

This chapter's logic gates will no longer have only one input, but usually have A and B two inputs.

- <color=gold>OR Gate</color>: If any input is non-zero, it outputs 15, otherwise it outputs 0.
- <color=gold>AND Gate</color>: If both inputs are non-zero, it outputs 15, otherwise it outputs 0.
- <color=gold>NOR Gate</color>: If any input is non-zero, it outputs 0, otherwise it outputs 15.
- <color=gold>NAND Gate</color>: If both inputs are non-zero, it outputs 0, otherwise it outputs 15.

```epp-experiments
elements-plus:or_gate
elements-plus:nor_gate
elements-plus:nand_gate
elements-plus:and_gate
```

Hint: NOR gate and NAND gate are not always just OR gate and AND gate with an additional NOT gate. For example, NAND gate actually needs fewer transistors than AND gate.

In the following chapters, we will frequently use these basic logic gates.

---

Additionally, this chapter also has an optional challenge:

- <color=gold>Short-circuit AND</color>: If A is non-zero, output B, otherwise output A
- <color=gold>Short-circuit OR</color>: If A is zero, output B, otherwise output A

```epp-experiments
elements-plus:and_gate_sc
elements-plus:or_gate_sc
```
