package ru.mkilord.colortomqttapp.service;

import java.util.function.LongSupplier;

public interface RepeaterService {
    void repeat(Runnable runnable);

    /**
     * Повторяет задачу; пауза перед каждым следующим запуском берется из {@code delayMillis}.
     */
    void repeat(Runnable runnable, LongSupplier delayMillis);

    void stop();
}
