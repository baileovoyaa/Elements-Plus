I'm sure you're already intimidated by the high cost of batteries, but it's okay. You don't need or recommend having a built-in battery for every chip - the chips you want to manufacture in the future will require more and more sub chips, and this "built-in battery" design will undoubtedly increase costs exponentially.

The experiment table will provide an additional optional input, "VCC", which you can connect to any pin of your circuit, and it will automatically receive a signal strength of 15. The VCC pin will not be explicitly exposed on the component; they will be implicitly connected to the outer layer one by one, so you only need one battery to power the entire machine.

You can also follow the same design when designing circuits: do not built-in batteries, but reserve input interfaces instead, which can significantly reduce the demand for batteries!

---

After making the basic logic gates, let's move on to two more advanced logic gates. These gates also have A and B two inputs and Y one output.

- <color=gold>XOR Gate</color>: Odd number of inputs non-zero, output 15; even number of inputs non-zero, output 0.
- <color=gold>XNOR Gate</color>: Odd number of inputs non-zero, output 0; even number of inputs non-zero, output 15.

You can try to solve it by combining the logic gates you have unlocked before.

```epp-experiments
elements-plus:xor_gate
elements-plus:xnor_gate
```

After completing these chapters, you can directly jump to “<color=gold>One-bit Adder</color>”, but it is recommended that you complete these chapters as soon as possible, as they can provide a lot of help in your future design. Moreover, they are prerequisite chapters for some optional chapters.

- <color=gold>Three-way Logic Gate</color>
- <color=gold>Multiplexer</color>
- <color=gold>Encoder and Decoder</color>
