package ru.mkilord.colortomqttapp.service.impl;

import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import ru.mkilord.colortomqttapp.service.RepeaterService;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static lombok.AccessLevel.PRIVATE;

/**
 * Повторяет задачу с паузой {@code updatePeriod} мс между запусками. Пауза отсчитывается
 * от конца предыдущего запуска, поэтому медленный захват не копит очередь.
 * Экземпляр одноразовый: после {@link #stop()} поток освобождается.
 */
@Slf4j
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class RepeatServiceImpl implements RepeaterService {

    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        var thread = new Thread(runnable, "color-capture");
        thread.setDaemon(true);
        return thread;
    });
    long updatePeriod;

    public RepeatServiceImpl(Properties properties) {
        this.updatePeriod = Long.parseLong(properties.getProperty("updatePeriod"));
    }

    @Override
    public void repeat(Runnable runnable) {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Ошибка в цикле захвата цвета", e);
            }
        }, 0, updatePeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        scheduler.shutdownNow();
    }
}
