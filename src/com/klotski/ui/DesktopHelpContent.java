package com.klotski.ui;

/**
 * Centralizes short desktop help text so menu dialogs and tests stay aligned.
 */
final class DesktopHelpContent {
    private DesktopHelpContent() {
    }

    static String howToPlay() {
        return String.join("\n",
                "Goal",
                "Arrange the numbers in row-major order with the empty cell at the end.",
                "",
                "Moves",
                "Click or swipe a tile in the same row or column as the empty cell.",
                "A whole-line slide moves every tile between that tile and the empty cell.",
                "The whole-line slide counts as one move, matching the Android app.",
                "",
                "Assist",
                "Use Assist > Show Movable Tiles to highlight tiles that can slide now.",
                "Solver-assisted completions do not replace player best records.");
    }

    static String practiceTutorial() {
        return String.join("\n",
                "Practice Tutorial",
                "1. Find a tile in the same row or column as the empty cell.",
                "2. Click the highlighted tile to make your first move.",
                "3. Try a farther highlighted tile to see a whole-line slide.",
                "",
                "This desktop practice flow mirrors the Android guided first puzzle.",
                "The board rules still come from the shared GameModel.");
    }
}
