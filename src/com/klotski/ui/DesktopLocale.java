package com.klotski.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Desktop locale catalog for the controls and learning copy needed to finish
 * a normal play session.  It intentionally uses plain Java maps instead of
 * Android resources so the packaged Swing app remains self-contained.
 */
public final class DesktopLocale {
    private static final String[] SUPPORTED_TAGS = {"en", "zh-TW", "ja-JP"};
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
        return Collections.unmodifiableMap(values);
    }
}
