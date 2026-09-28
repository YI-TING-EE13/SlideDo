package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.klotski.core.Direction;
import com.klotski.core.GameModel;
import com.klotski.core.RelayChallengeSpec;
import com.klotski.core.RelayCodeCodec;
import com.klotski.core.SaveManager;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainFrameRelayTest {
    @TempDir
    File tempDir;

    @Test
    void completedRelayIsNotOfferedAsResumable() {
        String old = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            String code = "SLD-R1-AwIABQECAwQFBgcACA-A1947A39";
            RelayChallengeSpec spec = RelayCodeCodec.decode(code);
            GameModel active = spec.createGame();
            assertTrue(active.move(Direction.UP));
            assertTrue(SaveManager.saveRelayGame(active, code, true));
            SaveManager.RelayGame activeSave = SaveManager.loadRelayGame();
            assertNotNull(activeSave);
            assertTrue(MainFrame.canResumeRelay(activeSave));

            GameModel completed = spec.createGame();
            assertTrue(completed.move(Direction.RIGHT));
            assertTrue(completed.isSolved());
            assertTrue(SaveManager.saveRelayGame(completed, code, true));
            assertFalse(MainFrame.canResumeRelay(SaveManager.loadRelayGame()));
        } finally {
            restoreDataDirectory(old);
        }
    }

    @Test
    void relayHomeButtonsUseLocalizedSharedCancelLabel() {
        assertEquals("Cancel", cancelLabel("en"));
        assertEquals("取消", cancelLabel("zh-TW"));
        assertEquals("キャンセル", cancelLabel("ja-JP"));
    }

    @Test
    void relayConfirmationButtonsUseLocalizedActionAndSharedCancelLabels() {
        assertRelayConfirmationLabels("en", "Start Relay", "Cancel");
        assertRelayConfirmationLabels("zh-TW", "開始接力", "取消");
        assertRelayConfirmationLabels("ja-JP", "リレーを開始", "キャンセル");
    }

    private static void assertRelayConfirmationLabels(String tag, String confirm, String cancel) {
        Object[] options = MainFrame.relayConfirmOptions(DesktopLocale.fromTag(tag), "relayStart");
        assertEquals(confirm, options[0]);
        assertEquals(cancel, options[1]);
    }

    private static String cancelLabel(String tag) {
        Object[] options = MainFrame.relayHomeOptions(DesktopLocale.fromTag(tag));
        return (String) options[2];
    }

    private static void restoreDataDirectory(String old) {
        if (old == null) {
            System.clearProperty(SaveManager.DATA_DIR_PROPERTY);
        } else {
            System.setProperty(SaveManager.DATA_DIR_PROPERTY, old);
        }
    }
}
