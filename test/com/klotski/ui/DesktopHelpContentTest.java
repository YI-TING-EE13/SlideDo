package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DesktopHelpContentTest {
    @Test
    void howToPlayDocumentsDesktopAndroidParityFeatures() {
        String text = DesktopHelpContent.howToPlay();

        assertTrue(text.contains("whole-line slide"));
        assertTrue(text.contains("Show Movable Tiles"));
        assertTrue(text.contains("Solver-assisted"));
    }

    @Test
    void practiceTutorialReferencesGuidedFirstPuzzleAndSharedModel() {
        String text = DesktopHelpContent.practiceTutorial();

        assertTrue(text.contains("Practice Tutorial"));
        assertTrue(text.contains("guided first puzzle"));
        assertTrue(text.contains("GameModel"));
    }
}
