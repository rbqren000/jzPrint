package com.org.jzprinter.print;

import android.content.Context;

import com.org.jzprinter.R;

public enum TaskStatus {
    PENDING(0),
    IN_PROGRESS(1),
    COMPLETED(2),
    CANCELLED(3),
    INTERRUPTED(4),
    PAUSED(5);

    private final int code;

    TaskStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public String getLabel(Context context) {
        return context.getString(getLabelRes());
    }

    public int getLabelRes() {
        switch (this) {
            case PENDING: return R.string.task_status_pending;
            case IN_PROGRESS: return R.string.task_status_in_progress;
            case COMPLETED: return R.string.task_status_completed;
            case CANCELLED: return R.string.task_status_cancelled;
            case INTERRUPTED: return R.string.task_status_interrupted;
            case PAUSED: return R.string.task_status_paused;
            default: throw new IllegalStateException("Unknown status: " + this);
        }
    }

    public static TaskStatus fromCode(int code) {
        for (TaskStatus status : values()) {
            if (status.code == code) return status;
        }
        throw new IllegalArgumentException("Unknown TaskStatus code: " + code);
    }

    public static boolean isResumable(int code) {
        return code == PENDING.code || code == INTERRUPTED.code || code == PAUSED.code;
    }
}
