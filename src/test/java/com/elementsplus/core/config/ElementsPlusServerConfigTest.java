package com.elementsplus.core.config;

import com.elementsplus.core.circuit.diagram.CircuitDiagram.Scale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自定义元件嵌套规则的真值表。
 *
 * <p>默认 {@code customComponentNestingLimit = 1}、{@code allowInfiniteInFiniteCircuit = false}：
 * 父电路规模必须比子元件大至少 1 级；无限规模元件只能放进无限规模电路。
 * 若修改这两个默认值，需要同步更新本测试。</p>
 */
class ElementsPlusServerConfigTest {

    @Test
    void nullScaleIsNeverNestable() {
        assertFalse(ElementsPlusServerConfig.canNest(null, Scale.SMALL));
        assertFalse(ElementsPlusServerConfig.canNest(Scale.SMALL, null));
        assertFalse(ElementsPlusServerConfig.canNest(null, null));
    }

    @Test
    void parentMustBeLargerThanChild() {
        assertFalse(ElementsPlusServerConfig.canNest(Scale.SMALL, Scale.SMALL));
        assertFalse(ElementsPlusServerConfig.canNest(Scale.MEDIUM, Scale.MEDIUM));
        assertTrue(ElementsPlusServerConfig.canNest(Scale.MEDIUM, Scale.SMALL));
        assertTrue(ElementsPlusServerConfig.canNest(Scale.LARGE, Scale.SMALL));
        assertTrue(ElementsPlusServerConfig.canNest(Scale.HUGE, Scale.LARGE));
        assertFalse(ElementsPlusServerConfig.canNest(Scale.SMALL, Scale.MEDIUM), "子元件不能比父电路更大");
    }

    @Test
    void infiniteParentAcceptsEverything() {
        assertTrue(ElementsPlusServerConfig.canNest(Scale.INFINITE, Scale.SMALL));
        assertTrue(ElementsPlusServerConfig.canNest(Scale.INFINITE, Scale.HUGE));
        assertFalse(ElementsPlusServerConfig.canNest(Scale.SMALL, Scale.INFINITE),
                "默认配置下无限规模元件不能放进有限电路");
    }
}
