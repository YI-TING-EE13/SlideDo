package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DesktopLocaleCoverageTest {
    @Test
    void everyMaterialDesktopKeyHasAnExplicitTranslation() {
        for (String tag : DesktopLocale.supportedTags()) {
            DesktopLocale locale = DesktopLocale.fromTag(tag);
            for (String key : DesktopLocale.requiredKeys()) {
                assertTrue(locale.hasTranslation(key), tag + " is missing localization key " + key);
                String value = locale.text(key);
                assertFalse(value == null || value.isBlank(), tag + " has blank key " + key);
                assertFalse(value.equals(key), tag + " exposes key " + key);
            }
        }
    }

    @Test
    void representativeFormattedMessagesRetainTheirPlaceholders() {
        for (String tag : DesktopLocale.supportedTags()) {
            DesktopLocale locale = DesktopLocale.fromTag(tag);
            assertTrue(locale.format("historyCounts", 2, 1).contains("2"), tag);
            assertTrue(locale.format("strategicHintStatus", 8, locale.text("direction.up"))
                    .contains("8"), tag);
            assertTrue(locale.format("cellTileName", 7, 2, 3).contains("7"), tag);
        }
    }

    @Test
    void restoreOutcomeMessagesExplainSuccessWarningAndRetainedPaths() {
        for (String tag : DesktopLocale.supportedTags()) {
            DesktopLocale locale = DesktopLocale.fromTag(tag);
            String warning = locale.format("backupRestoreCleanupWarning", "C:\\recovery");
            String failure = locale.format("backupRestoreRecoveryRequired",
                    "C:\\recovery", "C:\\previous");
            assertTrue(warning.contains("recovery"), tag);
            assertTrue(failure.contains("recovery"), tag);
            assertTrue(failure.contains("previous"), tag);
            assertFalse(failure.contains("Exception"), tag);
        }
    }
}
