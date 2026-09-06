package com.klotski.ui;

/**
 * Centralizes short desktop help text so menu dialogs and tests stay aligned.
 */
final class DesktopHelpContent {
    private DesktopHelpContent() {
    }

    static String howToPlay() {
        return howToPlay(DesktopLocale.fromTag("en"));
    }

    static String practiceTutorial() {
        return practiceTutorial(DesktopLocale.fromTag("en"));
    }

    static String howToPlay(DesktopLocale locale) {
        return DesktopLearningContent.howToPlay(locale);
    }

    static String practiceTutorial(DesktopLocale locale) {
        return DesktopLearningContent.practiceTutorial(locale);
    }

    static String quickReminder(DesktopLocale locale) {
        return DesktopLearningContent.quickReminder(locale);
    }
}
