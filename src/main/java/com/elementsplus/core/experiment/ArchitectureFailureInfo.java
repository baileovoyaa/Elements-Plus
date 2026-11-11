package com.elementsplus.core.experiment;

import java.util.List;

/**
 * 计算机架构测例失败时的逐 tick 数据。
 *
 * <p>架构测例使用外部虚拟 ROM：测试系统根据电路输出的 PC 取出程序字节，再作为 INS 输入电路。
 * 因此失败信息需要同时保存当前 tick 的 PC、INS、外部输入和电路输出，供客户端按固定列显示。
 * 其中 RI 是执行阶段采样的输入握手输出，VO/DO 是时钟步进后的输出寄存器状态。</p>
 */
public record ArchitectureFailureInfo(byte[] program, List<Row> rows, boolean copyWholeProgram) {

    public ArchitectureFailureInfo {
        program = program == null ? new byte[0] : program.clone();
    }

    public ArchitectureFailureInfo(byte[] program, List<Row> rows) {
        this(program, rows, false);
    }

    @Override
    public byte[] program() {
        return program.clone();
    }

    public record Row(int tick,
                      int pc,
                      int ins,
                      int expectedPc,
                      int expectedIns,
                      int vi,
                      int ri,
                      int di,
                      int vo,
                      int ro,
                      int doValue,
                      int expectedRi,
                      int expectedVo,
                      int expectedDo) {
    }
}
