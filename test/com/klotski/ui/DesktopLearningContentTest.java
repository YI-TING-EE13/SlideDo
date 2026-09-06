package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DesktopLearningContentTest {
    @Test
    void onboardingHasFourLocalizedPagesForEverySupportedLocale() {
        for (String tag : DesktopLocale.supportedTags()) {
            List<DesktopLearningContent.OnboardingPage> pages =
                    DesktopLearningContent.onboardingPages(DesktopLocale.fromTag(tag));
            assertEquals(4, pages.size());
            assertFalse(pages.get(0).getTitle().isBlank());
            assertFalse(pages.get(3).getBody().isBlank());
        }
    }

    @Test
    void criticalLearningCopyDescribesWholeLineAndRecordBoundaries() {
        String reminder = DesktopLearningContent.quickReminder(DesktopLocale.fromTag("en"));
        assertTrue(reminder.contains("whole-line"));
        assertTrue(reminder.contains("best records"));
        assertTrue(DesktopLearningContent.practiceTutorial(DesktopLocale.fromTag("zh-TW"))
                .contains("GameModel"));
    }
}
