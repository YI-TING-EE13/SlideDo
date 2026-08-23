package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_DANGER_PANEL;
import static com.klotski.android.AndroidUi.COLOR_MUTED_TEXT;
import static com.klotski.android.AndroidUi.COLOR_PANEL;
import static com.klotski.android.AndroidUi.COLOR_PANEL_HIGHLIGHT;
import static com.klotski.android.AndroidUi.COLOR_PANEL_LIGHT;
import static com.klotski.android.AndroidUi.COLOR_PRIMARY;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klotski.core.DailyCalendarMonth;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Builder for the offline daily-challenge month calendar.
 */
final class AndroidDailyCalendarScreen {
    private final Activity activity;
    private final AndroidUi ui;

    AndroidDailyCalendarScreen(Activity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
    }

    ScreenLayout build(DailyCalendarMonth calendar, DayStateProvider stateProvider,
            CalendarActions actions) {
        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.daily_calendar_root);
        ui.addScreenHeader(screen.content,
                activity.getString(R.string.daily_calendar_title),
                activity.getString(R.string.daily_calendar_subtitle));

        addMonthNavigation(screen.content, calendar, actions);
        addWeekdayHeader(screen.content);
        addCalendarGrid(screen.content, calendar, stateProvider, actions);

        TextView legend = ui.createText(activity.getString(R.string.daily_calendar_legend),
                13, COLOR_MUTED_TEXT, Typeface.NORMAL);
        legend.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams legendParams = ui.fullWidthParams();
        legendParams.setMargins(0, ui.dp(10), 0, ui.dp(14));
        screen.content.addView(legend, legendParams);

        Button back = ui.addWideButton(screen.content, R.string.nav_back, COLOR_PANEL,
                view -> actions.onBack());
        back.setId(R.id.daily_calendar_back_button);
        return screen;
    }

    private void addMonthNavigation(LinearLayout parent, DailyCalendarMonth calendar,
            CalendarActions actions) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button previous = ui.addRowButton(row, R.string.daily_calendar_previous, COLOR_PANEL_LIGHT,
                view -> actions.onPreviousMonth());
        previous.setId(R.id.daily_calendar_previous_button);

        Locale locale = activity.getResources().getConfiguration().getLocales().get(0);
        String monthName = calendar.getMonth().getMonth().getDisplayName(TextStyle.FULL, locale);
        String label = activity.getString(R.string.daily_calendar_month_format,
                monthName, calendar.getMonth().getYear());
        TextView month = ui.createText(label, 18, android.graphics.Color.WHITE, Typeface.BOLD);
        month.setId(R.id.daily_calendar_month_text);
        month.setGravity(Gravity.CENTER);
        row.addView(month, new LinearLayout.LayoutParams(0, ui.dp(52), 1.35f));

        Button next = ui.addRowButton(row, R.string.daily_calendar_next, COLOR_PANEL_LIGHT,
                view -> actions.onNextMonth());
        next.setId(R.id.daily_calendar_next_button);
        next.setEnabled(calendar.canGoNext());
        next.setAlpha(calendar.canGoNext() ? 1f : 0.4f);

        LinearLayout.LayoutParams rowParams = ui.fullWidthParams();
        rowParams.setMargins(0, 0, 0, ui.dp(8));
        parent.addView(row, rowParams);
    }

    private void addWeekdayHeader(LinearLayout parent) {
        LinearLayout weekdays = new LinearLayout(activity);
        String[] labels = activity.getResources().getStringArray(R.array.daily_calendar_weekdays);
        for (String label : labels) {
            TextView weekday = ui.createText(label, 12, COLOR_MUTED_TEXT, Typeface.BOLD);
            weekday.setGravity(Gravity.CENTER);
            weekdays.addView(weekday, new LinearLayout.LayoutParams(0, ui.dp(32), 1f));
        }
        parent.addView(weekdays, ui.fullWidthParams());
    }

    private void addCalendarGrid(LinearLayout parent, DailyCalendarMonth calendar,
            DayStateProvider stateProvider, CalendarActions actions) {
        GridLayout grid = new GridLayout(activity);
        grid.setColumnCount(7);
        grid.setRowCount(6);
        int offset = calendar.getFirstDayOffset();
        for (LocalDate date : calendar.getDates()) {
            DayState state = stateProvider.getState(date);
            Button day = ui.createButton(formatDayLabel(date, state), dayColor(state));
            day.setTextSize(12);
            day.setPadding(0, 0, 0, 0);
            day.setContentDescription(formatDayContentDescription(date, state));
            boolean playable = state != DayState.FUTURE;
            day.setEnabled(playable);
            day.setAlpha(playable ? 1f : 0.35f);
            if (playable) {
                day.setOnClickListener(view -> actions.onDateSelected(date));
            }

            int cell = offset + date.getDayOfMonth() - 1;
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(cell / 7), GridLayout.spec(cell % 7, 1f));
            params.width = 0;
            params.height = ui.dp(54);
            int margin = ui.dp(2);
            params.setMargins(margin, margin, margin, margin);
            grid.addView(day, params);
        }
        parent.addView(grid, ui.fullWidthParams());
    }

    private String formatDayLabel(LocalDate date, DayState state) {
        String suffix = switch (state) {
            case COMPLETED -> " +";
            case IN_PROGRESS -> " ~";
            case MISSED -> " !";
            case READY, FUTURE -> "";
        };
        return date.getDayOfMonth() + suffix;
    }

    private String formatDayContentDescription(LocalDate date, DayState state) {
        int statusId = switch (state) {
            case READY -> R.string.daily_calendar_status_ready;
            case COMPLETED -> R.string.daily_calendar_status_completed;
            case IN_PROGRESS -> R.string.daily_calendar_status_in_progress;
            case MISSED -> R.string.daily_calendar_status_missed;
            case FUTURE -> R.string.daily_calendar_status_future;
        };
        int actionId = switch (state) {
            case COMPLETED -> R.string.daily_calendar_action_replay;
            case READY, IN_PROGRESS, MISSED -> R.string.daily_calendar_action_play;
            case FUTURE -> R.string.daily_calendar_action_unavailable;
        };
        return activity.getString(R.string.daily_calendar_day_accessibility,
                date.toString(), activity.getString(statusId), activity.getString(actionId));
    }

    private int dayColor(DayState state) {
        return switch (state) {
            case READY -> COLOR_PRIMARY;
            case COMPLETED -> COLOR_PANEL_HIGHLIGHT;
            case IN_PROGRESS -> COLOR_PANEL_LIGHT;
            case MISSED -> COLOR_DANGER_PANEL;
            case FUTURE -> COLOR_PANEL;
        };
    }

    enum DayState {
        READY,
        COMPLETED,
        IN_PROGRESS,
        MISSED,
        FUTURE
    }

    interface DayStateProvider {
        DayState getState(LocalDate date);
    }

    interface CalendarActions {
        void onPreviousMonth();

        void onNextMonth();

        void onDateSelected(LocalDate date);

        void onBack();
    }
}
