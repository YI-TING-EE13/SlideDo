package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.klotski.core.SaveManager;
import com.klotski.core.PuzzleDifficulty;
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
        assertTrue(text.contains("Fewer moves rank first"));
        assertTrue(text.contains("ties use faster time"));
        assertTrue(text.contains("Solver-assisted"));
    }

    @Test
    void preferencesDescriptionKeepsSettingsPresentationOnly() {
        String text = DesktopHomeContent.preferencesDescription();

        assertTrue(text.contains("presentation"));
        assertTrue(text.contains("Puzzle rules"));
        assertTrue(text.contains("records"));
    }

    @Test
    void scopedRecordsSummaryShowsDifficultyAndCompletionCounts() {
        SaveManager.BestRecord[][] records = new SaveManager.BestRecord[3][3];
        SaveManager.CompletionStats[][] stats = new SaveManager.CompletionStats[3][3];
        records[0][PuzzleDifficulty.CHALLENGE.ordinal()] = new SaveManager.BestRecord(9, 12_000);
        stats[0][PuzzleDifficulty.CHALLENGE.ordinal()] =
                completionStatsForTest(2, 1, 20, 30_000);

        String text = DesktopHomeContent.recordsSummary(records, stats);

        assertTrue(text.contains("3x3"));
        assertTrue(text.contains("Challenge: 9 moves, 12s"));
        assertTrue(text.contains("Player solves: 2"));
        assertTrue(text.contains("Assisted: 1"));
        assertTrue(text.contains("size"));
        assertTrue(text.contains("difficulty"));
    }

    private static SaveManager.CompletionStats completionStatsForTest(int player,
            int assisted, long moves, long timeMs) {
        return new SaveManager.CompletionStats(player, assisted, moves, timeMs);
    }
}
