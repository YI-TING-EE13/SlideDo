package com.klotski.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

/**
 * Desktop locale catalog for the controls and learning copy needed to finish
 * a normal play session.  It intentionally uses plain Java maps instead of
 * Android resources so the packaged Swing app remains self-contained.
 */
public final class DesktopLocale {
    private static final String[] SUPPORTED_TAGS = {"en", "zh-TW", "ja-JP"};
    private static final String[] REQUIRED_KEYS = {
            "history", "historyNoMoves", "historyCounts", "historyAllUndone",
            "historyLatest50", "historyAction", "historyWholeLine",
            "direction.up", "direction.down", "direction.left", "direction.right",
            "strategicHint", "strategicHintUnavailable", "strategicHintStatus",
            "solverTools", "solverBfs", "solverAStar", "solverIdaStar", "solverCancel",
            "solverWarningBfs", "solverWarningAStar", "solverWarningIdaStar", "warning",
            "solverFound", "solverNoSolution", "solverCancelled", "solverError",
            "assistedRun", "assistedResult", "playerBestProtected",
            "favoriteSolved", "favoriteIsolation", "favoritePracticeSummary",
            "dailyCompleted", "dailyInProgress", "dailyReady", "dailyMissed", "dailyFuture",
            "dailyPuzzle", "dailyProgress",
            "continuousResume", "continuousStatus", "continuousResult", "continuousComplete", "continuousRetained",
            "trendTitle", "trendScope", "trendGoal", "trendReached", "trendWeek", "trendNoData", "trendExclusion", "trendRecent", "trendNeedData", "trendPrevious", "trendMoves", "trendTime", "trendImproving", "trendDeclining", "trendSteady", "trendNotEnough",
            "recordsPlayer", "recordsAssisted", "recordsFootnote", "preferencesDescription",
            "recordPlayerBest", "recordFirst", "recordNewBest", "recordBestRemains", "moveSingular", "movePlural",
            "difficultyRelaxed", "difficultyClassic", "difficultyChallenge",
            "records", "daily", "favorites", "trends", "continuous",
            "preferences", "resetSaved", "resetRecords", "quickReminder",
            "cellEmptyName", "cellTileName", "cellEmptyDescription",
            "cellMovableDescription", "cellNotAlignedDescription",
            "boardAccessibleName", "boardAccessibleDescription", "winTitle", "winMessage"
    };
    private static final Map<String, Map<String, String>> STRINGS = createStrings();

    private final String tag;

    private DesktopLocale(String tag) {
        this.tag = tag;
    }

    /**
     * Returns the stable language tag persisted for this locale.
     *
     * @return stable language tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Returns a defensive copy of supported desktop language tags.
     *
     * @return supported language tags
     */
    public static String[] supportedTags() {
        return SUPPORTED_TAGS.clone();
    }

    /**
     * Resolves a stable language tag, defaulting unknown values to English.
     *
     * @param tag BCP-47-like desktop tag
     * @return locale object
     */
    public static DesktopLocale fromTag(String tag) {
        if (tag != null) {
            for (String supported : SUPPORTED_TAGS) {
                if (supported.equalsIgnoreCase(tag.trim())) {
                    return new DesktopLocale(supported);
                }
            }
        }
        return new DesktopLocale("en");
    }

    /**
     * Looks up a critical control or message.
     *
     * @param key stable English key
     * @return translated text, or the key when a non-critical future key is missing
     */
    public String text(String key) {
        Map<String, String> values = STRINGS.get(tag);
        String value = values == null ? null : values.get(key);
        if (value != null) {
            return value;
        }
        Map<String, String> english = STRINGS.get("en");
        value = english == null ? null : english.get(key);
        return value == null ? key : value;
    }

    /**
     * Formats a localized message using the catalog's stable placeholders.
     * Missing locale entries fall back to English before formatting.
     *
     * @param key message key
     * @param arguments format arguments
     * @return formatted player-facing message
     */
    public String format(String key, Object... arguments) {
        return String.format(Locale.ROOT, text(key), arguments);
    }

    /**
     * Returns the material keys required by the completed desktop flows.
     *
     * @return defensive copy of required keys
     */
    public static String[] requiredKeys() {
        return REQUIRED_KEYS.clone();
    }

