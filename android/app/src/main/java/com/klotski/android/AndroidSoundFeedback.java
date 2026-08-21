package com.klotski.android;

import android.media.AudioManager;
import android.media.ToneGenerator;

/** Owns optional short, asset-free local gameplay tones. */
final class AndroidSoundFeedback {
    private ToneGenerator toneGenerator;
    private boolean enabled;

    AndroidSoundFeedback(boolean enabled) {
        setEnabled(enabled);
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            releaseGenerator();
        }
    }

    void playMove() {
        play(ToneGenerator.TONE_PROP_BEEP, 35);
    }

    void playCompletion() {
        play(ToneGenerator.TONE_PROP_ACK, 180);
    }

    void release() {
        enabled = false;
        releaseGenerator();
    }

    private void play(int tone, int durationMs) {
        if (!enabled) {
            return;
        }
        ToneGenerator generator = ensureGenerator();
        if (generator != null) {
            generator.startTone(tone, durationMs);
        }
    }

    private ToneGenerator ensureGenerator() {
        if (toneGenerator == null) {
            try {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 45);
            } catch (RuntimeException exception) {
                toneGenerator = null;
            }
        }
        return toneGenerator;
    }

    private void releaseGenerator() {
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }
}
