package ru.mkilord.colortomqttapp.service.impl;

import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RepeatServiceImplTest {

    @Test
    void repeatsWithDelayFromSupplierAndStops() throws InterruptedException {
        var props = new Properties();
        props.setProperty("updatePeriod", "1");
        var repeater = new RepeatServiceImpl(props);
        var runs = new AtomicInteger();
        var five = new CountDownLatch(5);

        repeater.repeat(() -> {
            runs.incrementAndGet();
            five.countDown();
        }, () -> 1);

        assertThat(five.await(2, TimeUnit.SECONDS)).isTrue();
        repeater.stop();
        var afterStop = runs.get();
        Thread.sleep(50);
        assertThat(runs.get()).isBetween(afterStop, afterStop + 1);
    }
}
