package ru.mkilord.colortomqttapp.core;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.core.sampling.PixelSamples;
import ru.mkilord.colortomqttapp.core.sampling.PointSampler;
import ru.mkilord.colortomqttapp.domain.settings.SamplingPattern;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SamplingTest {

    private static List<List<Integer>> points(PointSampler sampler, int width, int height) {
        var points = new ArrayList<List<Integer>>();
        sampler.forEach(width, height, (x, y) -> points.add(List.of(x, y)));
        return points;
    }

    @Test
    void chessSkipsEveryOtherCell() {
        assertThat(points(PointSampler.of(SamplingPattern.CHESS, 10), 40, 20))
                .containsExactly(List.of(10, 0), List.of(30, 0), List.of(0, 10), List.of(20, 10));
    }

    @Test
    void gridVisitsEveryCellTwiceAsMany() {
        assertThat(points(PointSampler.of(SamplingPattern.GRID, 10), 30, 20)).hasSize(6);
        assertThat(points(PointSampler.of(SamplingPattern.GRID, 10), 200, 200))
                .hasSize(points(PointSampler.of(SamplingPattern.CHESS, 10), 200, 200).size() * 2);
    }

    @Test
    void areaSmallerThanCellGivesCenterPixel() {
        var image = Frames.filled(5, 5, java.awt.Color.ORANGE);

        assertThat(PixelSamples.collect(PointSampler.of(SamplingPattern.CHESS, 20), image))
                .containsExactly(java.awt.Color.ORANGE.getRGB() & 0xFFFFFF);
    }

    @Test
    void directReadMatchesGetRgb() {
        var sampler = PointSampler.grid(3);
        var direct = PixelSamples.collect(sampler, gradient(BufferedImage.TYPE_INT_RGB));
        var generic = PixelSamples.collect(sampler, gradient(BufferedImage.TYPE_3BYTE_BGR));

        assertThat(direct).hasSize(14 * 10).containsExactly(generic);
    }

    @Test
    void subImageIsReadAtRightOffset() {
        var image = gradient(BufferedImage.TYPE_INT_RGB);

        assertThat(PixelSamples.collect(PointSampler.grid(100), image.getSubimage(5, 7, 10, 10))[0])
                .isEqualTo(image.getRGB(5, 7) & 0xFFFFFF);
    }

    private static BufferedImage gradient(int type) {
        var image = new BufferedImage(40, 30, type);
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 40; x++) {
                image.setRGB(x, y, 0xFF000000 | (x * 6) << 16 | (y * 8) << 8 | (x + y));
            }
        }
        return image;
    }
}
