package com.klotski.android;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * Shared Android view primitives used by the Activity screen builders.
 */
final class AndroidUi {
    static final int COLOR_BACKGROUND = Color.rgb(11, 18, 32);
    static final int COLOR_PANEL = Color.rgb(24, 34, 51);
    static final int COLOR_PANEL_LIGHT = Color.rgb(37, 50, 73);
    static final int COLOR_PANEL_HIGHLIGHT = Color.rgb(23, 54, 45);
    static final int COLOR_PRIMARY = Color.rgb(47, 138, 69);
    static final int COLOR_ACCENT = Color.rgb(251, 191, 36);
    static final int COLOR_MUTED_TEXT = Color.rgb(190, 199, 213);
    static final int COLOR_POSITIVE_TEXT = Color.rgb(134, 239, 172);
    static final int COLOR_DANGER_PANEL = Color.rgb(72, 31, 40);

    private final Activity activity;
    private final List<Button> commandButtons;

    AndroidUi(Activity activity, List<Button> commandButtons) {
        this.activity = activity;
        this.commandButtons = commandButtons;
    }

    ScreenLayout createScreenLayout() {
        ScrollView root = new ScrollView(activity);
        root.setFillViewport(true);
        root.setVerticalScrollBarEnabled(false);
        root.setBackgroundColor(COLOR_BACKGROUND);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), systemBarHeight("status_bar_height") + dp(26),
                dp(18), systemBarHeight("navigation_bar_height") + dp(18));
        int availableWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int contentWidth = availableWidth >= dp(720)
                ? dp(680)
                : ScrollView.LayoutParams.MATCH_PARENT;
        ScrollView.LayoutParams contentParams = new ScrollView.LayoutParams(
                contentWidth, ScrollView.LayoutParams.WRAP_CONTENT);
        contentParams.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(content, contentParams);
        return new ScreenLayout(root, content);
    }

    void addScreenHeader(LinearLayout parent, String title, String subtitle) {
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        header.setOrientation(LinearLayout.VERTICAL);

        TextView titleText = createText(title, 34, Color.WHITE, Typeface.BOLD);
        titleText.setGravity(Gravity.CENTER);
        titleText.setLetterSpacing(-0.01f);
        header.addView(titleText, fullWidthParams());

        TextView subtitleText = createText(subtitle, 16, COLOR_MUTED_TEXT, Typeface.NORMAL);
        subtitleText.setGravity(Gravity.CENTER);
        subtitleText.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams subtitleParams = fullWidthParams();
        subtitleParams.setMargins(0, dp(8), 0, 0);
        header.addView(subtitleText, subtitleParams);

        LinearLayout.LayoutParams headerParams = fullWidthParams();
        headerParams.setMargins(0, 0, 0, dp(18));
        parent.addView(header, headerParams);
    }

    TextView addSectionLabel(LinearLayout parent, int textResId) {
        TextView label = createText(activity.getString(textResId), 12, COLOR_MUTED_TEXT, Typeface.BOLD);
        label.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams params = fullWidthParams();
        params.setMargins(dp(4), dp(10), 0, dp(10));
        parent.addView(label, params);
        return label;
    }

    Button addWideButton(LinearLayout parent, int textResId, int color, View.OnClickListener listener) {
        Button button = createButton(activity.getString(textResId), color);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = fullWidthParams();
        params.setMargins(0, 0, 0, dp(10));
        parent.addView(button, params);
        return button;
    }

    Button addGameButton(LinearLayout parent, int textResId, View.OnClickListener listener) {
        Button button = createButton(activity.getString(textResId), COLOR_PANEL_LIGHT);
        button.setOnClickListener(listener);
        commandButtons.add(button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        parent.addView(button, params);
        return button;
    }

    Button addRowButton(LinearLayout parent, int textResId, int color, View.OnClickListener listener) {
        Button button = createButton(activity.getString(textResId), color);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        parent.addView(button, params);
        return button;
    }

    Button createButton(String text, int color) {
        Button button = new Button(activity);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinHeight(dp(48));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setLetterSpacing(0.01f);
        button.setStateListAnimator(null);
        button.setBackground(makeInteractiveBackground(color));
        return button;
    }

    TextView createText(CharSequence text, int sp, int color, int style) {
        TextView textView = new TextView(activity);
        textView.setText(text);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        return textView;
    }

    LinearLayout.LayoutParams fullWidthParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    LinearLayout.LayoutParams fixedButtonParams(int widthDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(widthDp), dp(48));
        params.setMargins(dp(4), 0, dp(4), 0);
        return params;
    }

    LinearLayout.LayoutParams centeredWrapParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }

    GradientDrawable makePanelBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(14));
        drawable.setStroke(dp(1), Color.argb(48, 255, 255, 255));
        return drawable;
    }

    RippleDrawable makeInteractiveBackground(int color) {
        return new RippleDrawable(
                ColorStateList.valueOf(Color.argb(48, 255, 255, 255)),
                makePanelBackground(color),
                null);
    }

    GradientDrawable makeCircleBackground(int color, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    GradientDrawable makeCellBackground(int color, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(9));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    int systemBarHeight(String resourceName) {
        int resourceId = activity.getResources().getIdentifier(resourceName, "dimen", "android");
        if (resourceId == 0) {
            return 0;
        }
        return activity.getResources().getDimensionPixelSize(resourceId);
    }
}
