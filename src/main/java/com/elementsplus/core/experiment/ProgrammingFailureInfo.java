package com.elementsplus.core.experiment;

/**
 * 编程实验输出错误（WA）时用于客户端展示的输入/输出数据。
 *
 * <p>inputRead 表示程序已经读取的输入字节数；wrongIndex 是第一个不匹配的输出字节下标。</p>
 */
public record ProgrammingFailureInfo(int testIndex, byte[] input, int inputRead,
                                     byte[] expectedOutput, byte[] actualOutput,
                                     int wrongIndex) {
    public ProgrammingFailureInfo {
        input = input == null ? new byte[0] : input.clone();
        expectedOutput = expectedOutput == null ? new byte[0] : expectedOutput.clone();
        actualOutput = actualOutput == null ? new byte[0] : actualOutput.clone();
    }

    @Override
    public byte[] input() {
        return input.clone();
    }

    @Override
    public byte[] expectedOutput() {
        return expectedOutput.clone();
    }

    @Override
    public byte[] actualOutput() {
        return actualOutput.clone();
    }
}
