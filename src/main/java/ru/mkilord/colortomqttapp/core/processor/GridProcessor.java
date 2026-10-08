package ru.mkilord.colortomqttapp.core.processor;

import java.util.Properties;
import java.util.function.BiConsumer;

/**
 * Обходит по одному пикселю из каждой клетки сетки. Точек вдвое больше, чем у {@link ChessProcessor}:
 * мелкие детали реже проскакивают между точками, но расчет дороже.
 */
public final class GridProcessor implements Processor {

    private final int cellSize;

    public GridProcessor(int cellSize) {
        this.cellSize = Math.max(1, cellSize);
    }

    public GridProcessor(Properties properties) {
        this(Integer.parseInt(properties.getProperty(ChessProcessor.CELL_SIZE_KEY)));
    }

    @Override
    public void process(int width, int height, BiConsumer<Integer, Integer> action) {
        for (int y = 0; y < height; y += cellSize) {
            for (int x = 0; x < width; x += cellSize) {
                action.accept(x, y);
            }
        }
    }
}
