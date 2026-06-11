package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.klotski.core.SaveManager;
import org.junit.jupiter.api.Test;

class DesktopResultContentTest {
    @Test
    void playerFirstRecordUsesAndroidResultsWording() {
        String text = DesktopResultContent.resultsMessage(4, 1, 2_000,
                false, true, null, new SaveManager.BestRecord(1, 2_000));

        assertTrue(text.contains("Puzzle solved."));
        assertTrue(text.contains("4x4 Puzzle"));
        assertTrue(text.contains("1 move   Time: 2s"));
        assertTrue(text.contains("First player record for this size."));
    }

    @Test
    void playerNewBestShowsPreviousBest() {
        String text = DesktopResultContent.resultsMessage(3, 12, 34_000,
                false, true, new SaveManager.BestRecord(14, 40_000),
                new SaveManager.BestRecord(12, 34_000));

        assertTrue(text.contains("New best. Previous best: 14 moves, 40s"));
    }

    @Test
    void assistedResultIsNotSavedAsPlayerBest() {
        String text = DesktopResultContent.resultsMessage(5, 56, 78_000,
                true, false, new SaveManager.BestRecord(40, 60_000), null);

        assertTrue(text.contains("Solved with assist."));
        assertTrue(text.contains("Assist result not saved. Player best: 40 moves, 60s"));
    }

    @Test
    void noNewBestShowsCurrentBest() {
        String text = DesktopResultContent.resultsMessage(4, 20, 90_000,
                false, false, new SaveManager.BestRecord(15, 80_000),
                new SaveManager.BestRecord(15, 80_000));

        assertTrue(text.contains("Best remains: 15 moves, 80s"));
    }
}
