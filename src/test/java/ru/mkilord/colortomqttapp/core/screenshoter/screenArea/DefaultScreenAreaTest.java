package ru.mkilord.colortomqttapp.core.screenshoter.screenArea;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Rectangle;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultScreenAreaTest {

    @Test
    void centersAreaOnScreen() {
        assertThat(DefaultScreenArea.centered(new Dimension(1920, 1080), new Dimension(400, 400)))
                .isEqualTo(new Rectangle(760, 340, 400, 400));
    }

    @Test
    void clampsAreaLargerThanScreen() {
        assertThat(DefaultScreenArea.centered(new Dimension(1920, 1080), new Dimension(3000, 500)))
                .isEqualTo(new Rectangle(0, 290, 1920, 500));
    }
}