    /**
     * Checks the selected locale itself rather than accepting an English
     * fallback. This makes localization coverage tests identify the exact key.
     *
     * @param key material key
     * @return whether this locale has an explicit value for the key
     */
    public boolean hasTranslation(String key) {
        Map<String, String> values = STRINGS.get(tag);
        return values != null && values.containsKey(key);
    }

    /**
     * Checks whether a tag is one of the supported desktop locales.
     *
     * @param tag candidate language tag
     * @return whether the tag is supported
     */
    public static boolean isSupported(String tag) {
        if (tag == null) {
            return false;
        }
        for (String supported : SUPPORTED_TAGS) {
            if (supported.equalsIgnoreCase(tag.trim())) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Map<String, String>> createStrings() {
        Map<String, Map<String, String>> all = new LinkedHashMap<>();
        all.put("en", english());
        all.put("zh-TW", traditionalChinese());
        all.put("ja-JP", japanese());
        return Collections.unmodifiableMap(all);
    }

    private static Map<String, String> english() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("game", "Game");
        values.put("assist", "Assist");
        values.put("solver", "Solver");
        values.put("help", "Help");
        values.put("new3", "New 3x3");
        values.put("new4", "New 4x4");
        values.put("new5", "New 5x5");
        values.put("restart", "Restart This Puzzle");
        values.put("undo", "Undo");
        values.put("redo", "Redo");
        values.put("history", "Move History");
        values.put("save", "Save Game");
        values.put("load", "Load Game");
        values.put("records", "Records");
        values.put("daily", "Daily Calendar");
        values.put("favorites", "Favorites");
        values.put("trends", "Trends / Weekly Goal");
        values.put("continuous", "Continuous Challenge");
        values.put("preferences", "Preferences");
        values.put("quickReminder", "Quick Reminder");
        values.put("exit", "Exit");
        values.put("showMovable", "Show Movable Tiles");
        values.put("howToPlay", "How to Play");
        values.put("beginnerGuide", "Beginner Guide");
        values.put("practiceTutorial", "Practice Tutorial");
        values.put("continueLoad", "Continue / Load");
        values.put("resetSaved", "Reset Saved Games");
        values.put("resetRecords", "Reset Records & Statistics");
        values.put("reduceMotion", "Reduce motion");
        values.put("sound", "Sound feedback");
        values.put("theme", "Theme");
        values.put("language", "Language");
        values.put("close", "Close");
        values.put("back", "Back");
        values.put("next", "Next");
        values.put("previous", "Previous");
        values.put("skip", "Skip");
        values.put("start", "Start 3x3");
        values.put("resetLesson", "Reset Lesson");
        values.put("startTutorialPuzzle", "Start 3x3 Puzzle");
        values.put("resetSavedConfirm", "Clear normal, Daily, Favorite Practice, and Continuous saves?\nFavorite labels, records, statistics, Daily history, and preferences stay.");
        values.put("resetRecordsConfirm", "Clear best records, completion history/statistics, and Daily streak state?\nActive Continuous Challenge and preferences stay.");
        values.put("homeSummary", "Home | New Game, Continue, Daily, Favorites, Trends, Continuous, Records");
        values.put("noSaves", "No saved games yet.");
        values.put("firstRunSubtitle", "Learn the rules, try one guided move, then start playing.");
        values.put("historyNoMoves", "No moves yet. Your current run history will appear here.");
        values.put("historyCounts", "%d completed actions · %d available to redo");
        values.put("historyAllUndone", "All completed actions are currently undone.");
        values.put("historyLatest50", "Showing the latest 50 actions.");
        values.put("historyAction", "%d. Empty %s");
        values.put("historyWholeLine", "× %d (one move)");
        values.put("direction.up", "up"); values.put("direction.down", "down");
        values.put("direction.left", "left"); values.put("direction.right", "right");
        values.put("strategicHint", "Strategic Hint");
        values.put("strategicHintUnavailable", "No strategic hint is available for this position.");
        values.put("strategicHintStatus", "Hint: try tile %d (empty cell moves %s)");
        values.put("solverTools", "Solver Tools");
        values.put("warning", "Warning");
        values.put("solverBfs", "Solve with BFS (best for 3x3)");
        values.put("solverAStar", "Solve with A* (best for 4x4+)");
        values.put("solverIdaStar", "Solve with IDA* (mobile-friendly core)");
        values.put("solverCancel", "Cancel Solver");
        values.put("solverWarningBfs", "BFS on 4x4 or larger may use substantial memory or time. Continue?");
        values.put("solverWarningAStar", "A* on 5x5 can be very slow or memory-heavy. Continue?");
        values.put("solverWarningIdaStar", "IDA* on 5x5 can take a long time. Continue?");
        values.put("solverFound", "Solution found: %d moves.\nAnimate it now?");
        values.put("solverNoSolution", "No solution found or the search timed out.");
        values.put("solverCancelled", "Solver cancelled. The current puzzle was not changed.");
        values.put("solverError", "Solver could not complete. You can keep playing or try a smaller board.");
        values.put("assistedRun", "Assisted run");
        values.put("assistedResult", "Solved with assist.");
        values.put("playerBestProtected", "Assist result not saved as a player best.");
        values.put("favoriteSolved", "Favorite Practice solved.");
        values.put("favoriteIsolation", "Practice result not saved to records, history, statistics, or Daily streaks.");
        values.put("favoritePracticeSummary", "Favorite Practice is isolated: saves, records, completion history, lifetime statistics, and Daily streaks are not changed.");
        values.put("dailyCompleted", "Completed"); values.put("dailyInProgress", "In progress"); values.put("dailyReady", "Ready"); values.put("dailyMissed", "Missed"); values.put("dailyFuture", "Future date"); values.put("dailyPuzzle", "Daily 4x4 Classic puzzle."); values.put("dailyProgress", "Daily streak: %d · Best: %d");
        values.put("continuousResume", "Resume %d/%d · %dx%d · %s"); values.put("continuousStatus", "Continuous: %d/%d puzzles · %d moves · %ds · Assisted: %d"); values.put("continuousResult", "Continuous Challenge\n\n%s\nScope: %dx%d · %s\n\n%s"); values.put("continuousComplete", "Challenge complete."); values.put("continuousRetained", "The solved puzzle is retained for resume or Next Puzzle.");
        values.put("trendTitle", "Trends and Weekly Goal"); values.put("trendScope", "Scope: %dx%d · %s"); values.put("trendGoal", "Weekly goal: %d/%d"); values.put("trendReached", "reached"); values.put("trendWeek", "Week: %s to %s"); values.put("trendNoData", "No player completions in this scope yet."); values.put("trendExclusion", "Assisted and Favorite Practice results are excluded."); values.put("trendRecent", "Recent player solves: %d · average %d moves, %ds"); values.put("trendNeedData", "Comparison: need at least six player solves."); values.put("trendPrevious", "Previous window: %d moves, %ds"); values.put("trendMoves", "Moves: %s (%d%%)"); values.put("trendTime", "Time: %s (%d%%)"); values.put("trendImproving", "improving"); values.put("trendDeclining", "declining"); values.put("trendSteady", "steady"); values.put("trendNotEnough", "not enough data");
        values.put("recordsPlayer", "Player solves: %d"); values.put("recordsAssisted", "Assisted: %d"); values.put("recordsFootnote", "Player records use size and difficulty scopes; fewer moves rank first; ties use faster time.\nAssisted completions remain in history/statistics but never replace player best records."); values.put("preferencesDescription", "Desktop preferences affect only presentation. Puzzle rules and records remain unchanged.");
        values.put("recordPlayerBest", "Assist result not saved. Player best: %s"); values.put("recordFirst", "First player record for this size and difficulty."); values.put("recordNewBest", "New best. Previous best: %s"); values.put("recordBestRemains", "Best remains: %s"); values.put("moveSingular", "%d move"); values.put("movePlural", "%d moves");
        values.put("difficultyRelaxed", "Relaxed"); values.put("difficultyClassic", "Classic");
        values.put("difficultyChallenge", "Challenge");
        values.put("cellEmptyName", "Empty cell, row %d, column %d");
        values.put("cellTileName", "Tile %d, row %d, column %d");
        values.put("cellEmptyDescription", "Empty cell. Select another cell or use the arrow keys to move the empty cell.");
        values.put("cellMovableDescription", "Movable tile. Press Space or Enter to slide this aligned tile.");
        values.put("cellNotAlignedDescription", "Tile is not aligned with the empty cell right now.");
        values.put("boardAccessibleName", "Puzzle board");
        values.put("boardAccessibleDescription", "Keyboard-accessible sliding puzzle board. Tab between cells and use arrow keys to move the empty cell.");
        values.put("winTitle", "Winner!");
        values.put("winMessage", "Congratulations! You won in %d moves.\nTime: %ds");
        return Collections.unmodifiableMap(values);
    }

    private static Map<String, String> traditionalChinese() {
        Map<String, String> values = new LinkedHashMap<>();
        values.putAll(english());
        values.put("game", "遊戲"); values.put("assist", "輔助"); values.put("solver", "求解器"); values.put("help", "說明");
        values.put("new3", "新建 3x3"); values.put("new4", "新建 4x4"); values.put("new5", "新建 5x5");
        values.put("restart", "重新開始本題"); values.put("undo", "復原"); values.put("redo", "重做"); values.put("history", "移動記錄");
        values.put("save", "儲存遊戲"); values.put("load", "載入遊戲"); values.put("records", "紀錄"); values.put("daily", "每日挑戰日曆");
        values.put("favorites", "收藏題目"); values.put("trends", "趨勢／每週目標"); values.put("continuous", "連續挑戰"); values.put("preferences", "偏好設定");
        values.put("quickReminder", "快速提醒"); values.put("exit", "離開"); values.put("showMovable", "顯示可移動方塊");
        values.put("howToPlay", "玩法說明"); values.put("beginnerGuide", "新手指南"); values.put("practiceTutorial", "練習教學"); values.put("continueLoad", "繼續／載入");
        values.put("resetSaved", "重置已儲存遊戲"); values.put("resetRecords", "重置紀錄與統計"); values.put("reduceMotion", "減少動態效果"); values.put("sound", "音效提示");
        values.put("theme", "主題"); values.put("language", "語言"); values.put("close", "關閉"); values.put("back", "返回"); values.put("next", "下一步"); values.put("previous", "上一步"); values.put("skip", "略過"); values.put("start", "開始 3x3");
        values.put("resetLesson", "重置課程"); values.put("startTutorialPuzzle", "開始 3x3 題目"); values.put("homeSummary", "首頁｜新遊戲、繼續、每日、收藏、趨勢、連續、紀錄"); values.put("noSaves", "目前沒有已儲存的遊戲。"); values.put("firstRunSubtitle", "先學會規則、完成引導移動，再開始遊玩。");
        values.put("historyNoMoves", "目前還沒有移動，這一局的記錄會顯示在這裡。");
        values.put("historyCounts", "已完成 %d 個動作 · 可重做 %d 個動作");
        values.put("historyAllUndone", "所有已完成動作目前都已復原。");
        values.put("historyLatest50", "顯示最近 50 個動作。");
        values.put("historyAction", "%d. 空格向%s"); values.put("historyWholeLine", "× %d（一次移動）");
        values.put("direction.up", "上"); values.put("direction.down", "下"); values.put("direction.left", "左"); values.put("direction.right", "右");
        values.put("strategicHint", "策略提示"); values.put("strategicHintUnavailable", "目前局面沒有可用的策略提示。");
        values.put("strategicHintStatus", "提示：嘗試方塊 %d（空格向%s移動）"); values.put("solverTools", "求解器工具");
        values.put("solverTools", "求解器工具"); values.put("solverBfs", "使用 BFS 求解（適合 3x3）"); values.put("solverAStar", "使用 A* 求解（適合 4x4 以上）"); values.put("solverIdaStar", "使用 IDA* 求解（適合行動核心）"); values.put("solverCancel", "取消求解器");
        values.put("warning", "警告");
        values.put("solverWarningBfs", "4x4 以上使用 BFS 可能需要大量記憶體或時間。要繼續嗎？"); values.put("solverWarningAStar", "5x5 使用 A* 可能非常慢或耗用大量記憶體。要繼續嗎？"); values.put("solverWarningIdaStar", "5x5 使用 IDA* 可能需要很久。要繼續嗎？");
        values.put("solverFound", "找到解答：%d 步。\n要現在播放嗎？"); values.put("solverNoSolution", "找不到解答或搜尋逾時。"); values.put("solverCancelled", "已取消求解器，目前題目沒有變更。"); values.put("solverError", "求解器無法完成。你可以繼續遊玩或改用較小棋盤。");
        values.put("assistedRun", "輔助遊戲"); values.put("assistedResult", "使用輔助完成。"); values.put("playerBestProtected", "輔助結果不會寫入玩家最佳紀錄。");
        values.put("favoriteSolved", "收藏練習完成。"); values.put("favoriteIsolation", "練習結果不會寫入紀錄、歷史、統計或每日連勝。");
        values.put("favoritePracticeSummary", "收藏練習彼此隔離：不會變更儲存、紀錄、完成歷史、終身統計或每日連勝。");
        values.put("dailyCompleted", "已完成"); values.put("dailyInProgress", "進行中"); values.put("dailyReady", "可開始"); values.put("dailyMissed", "錯過"); values.put("dailyFuture", "未來日期"); values.put("dailyPuzzle", "每日 4x4 經典題目。"); values.put("dailyProgress", "每日連勝：%d · 最佳：%d");
        values.put("continuousResume", "繼續 %d/%d · %dx%d · %s"); values.put("continuousStatus", "連續挑戰：%d/%d 題 · %d 步 · %d 秒 · 輔助：%d"); values.put("continuousResult", "連續挑戰\n\n%s\n範圍：%dx%d · %s\n\n%s"); values.put("continuousComplete", "挑戰完成。"); values.put("continuousRetained", "已完成題目會保留，方便繼續或開始下一題。");
        values.put("trendTitle", "趨勢與每週目標"); values.put("trendScope", "範圍：%dx%d · %s"); values.put("trendGoal", "每週目標：%d/%d"); values.put("trendReached", "已達成"); values.put("trendWeek", "週期：%s 至 %s"); values.put("trendNoData", "此範圍尚無玩家完成紀錄。"); values.put("trendExclusion", "輔助與收藏練習結果不列入。"); values.put("trendRecent", "最近玩家完成：%d · 平均 %d 步、%d 秒"); values.put("trendNeedData", "比較：至少需要六次玩家完成。"); values.put("trendPrevious", "前一區間：%d 步、%d 秒"); values.put("trendMoves", "步數：%s（%d%%）"); values.put("trendTime", "時間：%s（%d%%）"); values.put("trendImproving", "改善"); values.put("trendDeclining", "變差"); values.put("trendSteady", "持平"); values.put("trendNotEnough", "資料不足");
        values.put("recordsPlayer", "玩家完成：%d"); values.put("recordsAssisted", "輔助：%d"); values.put("recordsFootnote", "玩家紀錄依棋盤大小與難度分開；步數越少越好，同步數以時間較快者優先。\n輔助完成仍保留在歷史／統計，但不會取代玩家最佳紀錄。"); values.put("preferencesDescription", "桌面偏好只影響呈現。拼圖規則與紀錄保持不變。");
        values.put("recordPlayerBest", "輔助結果不儲存。玩家最佳：%s"); values.put("recordFirst", "此大小與難度的第一筆玩家紀錄。"); values.put("recordNewBest", "新最佳。之前最佳：%s"); values.put("recordBestRemains", "最佳維持：%s"); values.put("moveSingular", "%d 步"); values.put("movePlural", "%d 步");
        values.put("difficultyRelaxed", "輕鬆"); values.put("difficultyClassic", "經典"); values.put("difficultyChallenge", "挑戰");
        values.put("cellEmptyName", "空格，第 %d 列，第 %d 欄"); values.put("cellTileName", "方塊 %d，第 %d 列，第 %d 欄"); values.put("cellEmptyDescription", "空格。選擇其他方塊，或使用方向鍵移動空格。"); values.put("cellMovableDescription", "可移動方塊。按空白鍵或 Enter 將對齊方塊滑入。"); values.put("cellNotAlignedDescription", "此方塊目前未與空格對齊。"); values.put("boardAccessibleName", "拼圖棋盤"); values.put("boardAccessibleDescription", "可用鍵盤操作的滑塊拼圖棋盤。使用 Tab 在方塊間移動，方向鍵移動空格。");
        values.put("winTitle", "完成！"); values.put("winMessage", "恭喜！你用了 %d 步完成。\n時間：%d 秒");
        return Collections.unmodifiableMap(values);
    }

    private static Map<String, String> japanese() {
        Map<String, String> values = new LinkedHashMap<>();
        values.putAll(english());
        values.put("game", "ゲーム"); values.put("assist", "アシスト"); values.put("solver", "ソルバー"); values.put("help", "ヘルプ");
        values.put("new3", "3x3を開始"); values.put("new4", "4x4を開始"); values.put("new5", "5x5を開始"); values.put("restart", "このパズルを再開");
        values.put("undo", "元に戻す"); values.put("redo", "やり直す"); values.put("history", "手順履歴"); values.put("save", "ゲームを保存"); values.put("load", "ゲームを読み込む");
        values.put("records", "記録"); values.put("daily", "デイリーカレンダー"); values.put("favorites", "お気に入り"); values.put("trends", "傾向／週間目標"); values.put("continuous", "連続チャレンジ"); values.put("preferences", "設定"); values.put("quickReminder", "クイックリマインダー"); values.put("exit", "終了");
        values.put("showMovable", "動かせるタイルを表示"); values.put("howToPlay", "遊び方"); values.put("beginnerGuide", "初心者ガイド"); values.put("practiceTutorial", "練習チュートリアル"); values.put("continueLoad", "続き／読み込み");
        values.put("resetSaved", "保存ゲームをリセット"); values.put("resetRecords", "記録と統計をリセット"); values.put("reduceMotion", "動きを減らす"); values.put("sound", "サウンド通知"); values.put("theme", "テーマ"); values.put("language", "言語");
        values.put("close", "閉じる"); values.put("back", "戻る"); values.put("next", "次へ"); values.put("previous", "前へ"); values.put("skip", "スキップ"); values.put("start", "3x3を開始"); values.put("resetLesson", "レッスンをリセット"); values.put("startTutorialPuzzle", "3x3パズルを開始"); values.put("homeSummary", "ホーム｜新規、続き、デイリー、お気に入り、傾向、連続、記録"); values.put("noSaves", "保存されたゲームはありません。"); values.put("firstRunSubtitle", "ルールを学び、ガイド付きの一手を試してから始めましょう。");
        values.put("solverTools", "ソルバーツール"); values.put("historyNoMoves", "まだ手順はありません。現在の履歴がここに表示されます。"); values.put("historyCounts", "完了した操作 %d · やり直し可能 %d"); values.put("historyAllUndone", "完了した操作はすべて元に戻されています。"); values.put("historyLatest50", "最新 50 件を表示しています。"); values.put("historyAction", "%d. 空白を%sへ"); values.put("historyWholeLine", "× %d（1手）");
        values.put("warning", "警告");
        values.put("direction.up", "上"); values.put("direction.down", "下"); values.put("direction.left", "左"); values.put("direction.right", "右"); values.put("strategicHint", "戦略ヒント"); values.put("strategicHintUnavailable", "この局面には戦略ヒントがありません。"); values.put("strategicHintStatus", "ヒント：タイル %d を試す（空白が%sへ移動）"); values.put("solverTools", "ソルバーツール"); values.put("solverBfs", "BFSで解く（3x3向け）"); values.put("solverAStar", "A*で解く（4x4以上向け）"); values.put("solverIdaStar", "IDA*で解く（モバイル向けコア）"); values.put("solverCancel", "ソルバーをキャンセル");
        values.put("solverWarningBfs", "4x4以上のBFSは大量のメモリや時間を使う場合があります。続行しますか？"); values.put("solverWarningAStar", "5x5のA*は非常に遅いか、大量のメモリを使う場合があります。続行しますか？"); values.put("solverWarningIdaStar", "5x5のIDA*は長時間かかる場合があります。続行しますか？"); values.put("solverFound", "解答が見つかりました：%d手。\n今すぐ再生しますか？"); values.put("solverNoSolution", "解答が見つからないか、検索がタイムアウトしました。"); values.put("solverCancelled", "ソルバーをキャンセルしました。現在のパズルは変更されていません。"); values.put("solverError", "ソルバーを完了できませんでした。続けるか、小さい盤面を試してください。");
        values.put("assistedRun", "アシストプレイ"); values.put("assistedResult", "アシストでクリア。"); values.put("playerBestProtected", "アシスト結果はプレイヤー記録に保存されません。"); values.put("difficultyRelaxed", "イージー"); values.put("difficultyClassic", "クラシック"); values.put("difficultyChallenge", "チャレンジ");
        values.put("favoriteSolved", "お気に入り練習をクリア。"); values.put("favoriteIsolation", "練習結果は記録、履歴、統計、デイリー連勝に保存されません。");
        values.put("favoritePracticeSummary", "お気に入り練習は独立しています。保存、記録、完了履歴、統計、デイリー連勝は変更されません。");
        values.put("dailyCompleted", "完了"); values.put("dailyInProgress", "進行中"); values.put("dailyReady", "開始可能"); values.put("dailyMissed", "未達成"); values.put("dailyFuture", "未来の日付"); values.put("dailyPuzzle", "デイリー4x4クラシックパズル。"); values.put("dailyProgress", "デイリー連勝：%d · ベスト：%d");
        values.put("continuousResume", "再開 %d/%d · %dx%d · %s"); values.put("continuousStatus", "連続：%d/%d パズル · %d手 · %d秒 · アシスト：%d"); values.put("continuousResult", "連続チャレンジ\n\n%s\n範囲：%dx%d · %s\n\n%s"); values.put("continuousComplete", "チャレンジ完了。"); values.put("continuousRetained", "クリアしたパズルは再開または次のパズル用に保持されます。");
        values.put("trendTitle", "傾向と週間目標"); values.put("trendScope", "範囲：%dx%d · %s"); values.put("trendGoal", "週間目標：%d/%d"); values.put("trendReached", "達成"); values.put("trendWeek", "週：%s から %s"); values.put("trendNoData", "この範囲にはプレイヤーの完了記録がありません。"); values.put("trendExclusion", "アシストとお気に入り練習は除外されます。"); values.put("trendRecent", "最近のクリア：%d · 平均 %d手、%d秒"); values.put("trendNeedData", "比較には少なくとも6回のプレイヤークリアが必要です。"); values.put("trendPrevious", "前の期間：%d手、%d秒"); values.put("trendMoves", "手数：%s（%d%%）"); values.put("trendTime", "時間：%s（%d%%）"); values.put("trendImproving", "改善"); values.put("trendDeclining", "悪化"); values.put("trendSteady", "安定"); values.put("trendNotEnough", "データ不足");
        values.put("recordsPlayer", "プレイヤークリア：%d"); values.put("recordsAssisted", "アシスト：%d"); values.put("recordsFootnote", "プレイヤー記録はサイズと難易度ごとに管理し、手数が少ない順、同数なら時間が短い順です。\nアシスト完了は履歴／統計に残りますが、プレイヤー記録を置き換えません。"); values.put("preferencesDescription", "デスクトップ設定は表示だけに影響します。パズルのルールと記録は変わりません。");
        values.put("recordPlayerBest", "アシスト結果は保存されません。プレイヤー記録：%s"); values.put("recordFirst", "このサイズと難易度の最初のプレイヤー記録です。"); values.put("recordNewBest", "新記録。以前の記録：%s"); values.put("recordBestRemains", "記録：%s"); values.put("moveSingular", "%d手"); values.put("movePlural", "%d手");
        values.put("cellEmptyName", "空白、%d行 %d列"); values.put("cellTileName", "タイル %d、%d行 %d列"); values.put("cellEmptyDescription", "空白です。別のタイルを選ぶか、矢印キーで空白を動かします。"); values.put("cellMovableDescription", "移動可能なタイルです。SpaceまたはEnterで揃ったタイルを動かします。"); values.put("cellNotAlignedDescription", "このタイルは現在空白と揃っていません。"); values.put("boardAccessibleName", "パズル盤面"); values.put("boardAccessibleDescription", "キーボードで操作できるスライドパズル盤面。Tabでセルを移動し、矢印キーで空白を動かします。");
        values.put("winTitle", "クリア！"); values.put("winMessage", "おめでとうございます！%d手でクリアしました。\n時間：%d秒");
        return Collections.unmodifiableMap(values);
    }
}
