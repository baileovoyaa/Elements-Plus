Next, we will enter <color=blue>sequential logic</color>.

Previously we built <color=blue>combinational logic</color>. Do you remember the concept mentioned earlier: <color=blue>combinational logic</color> is a digital circuit whose output depends only on the current input state and is independent of past circuit states.

By contrast, <color=blue>sequential logic</color> is different. Its output depends not only on the current input but also on the circuit's previous state—that is, on earlier inputs.

The core of sequential logic is the <color=blue>clock</color> and the <color=blue>flip-flop</color>. The clock is the unit of circuit time; one clock cycle is called a “tick”. A flip-flop is a component that, at a clock edge, determines its output from the previous tick's input.

To achieve this, we introduce a new basic component—the <color=gold>capacitor</color>. It delays the input signal by one tick, thereby briefly storing it.

The capacitor's charge and discharge speeds can be configured; they represent the maximum signal-strength change per tick. This is usually used in analog circuits. By default, both are 15, meaning the signal will match the input immediately after one tick, with no transition.

Therefore, the capacitor itself can be seen as a flip-flop—its output depends on the previous tick's input. We call this a “D flip-flop”.

Let's try using it. In this experiment, we will build a “<color=gold>T flip-flop</color>”—

- <color=gold>T flip-flop</color>: Input T, output Q. When T is 0, the output at the next tick holds the current output; when T is non-zero, the output toggles (0 or 15). In other words, when the clock steps, if T is 0 the output stays unchanged; if T is non-zero the output toggles.

```epp-experiments
elements-plus:tff
```

If the input stays non-zero, the output toggles every tick, which is exactly the expected behavior.

---

There is another kind that toggles only when the input changes from 0 to non-zero (previous tick 0, current tick non-zero). It can be used as a toggle switch. In redstone circuits it is often called a “T flip-flop” or “toggle latch”; here, to distinguish it, we call it a “<color=gold>T' flip-flop</color>”.

- <color=gold>T' flip-flop</color>: Input C, output Q. It toggles the output at the next tick only when C is 1 this tick and was 0 last tick; otherwise the output holds.

```epp-experiments
elements-plus:tff_prime
```
