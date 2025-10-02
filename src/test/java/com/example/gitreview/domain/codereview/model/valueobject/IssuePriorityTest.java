package com.example.gitreview.domain.codereview.model.valueobject;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IssuePriority 枚举测试
 */
class IssuePriorityTest {

    @Test
    void testIssuePriorityValues() {
        assertEquals(4, ReviewResult.IssuePriority.values().length);
        assertEquals("P0", ReviewResult.IssuePriority.P0.getCode());
        assertEquals("P1", ReviewResult.IssuePriority.P1.getCode());
        assertEquals("P2", ReviewResult.IssuePriority.P2.getCode());
        assertEquals("P3", ReviewResult.IssuePriority.P3.getCode());
    }

    @Test
    void testPriorityDisplay() {
        assertEquals("阻断性", ReviewResult.IssuePriority.P0.getDisplayName());
        assertEquals("严重", ReviewResult.IssuePriority.P1.getDisplayName());
        assertEquals("一般", ReviewResult.IssuePriority.P2.getDisplayName());
        assertEquals("建议", ReviewResult.IssuePriority.P3.getDisplayName());
    }

    @Test
    void testPriorityEmoji() {
        assertEquals("🔴", ReviewResult.IssuePriority.P0.getEmoji());
        assertEquals("🟠", ReviewResult.IssuePriority.P1.getEmoji());
        assertEquals("🟡", ReviewResult.IssuePriority.P2.getEmoji());
        assertEquals("⚪", ReviewResult.IssuePriority.P3.getEmoji());
    }

    @Test
    void testPriorityLevel() {
        assertEquals(1, ReviewResult.IssuePriority.P0.getLevel());
        assertEquals(2, ReviewResult.IssuePriority.P1.getLevel());
        assertEquals(3, ReviewResult.IssuePriority.P2.getLevel());
        assertEquals(4, ReviewResult.IssuePriority.P3.getLevel());
    }

    @Test
    void testIsBlocking() {
        assertTrue(ReviewResult.IssuePriority.P0.isBlocking());
        assertFalse(ReviewResult.IssuePriority.P1.isBlocking());
        assertFalse(ReviewResult.IssuePriority.P2.isBlocking());
        assertFalse(ReviewResult.IssuePriority.P3.isBlocking());
    }

    @Test
    void testIsCritical() {
        assertTrue(ReviewResult.IssuePriority.P0.isCritical());
        assertTrue(ReviewResult.IssuePriority.P1.isCritical());
        assertFalse(ReviewResult.IssuePriority.P2.isCritical());
        assertFalse(ReviewResult.IssuePriority.P3.isCritical());
    }

    @Test
    void testFromCode() {
        assertEquals(ReviewResult.IssuePriority.P0, ReviewResult.IssuePriority.fromCode("P0"));
        assertEquals(ReviewResult.IssuePriority.P1, ReviewResult.IssuePriority.fromCode("p1"));
        assertEquals(ReviewResult.IssuePriority.P2, ReviewResult.IssuePriority.fromCode("P2"));
        assertEquals(ReviewResult.IssuePriority.P3, ReviewResult.IssuePriority.fromCode("p3"));
    }

    @Test
    void testFromCodeInvalid() {
        assertThrows(Exception.class, () -> ReviewResult.IssuePriority.fromCode("P4"));
        assertThrows(Exception.class, () -> ReviewResult.IssuePriority.fromCode("invalid"));
    }

    @Test
    void testToString() {
        String p0String = ReviewResult.IssuePriority.P0.toString();
        assertTrue(p0String.contains("🔴"));
        assertTrue(p0String.contains("P0"));
        assertTrue(p0String.contains("阻断性"));
    }
}
