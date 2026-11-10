Signals have 0~15 strengths in total, but we mostly focus on "yes" and "no".

To achieve this, we introduce a simple component - the <color=gold>amplifier</color>. As long as the input signal strength is greater than 0, the output signal strength is 15. Thus, we can focus on higher-level logic without worrying about the details.

To achieve this, we need a component that can amplify the signal strength. And that is the <color=gold>Transistor</color>.

A transistor is a component that can amplify signals. When there is no input on the side, it can amplify the input on the front by two times. (Side input will be mentioned later, this experiment does not require it)

Now, try to use this property to complete the experiment.

---

Input A, as long as A is non-zero, output Y=15, otherwise output Y=0.

Please build the circuit in the lithography machine, and place the circuit diagram into the slot above after completion. Click the "Start Experiment" button, and the experiment will be automatically performed.

```epp-experiments
elements-plus:amplifier
```

You should see below the "Test Information" appear multiple boxes, each corresponding to a test case. The text above the test case means:

- <color=gray>Waiting</color>: Test has not started
- <color=blue>Running</color>: Test is still running (you will see in the sequential logic that will be introduced later)
- <color=green>AC (Accepted)</color>: Test passed
- <color=red>WA (Wrong Answer)</color>: Test failed
