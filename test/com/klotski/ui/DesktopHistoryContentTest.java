package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.klotski.core.Direction;
import com.klotski.core.MoveAction;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DesktopHistoryContentTest {
    @Test
    void historyShowsCountsDirectionAndWholeLineAsOneAction() {
        String content = DesktopHistoryContent.format(
                List.of(new MoveAction(Direction.UP, 1), new MoveAction(Direction.LEFT, 3)),
                List.of(new MoveAction(Direction.DOWN, 1)), DesktopLocale.fromTag("en"));

        assertTrue(content.contains("2 completed actions · 1 available to redo"));
        assertTrue(content.contains("1. Empty up"));
        assertTrue(content.contains("2. Empty left × 3 (one move)"));
        assertFalse(content.contains("Empty down"), "redo actions are counted but not shown as completed");
    }

    @Test
    void historyUsesLatestFiftyInOldestFirstOrderWithoutMutatingInput() {
        List<MoveAction> actions = new ArrayList<>();
        for (int index = 0; index < 55; index++) {
            actions.add(new MoveAction(Direction.RIGHT, 1));
        }
        List<MoveAction> snapshot = new ArrayList<>(actions);
        String content = DesktopHistoryContent.format(actions, List.of(), DesktopLocale.fromTag("zh-TW"));

        assertTrue(content.contains("顯示最近 50 個動作。"));
        assertFalse(content.contains("\n5. "), "first five actions are outside the bounded view");
        assertTrue(content.contains("\n6. "));
        assertTrue(content.contains("\n55. "));
        assertEquals(snapshot, actions);
    }

    @Test
    void historyLocalizesJapaneseDirectionAndEmptyState() {
        assertEquals("まだ手順はありません。現在の履歴がここに表示されます。",
                DesktopHistoryContent.emptyMessage(DesktopLocale.fromTag("ja-JP")));
        String content = DesktopHistoryContent.format(
                List.of(new MoveAction(Direction.DOWN, 2)), List.of(),
                DesktopLocale.fromTag("ja-JP"));
        assertTrue(content.contains("空白を下へ"));
        assertTrue(content.contains("× 2（1手）"));
    }
}
