package com.klotski.android;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
    static final int COLOR_BACKGROUND = Color.rgb(17, 24, 39);
    static final int COLOR_PANEL = Color.rgb(31, 41, 55);
    static final int COLOR_PANEL_LIGHT = Color.rgb(55, 65, 81);
    static final int COLOR_PRIMARY = Color.rgb(46, 125, 50);
    static final int COLOR_ACCENT = Color.rgb(245, 158, 11);
    static final int COLOR_MUTED_TEXT = Color.rgb(209, 213, 219);

    private final Activity activity;
    private final List<Button> commandButtons;

    AndroidUi(Activity activity, List<Button> commandButtons) {
        this.activity = activity;
        this.commandButtons = commandButtons;
    }

    ScreenLayout createScreenLayout() {
        ScrollView root = new ScrollView(activity);
        root.setFillViewport(true);
        root.setBackgroundColor(COLOR_BACKGROUND);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), systemBarHeight("status_bar_height") + dp(26),
                dp(18), systemBarHeight("navigation_bar_height") + dp(18));
        root.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        return new ScreenLayout(root, content);
    }

    void addScreenHeader(LinearLayout parent, String title, String subtitle) {
        TextView titleText = createText(title, 34, Color.WHITE, Typeface.BOLD);
        titleText.setGravity(Gravity.CENTER);
        parent.addView(titleText, fullWidthParams());

        TextView subtitleText = createText(subtitle, 16, COLOR_MUTED_TEXT, Typeface.NORMAL);
        subtitleText.setGravity(Gravity.CENTER);
        subtitleText.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams subtitleParams = fullWidthParams();
        subtitleParams.setMargins(0, dp(8), 0, dp(24));
        parent.addView(subtitleText, subtitleParams);
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

    Button createButton(String text, int color) {
        Button button = new Button(activity);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinHeight(dp(48));
        button.setBackground(makePanelBackground(color));
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(widthDp), dp(44));
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
        drawable.setCornerRadius(dp(8));
        drawable.setStroke(dp(1), Color.argb(80, 255, 255, 255));
        return drawable;
    }

    GradientDrawable makeCellBackground(int color, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(6));
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
