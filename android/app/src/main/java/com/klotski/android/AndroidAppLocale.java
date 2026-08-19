package com.klotski.android;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

import java.util.Locale;

/**
 * Applies and describes the app-supported locales independently of device locale.
 * <p>
 * Add a {@link LanguageOption} and provide a matching values directory to
 * extend the language list.
 * </p>
 */
final class AndroidAppLocale {
    static final String DEFAULT_LANGUAGE_TAG = "en";
    static final String TRADITIONAL_CHINESE_LANGUAGE_TAG = "zh-TW";
    static final String JAPANESE_LANGUAGE_TAG = "ja-JP";

    private static final LanguageOption[] SUPPORTED_LANGUAGES = {
            new LanguageOption(DEFAULT_LANGUAGE_TAG, R.string.language_english),
            new LanguageOption(TRADITIONAL_CHINESE_LANGUAGE_TAG,
                    R.string.language_traditional_chinese),
            new LanguageOption(JAPANESE_LANGUAGE_TAG, R.string.language_japanese)
    };

    private AndroidAppLocale() {
    }

    static Context wrap(Context baseContext, String languageTag) {
        String normalizedTag = normalizeLanguageTag(languageTag);
        Locale locale = Locale.forLanguageTag(normalizedTag);

        Configuration configuration = new Configuration(baseContext.getResources().getConfiguration());
        configuration.setLocales(new LocaleList(locale));
        return baseContext.createConfigurationContext(configuration);
    }

    static String normalizeLanguageTag(String languageTag) {
        if (languageTag != null) {
            String candidate = languageTag.trim();
            for (LanguageOption option : SUPPORTED_LANGUAGES) {
                if (option.languageTag.equalsIgnoreCase(candidate)) {
                    return option.languageTag;
                }
            }
        }
        return DEFAULT_LANGUAGE_TAG;
    }

    static LanguageOption[] getSupportedLanguages() {
        return SUPPORTED_LANGUAGES.clone();
    }

    static int indexOf(String languageTag) {
        String normalizedTag = normalizeLanguageTag(languageTag);
        for (int index = 0; index < SUPPORTED_LANGUAGES.length; index++) {
            if (SUPPORTED_LANGUAGES[index].languageTag.equals(normalizedTag)) {
                return index;
            }
        }
        return 0;
    }

    static int getDisplayNameResId(String languageTag) {
        return SUPPORTED_LANGUAGES[indexOf(languageTag)].displayNameResId;
    }

    /**
     * Immutable language selector entry.
     */
    static final class LanguageOption {
        final String languageTag;
        final int displayNameResId;

        LanguageOption(String languageTag, int displayNameResId) {
            this.languageTag = languageTag;
            this.displayNameResId = displayNameResId;
        }
    }
}
