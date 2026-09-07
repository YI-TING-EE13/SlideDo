package com.klotski.ui;

import com.klotski.core.MoveAction;

import java.util.List;

/**
 * Builds the deterministic Move History presentation without touching the
 * model.  The controller is responsible for pausing the active timer while
 * the resulting modal dialog is visible.
 */
final class DesktopHistoryContent {
    private static final int MAX_VISIBLE_ACTIONS = 50;

    private DesktopHistoryContent() {
    }

    static String title(DesktopLocale locale) {
        return locale.text("history");
    }

    static String emptyMessage(DesktopLocale locale) {
        return locale.text("historyNoMoves");
    }

    static String format(List<MoveAction> completed, List<MoveAction> redo,
            DesktopLocale locale) {
        List<MoveAction> history = completed == null ? List.of() : completed;
        List<MoveAction> redoHistory = redo == null ? List.of() : redo;
        if (history.isEmpty() && redoHistory.isEmpty()) {
            return emptyMessage(locale);
        }

        StringBuilder text = new StringBuilder();
        text.append(locale.format("historyCounts", history.size(), redoHistory.size()));
        if (history.isEmpty()) {
            text.append("\n\n").append(locale.text("historyAllUndone"));
        }
        int first = Math.max(0, history.size() - MAX_VISIBLE_ACTIONS);
        if (first > 0) {
            text.append("\n\n").append(locale.text("historyLatest50"));
        }
        for (int index = first; index < history.size(); index++) {
            MoveAction action = history.get(index);
            String direction = locale.text("direction." + action.getDirection().name().toLowerCase());
            text.append("\n").append(locale.format("historyAction", index + 1, direction));
            if (action.getSteps() > 1) {
                text.append(" ").append(locale.format("historyWholeLine", action.getSteps()));
            }
        }
        return text.toString();
    }
}
