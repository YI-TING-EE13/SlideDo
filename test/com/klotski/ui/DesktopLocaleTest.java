package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DesktopLocaleTest {
    @Test
    void onlyStableSupportedTagsAreAccepted() {
        assertTrue(DesktopLocale.isSupported("en"));
        assertTrue(DesktopLocale.isSupported("zh-tw"));
        assertTrue(DesktopLocale.isSupported("ja-JP"));
        assertFalse(DesktopLocale.isSupported("fr"));
        assertEquals("en", DesktopLocale.fromTag("unknown").getTag());
    }

    @Test
    void localizedCriticalControlsDoNotFallBackToKeys() {
        assertEquals("偏好設定", DesktopLocale.fromTag("zh-TW").text("preferences"));
        assertEquals("設定", DesktopLocale.fromTag("ja-JP").text("preferences"));
        assertEquals("Preferences", DesktopLocale.fromTag("en").text("preferences"));
    }
}
