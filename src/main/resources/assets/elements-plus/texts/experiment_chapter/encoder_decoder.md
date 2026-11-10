<color=dark_purple>Not yet familiar with the concept of binary? You can complete the “<color=gold>One-bit Adder</color>” chapter first before coming back.</color>

There is a component that lights up a separate output line for each binary input, which is the <color=gold>Decoder</color>. Its reverse operation: outputting the lit input line as the corresponding binary number, which is the <color=gold>Encoder</color>.

All decoder logic can be generalized as:

Inputs A0~An, outputs Y0~Ym `(m=2^(n+1)-1)`. Only one output is 15, and the rest are all 0. According to the two states of the inputs, 0 and non-zero, corresponding to 0 and 1 respectively, they form the binary number x=(An...A2A1A0)₂. Which output is 15 depends on x; only Yx outputs 15.

To save component space, we pack the outputs into <color=blue>8-bit</color>. The upper Y0~Y7 correspond to bits 1~8 of the following Y or Y0 respectively, and Y8~Y15 correspond to bits 1~8 of Y1.

- <color=gold>2-4 Decoder</color>: Inputs A0~A1, output Y (packed Y0~Y3).
- <color=gold>3-8 Decoder</color>: Inputs A0~A2, output Y (packed Y0~Y7).
- <color=gold>4-16 Decoder</color> (Optional): Inputs A0~A3, outputs Y0 (packed Y0~Y7), Y1 (packed Y8~Y15).

```epp-experiments
elements-plus:decoder_2_4
elements-plus:decoder_3_8
elements-plus:decoder_4_16
```

Then there is the encoder. The logic of the encoder is the opposite of the decoder, and can be generalized as:

Inputs A0~Am `(m=2^(n+1)-1)`, outputs Y0~Yn. When an input is non-zero, output its binary representation (Yn...Y2Y1Y0)₂, where 0 corresponds to signal strength 0, and 1 corresponds to signal strength 15.

To avoid undefined behavior, we specify that if multiple inputs are non-zero at the same time, the output is the bitwise OR of the outputs corresponding to all non-zero inputs. For example, for an 8-3 encoder, when A5 (corresponding to output 101) and A6 (corresponding to output 110) are active at the same time, the output is 111. In practical applications, you should generally avoid multiple inputs being non-zero at the same time.

- <color=gold>4-2 Encoder</color>: Input A (packed A0~A3), outputs Y0~Y1.
- <color=gold>8-3 Encoder</color>: Input A (packed A0~A7), outputs Y0~Y2.
- <color=gold>16-4 Encoder</color> (Optional): Inputs A0 (packed A0~A7), A1 (packed A8~A15), outputs Y0~Y3.

```epp-experiments
elements-plus:encoder_4_2
elements-plus:encoder_8_3
elements-plus:encoder_16_4
```

There is also one called the “<color=gold>Priority Encoder</color>”. Unlike a normal encoder, when multiple inputs are non-zero, only the largest input actually takes effect. This is an optional experiment.

```epp-experiments
elements-plus:priority_encoder_4_2
elements-plus:priority_encoder_8_3
elements-plus:priority_encoder_16_4
```
