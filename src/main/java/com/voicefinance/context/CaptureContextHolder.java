package com.voicefinance.context;

import com.voicefinance.domain.enums.ExpenseOrigin;

import java.util.UUID;

public final class CaptureContextHolder {

    private static final ThreadLocal<UUID> CAPTURE_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<ExpenseOrigin> ORIGIN_HOLDER = new ThreadLocal<>();

    private CaptureContextHolder() {}

    public static void set(UUID captureId, ExpenseOrigin origin) {
        CAPTURE_ID_HOLDER.set(captureId);
        ORIGIN_HOLDER.set(origin);
    }

    public static UUID getCaptureId() {
        return CAPTURE_ID_HOLDER.get();
    }

    public static ExpenseOrigin getOrigin() {
        ExpenseOrigin origin = ORIGIN_HOLDER.get();
        return origin != null ? origin : ExpenseOrigin.AUDIO;
    }

    public static void clear() {
        CAPTURE_ID_HOLDER.remove();
        ORIGIN_HOLDER.remove();
    }
}
