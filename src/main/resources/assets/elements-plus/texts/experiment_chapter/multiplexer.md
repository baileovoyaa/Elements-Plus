<color=gold>Multiplexer</color> can select one of multiple inputs as output, which is very helpful when designing Arithmetic Logic Unit.

<color=red>Note: Due to the complex logic of this circuit, it is difficult to preserve the signal strength. Therefore, the input D, D0, D1, D2, and D3 involved in the following experiment only consider two cases: 0 and non-zero. Non zero is considered as 15, rather than outputting the original signal strength. Below is referred to as "<color=blue>Amplified Value</color>".</color>

Firstly, we have <color=gold>2-to-1 Multiplexer</color>: Input A, D0, D1. When A is 0, output Y is the amplified value of D0. When A is non-zero, output Y is the amplified value of D1.

If you still don't know how to do it, think about what component in the <color=blue>Basic Gate</color> we did earlier has the most similar function to a "switch"?

```epp-experiments
elements-plus:mux_2
```

Then we extend the input to 4 lines, and we have <color=gold>4-to-1 Multiplexer</color>.

Inputs A0, A1, D0, D1, D2, D3, Output Y.

- When A1 is 0 and A0 is 0, output D0's amplified value.
- When A1 is 0 and A0 is non-zero, output D1's amplified value.
- When A1 is non-zero and A0 is 0, output D2's amplified value.
- When A1 is non-zero and A0 is non-zero, output D3's amplified value.

Hint: Four-to-one, is equivalent to first four-to-two and then two-to-one.

```epp-experiments
elements-plus:mux_4
```

Then we stack 8 lines, and we have <color=gold>8-to-1 Multiplexer</color>. It can select <color=blue>8-bit Bus</color>.

<color=dark_purple>Not yet understand the concept of <color=blue>Bus</color>? You can complete “<color=gold>Multi-bit Adder</color>” chapter first before coming back.</color>

The characteristics of 8-bit multiplexer are the same as 1-bit, just with input D0~D4 and output Y changed to <color=blue>8-bit</color>.

```epp-experiments
elements-plus:mux_2_8
elements-plus:mux_4_8
```

There is another useful component called “<color=gold>8-bit Tri-state Buffer</color>”, which acts similarly to a switch, only outputting a signal when “enabled”, otherwise outputting 0. Therefore, through this component, multiple data outputs can be easily connected to a shared bus, as long as only one is valid at any time to avoid data interference. Although the 8-bit operation corresponding to the components at the beginning can also be achieved, it is slightly more tedious. Encapsulating it into a single component can simplify the logic.

It has two inputs: enable input A and data input D, one output Y. Where D and Y are both <color=blue>8-bit</color>. When A is non-zero, output Y as the amplified value of D; When A is 0, the output Y is all 0.

```epp-experiments
elements-plus:tri_state_buffer_8
```

Another option is to try the "%1$s". This is left as an optional experiment for you. Completing the optional experiments "%3$s" and "%4$s" in "%2$s" chapter may make it easier.<color=gold>Analog Multiplexer</color><color=blue>Basic Gate</color><color=gold>Short-circuit AND</color><color=gold>Short-circuit OR</color>

```epp-experiments
elements-plus:mux_analog_2
elements-plus:mux_analog_4
elements-plus:mux_analog_2_8
elements-plus:mux_analog_4_8
elements-plus:tri_state_buffer_analog_8
```
