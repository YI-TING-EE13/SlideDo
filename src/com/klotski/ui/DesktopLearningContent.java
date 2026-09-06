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
        String tag = locale == null ? "en" : locale.getTag();
        if ("zh-TW".equals(tag)) {
            return Collections.unmodifiableList(Arrays.asList(
                    new OnboardingPage("目標", "依序排列數字，空格必須在右下角。"),
                    new OnboardingPage("移動", "點擊或滑動與空格同列或同欄的方塊。"),
                    new OnboardingPage("整列滑動", "點擊較遠的對齊方塊，整列會算作一次移動，也只佔一次復原。"),
                    new OnboardingPage("開始遊玩", "提示與求解器適合學習；只有玩家完成會更新最佳紀錄。")));
        }
        if ("ja-JP".equals(tag)) {
            return Collections.unmodifiableList(Arrays.asList(
                    new OnboardingPage("目標", "数字を順番に並べ、空きマスを右下に置きます。"),
                    new OnboardingPage("移動", "空きマスと同じ行または列のタイルをクリック／スワイプします。"),
                    new OnboardingPage("列の移動", "遠いタイルを選ぶと一列が一手として動き、元に戻すのも一回です。"),
                    new OnboardingPage("開始", "ヒントとソルバーは学習用です。プレイヤーのクリアだけが記録を更新します。")));
        }
        return Collections.unmodifiableList(Arrays.asList(
                new OnboardingPage("Goal", "Arrange the numbers in order and leave the empty cell in the bottom-right corner."),
                new OnboardingPage("Move", "Click or swipe a tile in the same row or column as the empty cell."),
                new OnboardingPage("Whole-line slides", "Choosing a farther aligned tile moves the whole line as one move and one undo."),
                new OnboardingPage("Start playing", "Hints and the solver are for learning; only player solves update best records.")));
    }

    /**
     * Returns localized full How to Play text.
     *
     * @param locale requested locale, or {@code null} for English
     * @return localized help text
     */
    public static String howToPlay(DesktopLocale locale) {
        String tag = locale == null ? "en" : locale.getTag();
        if ("zh-TW".equals(tag)) {
            return String.join("\n", "玩法說明", "目標", "依序排列數字，空格在右下角。", "", "移動", "點擊或滑動與空格同列或同欄的方塊。", "整列滑動算一次移動，符合 Android 規則。", "", "輔助", "使用「顯示可移動方塊」查看目前可移動的方塊。", "求解器完成不會取代玩家最佳紀錄。");
        }
        if ("ja-JP".equals(tag)) {
            return String.join("\n", "遊び方", "目標", "数字を行優先で並べ、空きマスを最後にします。", "", "移動", "空きマスと同じ行または列のタイルをクリック／スワイプします。", "列全体の移動は一手として数えます。", "", "アシスト", "「動かせるタイルを表示」で現在の候補を確認できます。", "ソルバーのクリアはプレイヤー記録を更新しません。");
        }
        return String.join("\n", "How to Play", "Goal", "Arrange the numbers in row-major order with the empty cell at the end.", "", "Moves", "Click or swipe a tile in the same row or column as the empty cell.", "A whole-line slide moves every tile between that tile and the empty cell and counts as one move.", "", "Assist", "Use Show Movable Tiles to highlight tiles that can slide now.", "Solver-assisted completions do not replace player best records.");
    }

    /**
     * Returns localized interactive-practice instructions.
     *
     * @param locale requested locale, or {@code null} for English
     * @return localized practice text
     */
    public static String practiceTutorial(DesktopLocale locale) {
        String tag = locale == null ? "en" : locale.getTag();
        if ("zh-TW".equals(tag)) {
            return String.join("\n", "練習教學", "1. 找到與空格同列或同欄的方塊。", "2. 點擊相鄰方塊完成第一步。", "3. 再點擊較遠的對齊方塊，觀察整列滑動。", "", "這個互動練習對齊 Android 引導式第一題，使用共用 GameModel 規則。");
        }
        if ("ja-JP".equals(tag)) {
            return String.join("\n", "練習チュートリアル", "1. 空きマスと同じ行または列のタイルを探します。", "2. 隣のタイルをクリックして最初の一手を試します。", "3. 遠いタイルを選び、列の移動を確認します。", "", "Android のガイド付き最初のパズルと同じ GameModel ルールです。");
        }
        return String.join("\n", "Practice Tutorial", "1. Find a tile in the same row or column as the empty cell.", "2. Click an adjacent tile to make your first move.", "3. Try a farther aligned tile to see a whole-line slide.", "", "This interactive practice mirrors the Android guided first puzzle and uses the shared GameModel.");
    }

    /**
     * Returns localized pause-menu reminder text.
     *
     * @param locale requested locale, or {@code null} for English
     * @return localized reminder text
     */
    public static String quickReminder(DesktopLocale locale) {
        String tag = locale == null ? "en" : locale.getTag();
        if ("zh-TW".equals(tag)) {
            return String.join("\n", "快速提醒", "• 空格是移動目標。", "• 同列或同欄的方塊都可滑入空格。", "• 整列滑動算一次移動與一次復原。", "• 求解器完成不會更新玩家最佳紀錄。");
        }
        if ("ja-JP".equals(tag)) {
            return String.join("\n", "クイックリマインダー", "• 空きマスが移動先です。", "• 同じ行または列のタイルを動かせます。", "• 列全体の移動は一手／一回の元に戻すです。", "• ソルバーのクリアはプレイヤー記録を更新しません。");
        }
        return String.join("\n", "Quick Reminder", "• The empty cell is the destination.", "• Any aligned tile can slide into it.", "• A whole-line slide counts as one move and one undo.", "• Solver-assisted wins do not update player best records.");
    }
}
