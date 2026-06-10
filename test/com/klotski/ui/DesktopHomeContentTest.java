package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.klotski.core.SaveManager;
import org.junit.jupiter.api.Test;

class DesktopHomeContentTest {
    @Test
    void recordsSummaryShowsAllSupportedPuzzleSizes() {
        String text = DesktopHomeContent.recordsSummary(
                new SaveManager.BestRecord(12, 34_000),
                null,
                new SaveManager.BestRecord(56, 78_000));

        assertTrue(text.contains("3x3: 12 moves, 34s"));
        assertTrue(text.contains("4x4: --"));
        assertTrue(text.contains("5x5: 56 moves, 78s"));
        assertTrue(text.contains("Solver-assisted"));
    }

    @Test
    void preferencesDescriptionKeepsSettingsPresentationOnly() {
        String text = DesktopHomeContent.preferencesDescription();

        assertTrue(text.contains("presentation"));
        assertTrue(text.contains("Puzzle rules"));
        assertTrue(text.contains("records"));
    }
}
