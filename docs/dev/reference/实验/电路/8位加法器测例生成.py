# -*- coding: utf-8 -*-

def build_fa_tests():
    """
    第 1 组：64 个。
    对每个位 i=0..7，覆盖该位全加器的 8 种输入：
    (a_i, b_i, c_i) = (0/1, 0/1, 0/1)
    """
    tests = []
    for i in range(8):
        for a in (0, 1):
            for b in (0, 1):
                for c in (0, 1):
                    A = 0
                    B = 0
                    Cin = 0

                    if c == 0:
                        # 让第 i 位进位 c_i = 0
                        Cin = 0
                    else:
                        # 让第 i 位进位 c_i = 1
                        if i == 0:
                            Cin = 1
                        else:
                            # 低 i 位设为 A=2^i-1, B=1，产生进位到第 i 位
                            A |= (1 << i) - 1
                            B |= 1
                            Cin = 0

                    # 设置第 i 位的 a_i, b_i
                    if a:
                        A |= (1 << i)
                    if b:
                        B |= (1 << i)

                    tests.append((A, B, Cin))
    return tests


def build_carry_tests():
    """
    第 2 组：32 个。
    进位链、边界、溢出、交替进位传播等定向测试。
    """
    pairs = [
        (0x00, 0x00),  # 全零
        (0xFF, 0x00),  # 全 1 + 0
        (0x00, 0xFF),  # 0 + 全 1
        (0xFF, 0x01),  # 进位产生并溢出
        (0x01, 0xFF),  # 另一方向进位
        (0x80, 0x80),  # 最高位进位
        (0x7F, 0x80),  # 边界不进位
        (0x80, 0x7F),  # 边界反向
        (0xAA, 0x55),  # 交替进位传播
        (0x55, 0xAA),  # 反向交替
        (0xF0, 0x0F),  # 半字节边界
        (0x0F, 0xF0),  # 半字节边界反向
        (0xFE, 0x01),  # 接近全 1
        (0xFF, 0xFF),  # 全 1 溢出
        (0x00, 0x01),  # 最低位进位
        (0x01, 0x00),  # 最低位进位反向
    ]

    tests = []
    for A, B in pairs:
        tests.append((A, B, 0))
        tests.append((A, B, 1))
    return tests


def linear_cover_256():
    """
    生成 256 个线性组合覆盖向量。
    这 17 个输入位由 8 位种子 s 线性生成，
    任意 3 个输入位线性无关，因此完整 256 个向量可覆盖任意 3 位组合。
    """
    tests = []
    for s in range(256):
        b = [(s >> k) & 1 for k in range(8)]  # b[0]..b[7]

        A_bits = [
            b[0], b[1], b[2], b[3],
            b[4], b[5], b[6], b[7],
        ]

        B_bits = [
            b[0] ^ b[1] ^ b[2],
            b[0] ^ b[1] ^ b[3],
            b[0] ^ b[2] ^ b[4],
            b[0] ^ b[3] ^ b[5],
            b[0] ^ b[4] ^ b[6],
            b[0] ^ b[5] ^ b[7],
            b[1] ^ b[2] ^ b[5],
            b[1] ^ b[3] ^ b[6],
        ]

        Cin = b[2] ^ b[3] ^ b[7]

        A = sum(bit << i for i, bit in enumerate(A_bits))
        B = sum(bit << i for i, bit in enumerate(B_bits))
        tests.append((A, B, Cin))
    return tests


def build_all_groups():
    fa_tests = build_fa_tests()          # 64
    carry_tests = build_carry_tests()    # 32

    used = set(fa_tests) | set(carry_tests)

    # 第 3 组：从 256 个线性覆盖向量中，取出不在前两组里的前 160 个
    linear = linear_cover_256()
    combo_tests = [t for t in linear if t not in used]
    combo_tests = combo_tests[:160]

    return fa_tests, carry_tests, combo_tests


def print_group(name, tests):
    print("=" * 80)
    print(f"{name}：{len(tests)} 个")
    print("=" * 80)
    for idx, (A, B, Cin) in enumerate(tests, 1):
        print(
            f"{idx:3d}: "
            f"A=0x{A:02X} ({A:08b})  "
            f"B=0x{B:02X} ({B:08b})  "
            f"Cin={Cin}  "
            f"-> (A, B, Cin)=({A}, {B}, {Cin})"
        )
    print()


if __name__ == "__main__":
    fa_tests, carry_tests, combo_tests = build_all_groups()

    print_group("第 1 组：全加器局部输入穷举", fa_tests)
    print_group("第 2 组：进位链/边界定向测试", carry_tests)
    print_group("第 3 组：3-way 组合覆盖/补充测试", combo_tests)

    total = len(fa_tests) + len(carry_tests) + len(combo_tests)
    print("=" * 80)
    print(f"总测例数：{len(fa_tests)} + {len(carry_tests)} + {len(combo_tests)} = {total}")
    print("=" * 80)