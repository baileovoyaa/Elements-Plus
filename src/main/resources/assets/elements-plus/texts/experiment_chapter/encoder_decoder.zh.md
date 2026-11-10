<color=dark_purple>还不了解二进制的概念？可以先完成“<color=gold>一位加法器</color>”章节后再来。</color>

有一种元件，对每个二进制输入都会点亮一条单独的输出线，这就是<color=gold>译码器</color>。而它的反向操作：将点亮的输入线输出为对应的二进制，这就是<color=gold>编码器</color>。

所有的译码器逻辑都可以统称为：

输入A0~An，输出Y0~Ym `(m=2^(n+1)-1)`。只有一个输出为15，其余的均为0。根据输入的0和非0两种状态分别对应0和1，组成二进制数x=(An...A2A1A0)₂，具体哪个输出15取决于x，只有Yx输出15。

为了节省元件空间，我们将输出打包成<color=blue>8位</color>。其中上面Y0~Y7分别对应下面Y或Y0的第1~8位，Y8~Y15对应Y1的第1~8位。

- <color=gold>2-4译码器</color>：输入A0~A1，输出Y（打包Y0~Y3）
- <color=gold>3-8译码器</color>：输入A0~A2，输出Y（打包Y0~Y7）
- <color=gold>4-16译码器</color>（可选）：输入A0~A3，输出Y0（打包Y0~Y7）、Y1（打包Y8~Y15）

```epp-experiments
elements-plus:decoder_2_4
elements-plus:decoder_3_8
elements-plus:decoder_4_16
```

然后是编码器。编码器的逻辑和译码器相反，可以统称为：

输入A0~Am `(m=2^(n+1)-1)`，输出Y0~Yn。某个输入端为非0时，输出其二进制表示(Yn...Y2Y1Y0)₂，其中0对应信号强度0，1对应信号强度15。

为避免未定义行为，这里规定如果有多个输入端同时非0，则输出所有非0输入对应输出的按位或的值，例如8-3编码器，A5（对应输出101）和A6（对应输出110）同时激活时，输出111。实际应用时通常应该避免多个输入端同时非0。

- <color=gold>4-2编码器</color>：输入A（打包A0~A3），输出Y0~Y1
- <color=gold>8-3编码器</color>：输入A（打包A0~A7），输出Y0~Y2
- <color=gold>16-4编码器</color>（可选）：输入A0（打包A0~A7）、A1（打包A8~A15），输出Y0~Y3

```epp-experiments
elements-plus:encoder_4_2
elements-plus:encoder_8_3
elements-plus:encoder_16_4
```

还有一个叫做“<color=gold>优先编码器</color>”。和普通编码器的不同是，当多个输入端非0时，只有最大的输入端会实际发挥作用。这里作为可选实验。

```epp-experiments
elements-plus:priority_encoder_4_2
elements-plus:priority_encoder_8_3
elements-plus:priority_encoder_16_4
```
