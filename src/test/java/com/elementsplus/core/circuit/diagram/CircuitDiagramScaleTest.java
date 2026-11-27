package com.elementsplus.core.circuit.diagram;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 电路图规模（Scale）的尺寸与升级规则；芯片容量与嵌套判定都依赖这些值。 */
class CircuitDiagramScaleTest {

    @Test
    void infiniteIsOnlyScaleWithoutBoundary() {
        assertTrue(CircuitDiagram.Scale.INFINITE.isInfinite());
        assertFalse(CircuitDiagram.Scale.SMALL.isInfinite());
        assertEquals(-1, CircuitDiagram.Scale.INFINITE.sizeInCells());
        assertEquals(32, CircuitDiagram.Scale.SMALL.sizeInCells());
        assertEquals(256, CircuitDiagram.Scale.HUGE.sizeInCells());
    }

    @Test
    void upgradeStopsAtHuge() {
        assertEquals(CircuitDiagram.Scale.MEDIUM, CircuitDiagram.Scale.SMALL.upgrade());
        assertEquals(CircuitDiagram.Scale.LARGE, CircuitDiagram.Scale.MEDIUM.upgrade());
        assertEquals(CircuitDiagram.Scale.HUGE, CircuitDiagram.Scale.LARGE.upgrade());
        assertEquals(CircuitDiagram.Scale.HUGE, CircuitDiagram.Scale.HUGE.upgrade());
        assertEquals(CircuitDiagram.Scale.INFINITE, CircuitDiagram.Scale.INFINITE.upgrade());
    }

    @Test
    void onlyUpgradableScalesUseUpgradePaper() {
        assertTrue(CircuitDiagram.Scale.SMALL.canUpgrade());
        assertTrue(CircuitDiagram.Scale.LARGE.canUpgrade());
        assertFalse(CircuitDiagram.Scale.HUGE.canUpgrade());
        assertFalse(CircuitDiagram.Scale.INFINITE.canUpgrade());
    }
}
