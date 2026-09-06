package com.klotski.ui;

import com.klotski.core.GameModel;
import com.klotski.core.MoveAction;

/** Small state machine used by the interactive desktop practice lesson. */
public final class DesktopTutorialProgress {
    /** Lesson milestones. */
    public enum Step {
        /** The learner has not made the first move yet. */
        FIRST_MOVE,
        /** The first move is complete and the line-slide example is next. */
        WHOLE_LINE,
        /** The two guided examples have been completed. */
        COMPLETE
    }

    private Step step = Step.FIRST_MOVE;

    /** Creates a fresh two-step lesson state. */
    public DesktopTutorialProgress() {
    }

    /**
     * Returns the current lesson step.
     *
     * @return current lesson step
     */
    public Step getStep() {
        return step;
    }

    /** Restarts the lesson without touching any normal game state. */
    public void reset() {
        step = Step.FIRST_MOVE;
    }

    /**
     * Advances the lesson from the model's most recent completed action.
     *
     * @param model isolated tutorial model
     */
    public void observe(GameModel model) {
        if (model == null || model.getActionHistory().isEmpty()) {
            return;
        }
        MoveAction action = model.getActionHistory().get(model.getActionHistory().size() - 1);
        if (step == Step.FIRST_MOVE) {
            step = Step.WHOLE_LINE;
        } else if (step == Step.WHOLE_LINE && action.getSteps() > 1) {
            step = Step.COMPLETE;
        }
    }
}
