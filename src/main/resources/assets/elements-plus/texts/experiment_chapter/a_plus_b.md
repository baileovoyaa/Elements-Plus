Before building the computer architecture, let us do a small experiment that actually solves a problem with a circuit and gets us familiar with input and output.

In this experiment you need to interact with the test system through inputs and outputs, receive two numbers from the outside, add them, and output the result.

In real applications, input data is usually not immediately available, and output data cannot always be processed immediately. We therefore need a blocking input/output convention: the circuit must wait when input data is not ready, and must also wait when output data is not ready.

This chapter uses <color=blue>same-tick transfer</color>: a transfer is completed on the tick when VI/RI on the input side or VO/RO on the output side are both non-zero. D must be valid on that tick; it cannot be delayed to the next tick.

---

Specifically, when input data is ready, the outside raises VI and puts the data on DI; DI stays unchanged while VI is valid. If the circuit is ready to receive, it raises RI. The tick when VI and RI are both non-zero is the <color=blue>transfer tick</color>: the circuit saves DI into a register on that tick's clock step. Only after the transfer is complete may the outside change DI; if there is no next data for the moment, VI is withdrawn.

When the circuit is ready to output, it raises VO and puts the data on DO; DO stays unchanged while VO is valid. The outside raises RO when it is ready to receive. The tick when VO and RO are both non-zero is the <color=blue>transfer tick</color>: the outside reads DO before that tick's clock step. Only after the transfer is complete may the circuit withdraw VO or change DO.

The inputs and outputs involved in this experiment are:

---

Inputs:

- VI (1-bit): indicates that data is waiting to be read
- RO (1-bit): indicates that the outside is ready to read the circuit output
- DI (8-bit): input data. It stays unchanged while VI is valid; on a transfer tick where VI and RI are both non-zero, the circuit should save DI into a register at the clock step and use the saved value afterwards.

Outputs:

- VO (1-bit): indicates that there is data to output
- RI (1-bit): indicates that the circuit is ready to receive input
- DO (8-bit): output data. It stays unchanged while VO is valid; on a transfer tick where VO and RO are both non-zero, the outside reads DO before the clock step.
---

Input data is not necessarily ready immediately, so your circuit needs to wait until VI is valid.

In the following, V and R mean VI and RI on the input side, and VO and RO on the output side.

Only when VI and RI are both non-zero is DI actually read; likewise, only when VO and RO are both non-zero is DO actually read. D must already be valid on this <color=blue>transfer tick</color>; it cannot be delayed to the next tick.

**Note**: on the same tick, D is only required to be valid on the transfer tick; you do not have to compute the result on that tick. You may wait for data first, take several ticks to calculate, and only raise RI or VO when ready. However, after V and R are both non-zero, D cannot be delayed by another tick.

For input, if VI=1 and RI=0, the outside keeps DI unchanged; for output, if VO=1 and RO=0, the circuit should keep DO unchanged. Only the tick when V and R are both non-zero completes the transfer.

The clock step is the sampling moment: DI is stable before the step and is sampled by the circuit during it; DO is read by the outside before the step, and the circuit may change it only after the step.

For example, on the input side: on tick 5, VI=1, RI=1, and DI=42. The circuit saves 42 at the clock step of tick 5 and can use the saved value from tick 6 onward; the outside changes DI to the next number only after the step of tick 5.

The output side works the same way: on tick 8, VO=1, RO=1, and DO=42. The outside reads 42 before the clock step of tick 8; only after the step of tick 8 may the circuit withdraw VO or change DO.

You may therefore decide for yourself when to be ready to receive input and when to be ready to output the result, but you cannot wait indefinitely; the test cases in this experiment have a time limit.

For this reason, we introduce a new test-case failure reason:

- <color=light_purple>TLE (Time Limit Exceeded)</color>: exceeds the maximum number of ticks allowed by the test case

Each test case must produce its output within 20 ticks. The test cases guarantee that both input values are ready within 10 ticks; after your VO becomes valid, the outside guarantees to raise RO within 2 ticks. Once output is produced, the outside immediately checks whether it is correct and moves on to the next test case.

```epp-experiments
elements-plus:a_plus_b
```

This experiment is no longer associated with a component.

---

20 ticks is actually quite generous; in theory input and output can be completed in 2 ticks — read the first number on the first tick, then read the second number and output the result on the second tick. However, this cannot verify the circuit's blocking input/output behavior.

This experiment makes both input and output ready immediately, so you do not need to wait for any data. Therefore VI is 1 for both ticks, and VO should also be 1 on the second tick. RO is guaranteed to be 1 on the second tick (the same tick when VO is 1).

```epp-experiments
elements-plus:a_plus_b_extreme
```
