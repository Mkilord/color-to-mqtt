package ru.mkilord.colortomqttapp.core.sampling;

import ru.mkilord.colortomqttapp.domain.settings.SamplingPattern;

/**
 * Обходит точки кадра, по которым считается цвет.
 */
@FunctionalInterface
public interface PointSampler {

    void forEach(int width, int height, PointConsumer action);

    static PointSampler of(SamplingPattern pattern, int cellSize) {
        var cell = Math.max(1, cellSize);
        return switch (pattern) {
            case CHESS -> chess(cell);
            case GRID -> grid(cell);
        };
    }

    /** Клетки через одну в шахматном порядке. */
    static PointSampler chess(int cell) {
        return (width, height, action) -> {
            for (int y = 0; y < height; y += cell) {
                int startX = (y / cell) % 2 == 0 ? cell : 0;
                for (int x = startX; x < width; x += cell * 2) {
                    action.accept(x, y);
                }
            }
        };
    }

    /** Каждая клетка. */
    static PointSampler grid(int cell) {
        return (width, height, action) -> {
            for (int y = 0; y < height; y += cell) {
                for (int x = 0; x < width; x += cell) {
                    action.accept(x, y);
                }
            }
        };
    }
}
