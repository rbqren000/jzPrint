package com.org.jzprinter.print;

public enum TaskStatus {
    PENDING(0, "待打印"),
    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消"),
    INTERRUPTED(4, "异常中断"),
    PAUSED(5, "已暂停");

    private final int code;
    private final String label;

    TaskStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
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
