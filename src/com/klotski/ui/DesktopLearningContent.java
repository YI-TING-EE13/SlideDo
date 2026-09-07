package com.klotski.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Localized learning copy shared by onboarding, help, and quick reminders. */
public final class DesktopLearningContent {
    private DesktopLearningContent() {
    }

    /** One first-run onboarding page. */
    public static final class OnboardingPage {
        private final String title;
        private final String body;

        private OnboardingPage(String title, String body) {
            this.title = title;
            this.body = body;
        }

        /**
         * Returns the page heading.
         *
         * @return page heading
         */
        public String getTitle() {
            return title;
        }

        /**
         * Returns the page body.
         *
         * @return page body
         */
        public String getBody() {
            return body;
        }
    }

    /**
     * Returns four short pages matching the Android first-run learning path.
     *
     * @param locale requested desktop locale
     * @return immutable page list
     */
    public static List<OnboardingPage> onboardingPages(DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        return Collections.unmodifiableList(Arrays.asList(
                new OnboardingPage(selected.text("learningGoalTitle"), selected.text("learningGoalBody")),
                new OnboardingPage(selected.text("learningMoveTitle"), selected.text("learningMoveBody")),
                new OnboardingPage(selected.text("learningWholeLineTitle"), selected.text("learningWholeLineBody")),
                new OnboardingPage(selected.text("learningStartTitle"), selected.text("learningStartBody"))));
    }

    /**
     * Returns localized full How to Play text.
     *
     * @param locale requested locale, or {@code null} for English
     * @return localized help text
     */
    public static String howToPlay(DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        return String.join("\n", selected.text("howToPlayTitle"), selected.text("howToPlayGoal"),
                selected.text("howToPlayGoalBody"), "", selected.text("howToPlayMoves"),
                selected.text("howToPlayMovesBody"), selected.text("howToPlayWholeLineBody"), "",
                selected.text("howToPlayAssist"), selected.text("howToPlayAssistBody"));
    }

    /**
     * Returns localized interactive-practice instructions.
     *
     * @param locale requested locale, or {@code null} for English
     * @return localized practice text
     */
    public static String practiceTutorial(DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        return String.join("\n", selected.text("practiceTitle"), selected.text("practiceStepOne"),
                selected.text("practiceStepTwo"), selected.text("practiceStepThree"), "",
                selected.text("practiceFootnote"));
    }

    /**
     * Returns localized pause-menu reminder text.
     *
     * @param locale requested locale, or {@code null} for English
     * @return localized reminder text
     */
    public static String quickReminder(DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        return String.join("\n", selected.text("reminderTitle"), selected.text("reminderEmpty"),
                selected.text("reminderAligned"), selected.text("reminderWholeLine"),
                selected.text("reminderSolver"));
    }
}
