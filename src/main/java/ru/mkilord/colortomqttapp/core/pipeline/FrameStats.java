package ru.mkilord.colortomqttapp.core.pipeline;

/**
 * Кадры в секунду и сглаженное время снимка и расчета. Пишет поток захвата, читает веб-запрос.
 */
final class FrameStats {

    private static final double SMOOTHING = 0.1;
    private static final long SECOND = 1_000_000_000L;

    private double captureMs = -1;
    private double processMs = -1;
    private long windowStart = -1;
    private int framesInWindow;
    private double fps;

    synchronized void record(long captureNanos, long processNanos, long now) {
        captureMs = smooth(captureMs, captureNanos / 1e6);
        processMs = smooth(processMs, processNanos / 1e6);
        if (windowStart < 0) {
            windowStart = now;
            return;
        }
        framesInWindow++;
        var elapsed = now - windowStart;
        if (elapsed >= SECOND) {
            fps = framesInWindow * (double) SECOND / elapsed;
            framesInWindow = 0;
            windowStart = now;
        }
    }

    synchronized Performance snapshot(boolean idle) {
        return new Performance(round(fps), round(Math.max(captureMs, 0)), round(Math.max(processMs, 0)), idle);
    }

    private static double smooth(double current, double value) {
        return current < 0 ? value : current + (value - current) * SMOOTHING;
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
